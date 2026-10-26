package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumenTest {

    @Test
    void sinOrdenarSaltaTemporalesYCarpetas(@TempDir Path d) throws IOException {
        // Crear archivos y directorios de prueba
        Files.createFile(d.resolve("b.zip"));
        Files.createFile(d.resolve("a.pdf"));
        Files.createFile(d.resolve("x.part"));
        Files.createFile(d.resolve(".oculto"));
        Files.createDirectory(d.resolve("sub"));

        // Ejecutar el método
        List<String> resultado = Resumen.sinOrdenar(d);

        // Esperado: "a.pdf" y "b.zip" en orden alfabético
        List<String> esperado = List.of("a.pdf", "b.zip");
        assertEquals(esperado, resultado);
    }

    @Test
    void textoVacioUnoYVarios() {
        // Caso vacío
        List<String> vacio = List.of();
        assertEquals("Descargas está ordenada.", Resumen.texto(vacio));

        // Caso uno
        List<String> uno = List.of("a.pdf");
        assertEquals("Queda 1 archivo sin ordenar en Descargas:\n- a.pdf", Resumen.texto(uno));

        // Caso varios
        List<String> varios = List.of("a.pdf", "b.zip", "c.txt");
        String esperadoVarios = "Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt";
        assertEquals(esperadoVarios, Resumen.texto(varios));
    }
}
