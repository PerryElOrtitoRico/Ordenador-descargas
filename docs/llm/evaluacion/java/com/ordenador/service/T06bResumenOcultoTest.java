package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
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

/** Test oculto de T06b: no se le muestra al LLM. */
class T06bResumenOcultoTest {


    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    private static Path archivo(Path carpeta, String nombre, int diasAtras) throws Exception {
        Files.createDirectories(carpeta);
        Path p = Files.writeString(carpeta.resolve(nombre), "x");
        Files.setLastModifiedTime(p, FileTime.from(AHORA.minus(Duration.ofDays(diasAtras))));
        return p;
    }

    @Test
    void sinOrdenar(@TempDir Path d) throws Exception {
        Path c = Files.createDirectories(d.resolve("Descargas"));
        for (String n : List.of("b.zip", "a.pdf", "x.part", "y.crdownload", ".oculto", "c.txt")) {
            Files.writeString(c.resolve(n), "x");
        }
        Files.createDirectories(c.resolve("Instaladores"));
        assertEquals(List.of("a.pdf", "b.zip", "c.txt"), Resumen.sinOrdenar(c));
        assertEquals(List.of(), Resumen.sinOrdenar(d.resolve("no-existe")));
    }

    @Test
    void texto() {
        assertEquals("Descargas está ordenada.", Resumen.texto(List.of()));
        assertEquals("Queda 1 archivo sin ordenar en Descargas:\n- a.pdf", Resumen.texto(List.of("a.pdf")));
        assertEquals("Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt",
                Resumen.texto(List.of("a.pdf", "b.zip", "c.txt")));
    }
}
