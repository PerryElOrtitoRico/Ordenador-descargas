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
        // TODO T06a
    }

    @Test
    void limpiarUsaElTachoYRespetaSimular(@TempDir Path home) throws Exception {
        // TODO T06a
    }

    @Test
    void diasCeroNoSeLimpia(@TempDir Path home) throws Exception {
        // TODO T06a
    }
}
