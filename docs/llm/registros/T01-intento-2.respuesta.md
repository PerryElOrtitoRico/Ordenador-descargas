// src/test/java/com/ordenador/service/NombresTest.java
package com.ordenador.service;

import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NombresTest {

    @Test
    public void testNormalizarConAcentosYGuiones() {
        String input = "  Cálculo_Numérico--Tema 3.PDF ";
        String expected = "calculo numerico tema 3 pdf";
        assertEquals(expected, Nombres.normalizar(input));
    }

    @Test
    public void testNormalizarMayusculas() {
        String input = "MECÁNICA DE FLUIDOS";
        String expected = "mecanica de fluidos";
        assertEquals(expected, Nombres.normalizar(input));
    }

    @Test
    public void testExtensionConPuntoMayusculo() {
        String input = "foto.JPG";
        String expected = "jpg";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionConMultiplePuntos() {
        String input = "backup.tar.gz";
        String expected = "gz";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionSinExtencion() {
        String input = "LEEME";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionConPuntoInicial() {
        String input = ".bashrc";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionTerminaEnPunto() {
        String input = "raro.";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testEsTemporalPart() {
        String input = "video.mp4.part";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalCrdownload() {
        String input = "x.CRDOWNLOAD";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalLock() {
        String input = ".~lock.doc#";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalNormal() {
        String input = "partido.pdf";
        assertFalse(Nombres.esTemporal(input));
    }

    @Test
    public void testNombreLibreSinArchivo() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a.pdf", resultado.getFileName().toString());
        } finally {
            deleteRecursively(carpeta);
        }
    }

    @Test
    public void testNombreLibreConArchivoExistente() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo a.pdf
            Files.createFile(carpeta.resolve("a.pdf"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a (2).pdf", resultado.getFileName().toString());
        } finally {
            deleteRecursively(carpeta);
        }
    }

    @Test
    public void testNombreLibreConArchivoYNumeracion() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivos a.pdf y a (2).pdf
            Files.createFile(carpeta.resolve("a.pdf"));
            Files.createFile(carpeta.resolve("a (2).pdf"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a (3).pdf", resultado.getFileName().toString());
        } finally {
            deleteRecursively(carpeta);
        }
    }

    @Test
    public void testNombreLibreConMultipleExtensiones() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo backup.tar.gz
            Files.createFile(carpeta.resolve("backup.tar.gz"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "backup.tar.gz");
            assertEquals("backup.tar (2).gz", resultado.getFileName().toString());
        } finally {
            deleteRecursively(carpeta);
        }
    }

    @Test
    public void testNombreLibreSinExtensionOriginal() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo LEEME
            Files.createFile(carpeta.resolve("LEEME"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "LEEME");
            assertEquals("LEEME (2)", resultado.getFileName().toString());
        } finally {
            deleteRecursively(carpeta);
        }
    }

    private void deleteRecursively(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(this::deleteRecursively);
            }
            Files.delete(path);
        } else {
            Files.deleteIfExists(path);
        }
    }
}

<!-- meta: {"segundos": 97.4, "finish_reason": "stop", "usage": {"prompt_tokens": 1519, "completion_tokens": 1209, "total_tokens": 2728, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
