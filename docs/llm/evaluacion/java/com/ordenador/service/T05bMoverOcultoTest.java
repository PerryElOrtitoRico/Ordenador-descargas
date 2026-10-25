package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T05b: no se le muestra al LLM. */
class T05bMoverOcultoTest {


    @TempDir Path home;
    Path descargas;
    Registro registro;
    List<String> avisos;
    Notificador falso;

    static final List<Regla> REGLAS = List.of(
            new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30),
            new Regla("fac", Accion.AVISAR, List.of("pdf"), null, List.of("FISICA", "QUIMICA"), null,
                    "Subilo a Drive, carpeta {palabra}/teoria", 0),
            new Regla("img", Accion.MOVER, List.of("png", "jpg"), null, null, "~/Imágenes", null, 0),
            new Regla("todo-pdf", Accion.AVISAR, List.of("pdf"), null, null, null, "Sin {palabra}materia", 0));

    @BeforeEach
    void preparar() throws Exception {
        descargas = Files.createDirectories(home.resolve("Descargas"));
        registro = new Registro(home.resolve("datos/movimientos.log"));
        avisos = new ArrayList<>();
        falso = (titulo, cuerpo) -> avisos.add(titulo + "|" + cuerpo);
    }

    private Ordenador ordenador(boolean simular) {
        return new Ordenador(new Config("~/Descargas", 5, REGLAS), home, registro, falso, simular);
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        Path foto = Files.writeString(descargas.resolve("foto.PNG"), "img");
        Resultado r = ordenador(false).procesar(foto);
        Path esperado = home.resolve("Imágenes/foto.PNG");
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals(foto, r.archivo());
        assertEquals(esperado, r.destino());
        assertEquals("foto.PNG → " + esperado, r.mensaje());
        assertEquals("img", Files.readString(esperado));
        assertFalse(Files.exists(foto));
        assertEquals(List.of("Ordenador|foto.PNG → " + esperado), avisos);
        assertEquals(1, registro.leer().size());
        assertEquals(foto, registro.leer().get(0).origen());
        assertEquals(esperado, registro.leer().get(0).destino());
    }

    @Test
    void noPisa() throws Exception {
        Files.createDirectories(home.resolve("Imágenes"));
        Files.writeString(home.resolve("Imágenes/foto.png"), "viejo");
        Path foto = Files.writeString(descargas.resolve("foto.png"), "nuevo");
        Resultado r = ordenador(false).procesar(foto);
        assertEquals(home.resolve("Imágenes/foto (2).png"), r.destino());
        assertEquals("viejo", Files.readString(home.resolve("Imágenes/foto.png")));
        assertEquals("nuevo", Files.readString(home.resolve("Imágenes/foto (2).png")));
    }

    @Test
    void simularNoTocaNada() throws Exception {
        Path foto = Files.writeString(descargas.resolve("a.jpg"), "x");
        Path pdf = Files.writeString(descargas.resolve("Fisica-1.pdf"), "x");
        Resultado r = ordenador(true).procesar(foto);
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals(home.resolve("Imágenes/a.jpg"), r.destino());
        assertTrue(Files.exists(foto));
        assertFalse(Files.exists(home.resolve("Imágenes")));
        Resultado a = ordenador(true).procesar(pdf);
        assertEquals(Resultado.Estado.AVISADO, a.estado());
        assertEquals("Subilo a Drive, carpeta FISICA/teoria", a.mensaje());
        assertEquals(List.of(), avisos);
        assertEquals(List.of(), registro.leer());
        assertFalse(Files.exists(home.resolve("datos/movimientos.log")));
    }

    @Test
    void yaEstaEnSuLugarYFechaParaPapelera() throws Exception {
        Path inst = Files.createDirectories(home.resolve("Descargas/Instaladores"));
        Path deb = Files.writeString(inst.resolve("ya.deb"), "x");
        assertEquals(Resultado.Estado.IGNORADO, ordenador(false).procesar(deb).estado());
        assertTrue(Files.exists(deb));

        Path nuevo = Files.writeString(descargas.resolve("app.deb"), "x");
        Files.setLastModifiedTime(nuevo, FileTime.from(Instant.now().minus(Duration.ofDays(100))));
        Resultado r = ordenador(false).procesar(nuevo);
        assertEquals(inst.resolve("app.deb"), r.destino());
        Instant llegada = Files.getLastModifiedTime(r.destino()).toInstant();
        assertTrue(Duration.between(llegada, Instant.now()).abs().toMinutes() < 5,
                "con diasPapelera > 0 la fecha de modificación tiene que ser la de llegada");
    }
}
