package com.ordenador.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @TempDir Path home;
    final List<String> avisos = new ArrayList<>();
    final List<Path> tirados = new ArrayList<>();

    /** Corre Main.ejecutar con reglas y registro dentro de home, y devuelve lo que imprimió. */
    String correr(int codigoEsperado, String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", home.resolve("cfg").toString(),
                "XDG_DATA_HOME", home.resolve("dat").toString());
        int codigo = Main.ejecutar(args, env, home, new PrintStream(bytes, true, StandardCharsets.UTF_8),
                (t, c) -> avisos.add(c), tirados::add);
        String salida = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(codigoEsperado, codigo, salida);
        return salida;
    }

    @Test
    void ordenarSimulandoNoMueve() throws Exception {
        Files.createDirectories(home.resolve("Descargas"));
        Path foto = home.resolve("Descargas").resolve("foto.png");
        Files.writeString(foto, "contenido");

        String salida = correr(0, "ordenar", "--simular");
        assertTrue(salida.contains("[simulación] mover: foto.png"));
        assertTrue(Files.exists(foto));
        assertFalse(Files.exists(home.resolve("Imágenes")));
    }

    @Test
    void resumenConUnArchivo() throws Exception {
        // TODO T08c
    }
}
