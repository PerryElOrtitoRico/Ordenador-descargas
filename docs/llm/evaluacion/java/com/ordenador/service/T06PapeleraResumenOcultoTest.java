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

/** Test oculto de T06: no se le muestra al LLM. */
class T06PapeleraResumenOcultoTest {

    private static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    private static Path archivo(Path carpeta, String nombre, int diasAtras) throws Exception {
        Files.createDirectories(carpeta);
        Path p = Files.writeString(carpeta.resolve(nombre), "x");
        Files.setLastModifiedTime(p, FileTime.from(AHORA.minus(Duration.ofDays(diasAtras))));
        return p;
    }

    @Test
    void vencidosPorFecha(@TempDir Path d) throws Exception {
        Path c = d.resolve("Instaladores");
        Path viejo = archivo(c, "viejo.deb", 31);
        Path justo = archivo(c, "justo.exe", 30);
        archivo(c, "nuevo.deb", 2);
        Files.createDirectories(c.resolve("sub"));
        Files.setLastModifiedTime(c.resolve("sub"), FileTime.from(AHORA.minus(Duration.ofDays(90))));
        assertEquals(List.of(justo, viejo), Papelera.vencidos(c, 30, AHORA));
        assertEquals(List.of(), Papelera.vencidos(c, 0, AHORA));
        assertEquals(List.of(), Papelera.vencidos(d.resolve("no-existe"), 30, AHORA));
    }

    @Test
    void limpiarUsaElTachoYRespetaSimular(@TempDir Path home) throws Exception {
        Path inst = home.resolve("Descargas/Instaladores");
        Path viejo = archivo(inst, "viejo.deb", 40);
        archivo(inst, "nuevo.deb", 1);
        Path otro = archivo(home.resolve("Viejos"), "a.zip", 400);
        archivo(home.resolve("Imágenes"), "foto.png", 400);
        Config config = new Config("~/Descargas", 5, List.of(
                new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30),
                new Regla("img", Accion.MOVER, List.of("png"), null, null, "~/Imágenes", null, 0),
                new Regla("aviso", Accion.AVISAR, List.of("pdf"), null, null, null, "m", 10),
                new Regla("viejos", Accion.MOVER, List.of("zip"), null, null, "~/Viejos", null, 365),
                new Regla("falta", Accion.MOVER, List.of("x"), null, null, "~/NoExiste", null, 1)));

        List<Path> tirados = new ArrayList<>();
        List<Path> simulados = Papelera.limpiar(config, home, AHORA, tirados::add, true);
        assertEquals(List.of(viejo, otro), simulados);
        assertEquals(List.of(), tirados);
        assertTrue(Files.exists(viejo));

        assertEquals(List.of(viejo, otro), Papelera.limpiar(config, home, AHORA, tirados::add, false));
        assertEquals(List.of(viejo, otro), tirados);
        assertTrue(Files.exists(viejo), "limpiar no borra: solo llama al tacho");
    }

    @Test
    void gioEsUnTacho() {
        assertNotNull(Papelera.GIO);
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
