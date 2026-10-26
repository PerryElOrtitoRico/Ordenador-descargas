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

class PapeleraTest {

    static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    /** Crea carpeta/nombre con fecha de modificación de hace diasAtras días. */
    static Path archivo(Path carpeta, String nombre, int diasAtras) throws Exception {
        Files.createDirectories(carpeta);
        Path p = Files.writeString(carpeta.resolve(nombre), "x");
        Files.setLastModifiedTime(p, FileTime.from(AHORA.minus(Duration.ofDays(diasAtras))));
        return p;
    }

    @Test
    void vencidosSoloLosViejos(@TempDir Path home) throws Exception {
        Path inst = home.resolve("Descargas/Instaladores");
        Path viejo = archivo(inst, "viejo.deb", 31);
        archivo(inst, "nuevo.deb", 2);
        assertEquals(List.of(viejo), Papelera.vencidos(inst, 30, AHORA));
    }

    @Test
    void limpiarUsaElTachoYRespetaSimular(@TempDir Path home) throws Exception {
        Path viejo = archivo(home.resolve("Descargas/Instaladores"), "viejo.deb", 31);
        Config config = new Config("~/Descargas", 5, List.of(
                new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30)));
        List<Path> tirados = new ArrayList<>();
        Papelera.Tacho t = tirados::add;
        assertEquals(List.of(viejo), Papelera.limpiar(config, home, AHORA, t, true));
        assertEquals(List.of(), tirados);
        assertEquals(List.of(viejo), Papelera.limpiar(config, home, AHORA, t, false));
        assertEquals(List.of(viejo), tirados);
        assertTrue(Files.exists(viejo));
    }

    @Test
    void diasCeroNoSeLimpia(@TempDir Path home) throws Exception {
        archivo(home.resolve("Descargas/Instaladores"), "viejo.deb", 31);
        Config config = new Config("~/Descargas", 5, List.of(
                new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 0)));
        assertEquals(List.of(), Papelera.limpiar(config, home, AHORA, p -> fail("no tiene que tirar nada"), false));
    }
}
