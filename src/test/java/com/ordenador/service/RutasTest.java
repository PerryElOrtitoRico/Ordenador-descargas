package com.ordenador.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RutasTest {

    private static final Path HOME = Path.of("/home/f");

    @Test
    void configYRegistroConYSinXdg() {
        // Sin variables de entorno: usa carpetas por defecto en home
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of(), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of(), HOME));

        // Con XDG_CONFIG_HOME definida
        assertEquals(Path.of("/x/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", "/x"), HOME));

        // Con XDG_DATA_HOME vacía (debe usar la por defecto)
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", ""), HOME));
    }

    @Test
    void expandir() {
        // Ruta con ~
        assertEquals(Path.of("/home/f/Imágenes"), Rutas.expandir("~/Imágenes", HOME));
        // Solo ~
        assertEquals(Path.of("/home/f"), Rutas.expandir("~", HOME));
        // Ruta absoluta
        assertEquals(Path.of("/tmp/a"), Rutas.expandir("/tmp/a", HOME));
        // Ruta relativa (no válida)
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", HOME));
        // Ruta con ~ pero no al inicio (no válida)
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("~otro/x", HOME));
    }
}
