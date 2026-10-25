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

/** Test oculto de T05a: no se le muestra al LLM. */
class T05aProcesarOcultoTest {


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
    void avisarNoMueve() throws Exception {
        Path pdf = Files.writeString(descargas.resolve("Química_parcial.pdf"), "x");
        Resultado r = ordenador(false).procesar(pdf);
        assertEquals(Resultado.Estado.AVISADO, r.estado());
        assertNull(r.destino());
        assertEquals("Subilo a Drive, carpeta QUIMICA/teoria", r.mensaje());
        assertEquals(List.of("Ordenador|Química_parcial.pdf: Subilo a Drive, carpeta QUIMICA/teoria"), avisos);
        assertTrue(Files.exists(pdf));
        Path otro = Files.writeString(descargas.resolve("otro.pdf"), "x");
        assertEquals("Sin materia", ordenador(false).procesar(otro).mensaje());
        assertEquals(List.of(), registro.leer());
    }

    @Test
    void ignoradosYSinRegla() throws Exception {
        Ordenador o = ordenador(false);
        Path part = Files.writeString(descargas.resolve("video.mp4.part"), "x");
        Path tmpPng = Files.writeString(descargas.resolve(".oculta.png"), "x");
        Path dir = Files.createDirectories(descargas.resolve("carpeta.png"));
        Path nada = Files.writeString(descargas.resolve("nada.xyz"), "x");
        assertEquals(Resultado.Estado.IGNORADO, o.procesar(part).estado());
        assertEquals(Resultado.Estado.IGNORADO, o.procesar(tmpPng).estado());
        assertEquals(Resultado.Estado.IGNORADO, o.procesar(dir).estado());
        assertEquals(Resultado.Estado.IGNORADO, o.procesar(descargas.resolve("no-existe.png")).estado());
        Resultado r = o.procesar(nada);
        assertEquals(Resultado.Estado.SIN_REGLA, r.estado());
        assertNull(r.destino());
        assertNull(r.mensaje());
        assertTrue(Files.exists(part) && Files.exists(tmpPng) && Files.exists(nada));
        assertEquals(List.of(), avisos);
    }
}
