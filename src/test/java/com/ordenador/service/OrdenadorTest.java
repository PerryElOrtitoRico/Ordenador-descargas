package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdenadorTest {

    @TempDir Path home;
    Path descargas;
    Registro registro;
    List<String> avisos;

    /** Imágenes → ~/Imágenes; PDF con "FISICA" → aviso. */
    static final List<Regla> REGLAS = List.of(
            new Regla("img", Accion.MOVER, List.of("png", "jpg"), null, null, "~/Imágenes", null, 0),
            new Regla("fac", Accion.AVISAR, List.of("pdf"), null, List.of("FISICA"), null, "Subilo a {palabra}/teoria", 0));

    @BeforeEach
    void preparar() throws Exception {
        descargas = Files.createDirectories(home.resolve("Descargas"));
        registro = new Registro(home.resolve("log.txt"));
        avisos = new ArrayList<>();
    }

    /** Un Ordenador sobre home/Descargas con REGLAS y un notificador falso que guarda los cuerpos en avisos. */
    Ordenador ordenador(boolean simular) {
        return new Ordenador(new Config("~/Descargas", 5, REGLAS), home, registro, (t, c) -> avisos.add(c), simular);
    }

    @Test
    void avisarNoMueve() throws Exception {
        Path archivo = descargas.resolve("Fisica_TP1.pdf");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.AVISADO, r.estado());
        assertEquals("Subilo a FISICA/teoria", r.mensaje());
        assertTrue(Files.exists(archivo));
        assertEquals(List.of("Fisica_TP1.pdf: Subilo a FISICA/teoria"), avisos);
    }

    @Test
    void temporalIgnoradoYSinRegla() throws Exception {
        Path part = Files.writeString(descargas.resolve("video.mp4.part"), "x");
        Path nada = Files.writeString(descargas.resolve("nada.xyz"), "x");
        assertEquals(Resultado.Estado.IGNORADO, ordenador(false).procesar(part).estado());
        assertEquals(Resultado.Estado.SIN_REGLA, ordenador(false).procesar(nada).estado());
        assertTrue(Files.exists(part));
        assertTrue(Files.exists(nada));
        assertEquals(List.of(), avisos);
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        Path archivo = descargas.resolve("foto.PNG");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.PNG → " + home.resolve("Imágenes/foto.PNG"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.PNG")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto.PNG")));
        assertEquals(1, avisos.size());
        assertEquals(1, registro.leer().size());
    }

    @Test
    void noPisa() throws Exception {
        Path destino = home.resolve("Imágenes");
        Files.createDirectories(destino);
        Path archivoViejo = destino.resolve("foto.png");
        Files.writeString(archivoViejo, "viejo");
        
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "nuevo");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.png → " + home.resolve("Imágenes/foto (2).png"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.png")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto (2).png")));
        assertEquals("viejo", Files.readString(archivoViejo));
    }

    @Test
    void simularNoTocaNada() throws Exception {
        Path archivo = descargas.resolve("a.jpg");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(true).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("a.jpg → " + home.resolve("Imágenes/a.jpg"), r.mensaje());
        assertTrue(Files.exists(descargas.resolve("a.jpg")));
        assertFalse(Files.exists(home.resolve("Imágenes")));
        assertEquals(0, avisos.size());
        assertEquals(0, registro.leer().size());
    }

    @Test
    void ordenarCarpeta() throws Exception {
        // TODO T05c
    }
}

