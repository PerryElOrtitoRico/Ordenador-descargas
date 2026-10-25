package com.ordenador.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RutasTest {

    @Test
    void configSinEnvUsaHomeConfig() {
        Path home = Path.of("/home/f");
        Path resultado = Rutas.config(Map.of(), home);
        assertEquals("/home/f/.config/ordenador/reglas.json", resultado.toString());
    }

    @Test
    void configConXdgConfigHome() {
        Path home = Path.of("/home/f");
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "/x");
        Path resultado = Rutas.config(env, home);
        assertEquals("/x/ordenador/reglas.json", resultado.toString());
    }

    @Test
    void registroSinEnvUsaHomeLocalShare() {
        Path home = Path.of("/home/f");
        Map<String, String> env = Map.of("XDG_DATA_HOME", "");
        Path resultado = Rutas.registro(env, home);
        assertEquals("/home/f/.local/share/ordenador/movimientos.log", resultado.toString());
    }

    @Test
    void expandirTilde() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("~/Imágenes", home);
        assertEquals("/home/usuario/Imágenes", resultado.toString());
    }

    @Test
    void expandirTildeSolo() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("~", home);
        assertEquals("/home/usuario", resultado.toString());
    }

    @Test
    void expandirAbsoluta() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("/tmp/a", home);
        assertEquals("/tmp/a", resultado.toString());
    }

    @Test
    void expandirRelativaLanzaExcepcion() {
        Path home = Path.of("/home/usuario");
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", home));
    }
}
