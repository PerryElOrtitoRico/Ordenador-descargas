package com.ordenador.cli;

import com.ordenador.service.Notificador;
import com.ordenador.service.Papelera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T08c: no se le muestra al LLM. */
class T08cMainOcultoTest {

    @TempDir Path home;
    Map<String, String> env;
    List<String> avisos;
    List<Path> tirados;
    Path descargas;

    @BeforeEach
    void preparar() throws Exception {
        env = Map.of("XDG_CONFIG_HOME", home.resolve("cfg").toString(), "XDG_DATA_HOME", home.resolve("dat").toString());
        avisos = new ArrayList<>();
        tirados = new ArrayList<>();
        descargas = Files.createDirectories(home.resolve("Descargas"));
    }

    private String correr(int codigoEsperado, String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        Notificador n = (t, c) -> avisos.add(c);
        Papelera.Tacho tacho = tirados::add;
        int codigo = Main.ejecutar(args, env, home, out, n, tacho);
        String texto = bytes.toString(StandardCharsets.UTF_8);
        assertEquals(codigoEsperado, codigo, texto);
        return texto;
    }

    @Test
    void resumenYLimpiar() throws Exception {
        assertTrue(correr(0, "resumen").contains("Descargas está ordenada."));
        assertEquals(List.of(), avisos);
        Files.writeString(descargas.resolve("raro.xyz"), "x");
        assertTrue(correr(0, "resumen").contains("Queda 1 archivo sin ordenar en Descargas:\n- raro.xyz"));
        assertEquals(1, avisos.size());

        Path inst = Files.createDirectories(home.resolve("Descargas/Instaladores"));
        Path viejo = Files.writeString(inst.resolve("viejo.deb"), "x");
        Files.setLastModifiedTime(viejo, FileTime.from(Instant.now().minus(Duration.ofDays(45))));
        String sim = correr(0, "limpiar", "--simular");
        assertTrue(sim.contains("[simulación] a la papelera: " + viejo), sim);
        assertEquals(List.of(), tirados);
        String real = correr(0, "limpiar");
        assertTrue(real.contains("a la papelera: " + viejo), real);
        assertEquals(List.of(viejo), tirados);
    }
}
