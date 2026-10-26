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

/** Test oculto de T08b: no se le muestra al LLM. */
class T08bMainOcultoTest {

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
    void argumentosInvalidosDevuelven2() {
        correr(2, "borrar");
        assertFalse(correr(0, "ayuda").isBlank(), "ayuda imprime el uso");
    }

    @Test
    void ordenarSimulandoNoMueveYCreaLasReglas() throws Exception {
        Files.writeString(descargas.resolve("foto.png"), "x");
        Files.writeString(descargas.resolve("Mecanica_de_Fluidos_TP2.pdf"), "x");
        Files.writeString(descargas.resolve("raro.xyz"), "x");
        String salida = correr(0, "ordenar", "--simular");
        assertTrue(Files.exists(home.resolve("cfg/ordenador/reglas.json")), "crea reglas.json con los defectos");
        assertTrue(salida.contains("[simulación] mover: foto.png → " + home.resolve("Imágenes/foto.png")), salida);
        assertTrue(salida.contains("[simulación] aviso: Subilo a Drive, carpeta MECANICA DE FLUIDOS/teoria"), salida);
        assertTrue(salida.contains("[simulación] sin regla: raro.xyz"), salida);
        assertTrue(Files.exists(descargas.resolve("foto.png")));
        assertFalse(Files.exists(home.resolve("Imágenes")));
        assertEquals(List.of(), avisos);
        assertFalse(Files.exists(home.resolve("dat/ordenador/movimientos.log")));
    }

    @Test
    void ordenarYDeshacer() throws Exception {
        Files.writeString(descargas.resolve("plan-franco-2026-10-26.json"), "{}");
        String salida = correr(0, "ordenar");
        Path destino = home.resolve("Documentos/Carpeta personal/Gimnasio/plan-franco-2026-10-26.json");
        assertTrue(Files.exists(destino), salida);
        assertTrue(salida.contains("mover: plan-franco-2026-10-26.json → " + destino), salida);
        assertFalse(salida.contains("[simulación]"));
        assertEquals(1, avisos.size());
        assertTrue(Files.exists(home.resolve("dat/ordenador/movimientos.log")));

        String deshecho = correr(0, "deshacer");
        assertTrue(deshecho.contains("deshecho: " + destino + " → " + descargas.resolve("plan-franco-2026-10-26.json")), deshecho);
        assertTrue(Files.exists(descargas.resolve("plan-franco-2026-10-26.json")));
        assertTrue(correr(0, "deshacer").contains("No hay movimientos para deshacer."));
    }

    @Test
    void reglasRotasDevuelven1() throws Exception {
        Files.createDirectories(home.resolve("cfg/ordenador"));
        Files.writeString(home.resolve("cfg/ordenador/reglas.json"), "{ roto");
        correr(1, "ordenar");
        assertEquals("{ roto", Files.readString(home.resolve("cfg/ordenador/reglas.json")), "no se pisa el archivo del usuario");
    }
}
