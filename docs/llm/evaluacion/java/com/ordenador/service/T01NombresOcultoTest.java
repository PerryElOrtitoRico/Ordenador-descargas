package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T01: no se le muestra al LLM. */
class T01NombresOcultoTest {

    @Test
    void normalizarQuitaAcentosYSeparadores() {
        assertEquals("calculo numerico tema 3 pdf", Nombres.normalizar("  Cálculo_Numérico--Tema 3.PDF "));
        assertEquals("introduccion a la ingenieria", Nombres.normalizar("INTRODUCCIÓN_A_LA_INGENIERÍA"));
        assertEquals("tecnologia mecanica", Nombres.normalizar("Tecnología...Mecánica"));
        assertEquals("nandu", Nombres.normalizar("Ñandú"));
        assertEquals("", Nombres.normalizar(" _-. "));
    }

    @Test
    void extension() {
        assertEquals("jpg", Nombres.extension("foto.JPG"));
        assertEquals("gz", Nombres.extension("backup.tar.gz"));
        assertEquals("appimage", Nombres.extension("Krita-5.2.AppImage"));
        assertEquals("", Nombres.extension("LEEME"));
        assertEquals("", Nombres.extension(".bashrc"));
        assertEquals("", Nombres.extension("raro."));
    }

    @Test
    void temporales() {
        assertTrue(Nombres.esTemporal("video.mp4.part"));
        assertTrue(Nombres.esTemporal("Unconfirmed 123.CRDOWNLOAD"));
        assertTrue(Nombres.esTemporal("algo.Tmp"));
        assertTrue(Nombres.esTemporal(".~lock.doc#"));
        assertFalse(Nombres.esTemporal("partido.pdf"));
        assertFalse(Nombres.esTemporal("tmp.pdf"));
        assertFalse(Nombres.esTemporal("parte.partes"));
    }

    @Test
    void nombreLibreSinConflicto(@TempDir Path c) {
        assertEquals(c.resolve("a.pdf"), Nombres.nombreLibre(c, "a.pdf"));
        assertEquals(c.resolve("no/existe/a.pdf"), Nombres.nombreLibre(c.resolve("no/existe"), "a.pdf"));
    }

    @Test
    void nombreLibreNumera(@TempDir Path c) throws Exception {
        Files.createFile(c.resolve("a.pdf"));
        assertEquals(c.resolve("a (2).pdf"), Nombres.nombreLibre(c, "a.pdf"));
        Files.createFile(c.resolve("a (2).pdf"));
        Files.createFile(c.resolve("a (3).pdf"));
        assertEquals(c.resolve("a (4).pdf"), Nombres.nombreLibre(c, "a.pdf"));
        assertEquals(4, Files.list(c).count(), "nombreLibre no tiene que crear archivos");
    }

    @Test
    void nombreLibreSinExtensionYDoble(@TempDir Path c) throws Exception {
        Files.createFile(c.resolve("LEEME"));
        Files.createFile(c.resolve("backup.tar.gz"));
        assertEquals(c.resolve("LEEME (2)"), Nombres.nombreLibre(c, "LEEME"));
        assertEquals(c.resolve("backup.tar (2).gz"), Nombres.nombreLibre(c, "backup.tar.gz"));
    }
}
