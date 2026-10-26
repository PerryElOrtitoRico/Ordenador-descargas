package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NombresTest {

    @Test
    void normalizar() {
        assertEquals("calculo numerico tema 3 pdf", Nombres.normalizar("  Cálculo_Numérico--Tema 3.PDF "));
        assertEquals("mecanica de fluidos", Nombres.normalizar("MECÁNICA DE FLUIDOS"));
    }

    @Test
    void extension() {
        assertEquals("jpg", Nombres.extension("foto.JPG"));
        assertEquals("gz", Nombres.extension("backup.tar.gz"));
        assertEquals("", Nombres.extension("LEEME"));
        assertEquals("", Nombres.extension(".bashrc"));
        assertEquals("", Nombres.extension("raro."));
    }

    @Test
    void esTemporal() {
        assertTrue(Nombres.esTemporal("video.mp4.part"));
        assertTrue(Nombres.esTemporal("x.CRDOWNLOAD"));
        assertTrue(Nombres.esTemporal(".~lock.doc#"));
        assertFalse(Nombres.esTemporal("partido.pdf"));
    }

    @Test
    void nombreLibreNumeraSinCambiarElNombre(@TempDir Path c) throws Exception {
        assertEquals(c.resolve("a.pdf"), Nombres.nombreLibre(c, "a.pdf"));
        Files.createFile(c.resolve("a.pdf"));
        Files.createFile(c.resolve("a (2).pdf"));
        assertEquals(c.resolve("a (3).pdf"), Nombres.nombreLibre(c, "a.pdf"));
        Files.createFile(c.resolve("LEEME"));
        assertEquals(c.resolve("LEEME (2)"), Nombres.nombreLibre(c, "LEEME"));
        Files.createFile(c.resolve("backup.tar.gz"));
        assertEquals(c.resolve("backup.tar (2).gz"), Nombres.nombreLibre(c, "backup.tar.gz"));
        Files.createFile(c.resolve("Mi_Foto.PNG"));
        assertEquals(c.resolve("Mi_Foto (2).PNG"), Nombres.nombreLibre(c, "Mi_Foto.PNG"));
    }

    @Test
    void nombreLibreConCarpetaQueNoExiste(@TempDir Path c) {
        assertEquals(c.resolve("no/a.pdf"), Nombres.nombreLibre(c.resolve("no"), "a.pdf"));
    }
}
