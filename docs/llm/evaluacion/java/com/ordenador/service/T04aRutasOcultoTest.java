package com.ordenador.service;

import com.ordenador.model.Movimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T04a: no se le muestra al LLM. */
class T04aRutasOcultoTest {


    private static final Path HOME = Path.of("/home/f");

    @Test
    void rutasXdg() {
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of(), HOME));
        assertEquals(Path.of("/x/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", "/x"), HOME));
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", ""), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of(), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", ""), HOME));
        assertEquals(Path.of("/d/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", "/d"), HOME));
    }

    @Test
    void expandir() {
        assertEquals(Path.of("/home/f/Imágenes"), Rutas.expandir("~/Imágenes", HOME));
        assertEquals(HOME, Rutas.expandir("~", HOME));
        assertEquals(Path.of("/home/f/Documentos/Carpeta personal/Gimnasio"),
                Rutas.expandir("~/Documentos/Carpeta personal/Gimnasio", HOME));
        assertEquals(Path.of("/tmp/a"), Rutas.expandir("/tmp/a", HOME));
        var e = assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", HOME));
        assertTrue(e.getMessage().contains("Descargas"));
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("~otro/x", HOME));
    }
}
