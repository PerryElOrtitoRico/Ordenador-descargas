```java
// src/main/java/com/ordenador/cli/Main.java
package com.ordenador.cli;

import com.ordenador.model.Config;
import com.ordenador.model.Movimiento;
import com.ordenador.service.ConfigRepo;
import com.ordenador.service.Notificador;
import com.ordenador.service.Ordenador;
import com.ordenador.service.Papelera;
import com.ordenador.service.Registro;
import com.ordenador.service.Resultado;
import com.ordenador.service.Resumen;
import com.ordenador.service.Rutas;
import com.ordenador.service.Vigilante;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Comandos de consola del ORDENADOR (sección 6 de la especificación). */
public final class Main {

    static final String USO = """
            Uso: ordenador <comando> [--simular]
              vigilar [--simular]   vigila la carpeta y ordena cada archivo nuevo
              ordenar [--simular]   ordena una vez lo que ya hay
              deshacer              deshace el último movimiento
              resumen               lista lo que quedó sin ordenar
              limpiar [--simular]   manda a la papelera lo vencido
              ayuda                 muestra esto""";

    private Main() { }

    public static void main(String[] args) {
        System.exit(ejecutar(args, System.getenv(), Path.of(System.getProperty("user.home")),
                System.out, new NotifySend(), Papelera.GIO));
    }

    /** Código de salida: 0 bien, 1 error (IOException o IllegalArgumentException al ejecutar), 2 argumentos inválidos. */
    public static int ejecutar(String[] args, Map<String, String> env, Path home, PrintStream out,
                               Notificador notificador, Papelera.Tacho tacho) {
        Argumentos a;
        try {
            a = Argumentos.parsear(args);
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            out.println(USO);
            return 2;
        }

        if (a.comando().equals("ayuda")) {
            out.println(USO);
            return 0;
        }

        try {
            Config config = ConfigRepo.cargarOCrear(Rutas.config(env, home));
            Registro registro = new Registro(Rutas.registro(env, home));
            Path carpeta = Rutas.expandir(config.carpeta(), home);
            switch (a.comando()) {
                case "ordenar" -> ordenar(config, home, registro, notificador, a.simular(), out);
                case "deshacer" -> deshacer(registro, out);
                case "resumen" -> resumen(carpeta, notificador, out);
                case "limpiar" -> limpiar(config, home, tacho, a.simular(), out);
                case "vigilar" -> vigilar(config, home, carpeta, registro, notificador, tacho, a.simular(), out);
            }
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /** La línea que se imprime por un resultado, o null si no se imprime nada (IGNORADO). */
    static String linea(Resultado r, boolean simular) {
        String pre = simular ? "[simulación] " : "";
        return switch (r.estado()) {
            case MOVIDO -> pre + "mover: " + r.mensaje();
            case AVISADO -> pre + "aviso: " + r.mensaje();
            case SIN_REGLA -> pre + "sin regla: " + r.archivo().getFileName();
            case IGNORADO -> null;
        };
    }

    private static void ordenar(Config config, Path home, Registro registro, Notificador n, boolean simular, PrintStream out)
            throws IOException {
        for (Resultado r : new Ordenador(config, home, registro, n, simular).ordenarCarpeta()) {
            String l = linea(r, simular);
            if (l != null) out.println(l);
        }
    }

    private static void deshacer(Registro registro, PrintStream out) throws IOException {
        Optional<Movimiento> m = registro.deshacerUltimo();
        if (m.isPresent()) {
            out.println("deshecho: " + m.get().origen() + " → " + m.get().destino());
        } else {
            out.println("No hay movimientos para deshacer.");
        }
    }

    private static void resumen(Path carpeta, Notificador n, PrintStream out) throws IOException {
        List<String> nombres = Resumen.sinOrdenar(carpeta);
        String texto = Resumen.texto(nombres);
        out.println(texto);
        if (!nombres.isEmpty()) n.notificar("Ordenador", texto);
    }

    private static void limpiar(Config config, Path home, Papelera.Tacho tacho, boolean simular, PrintStream out)
            throws IOException {
        String pre = simular ? "[simulación] " : "";
        for (Path p : Papelera.limpiar(config, home, Instant.now(), tacho, simular)) {
            out.println(pre + "a la papelera: " + p);
        }
    }

    private static void vigilar(Config config, Path home, Path carpeta, Registro registro, Notificador n,
                                Papelera.Tacho tacho, boolean simular, PrintStream out) throws IOException {
        out.println("Vigilando " + carpeta + (simular ? " (simulación: no se mueve nada)" : ""));
        limpiar(config, home, tacho, simular, out);
        Ordenador ordenador = new Ordenador(config, home, registro, n, simular);
        try (Vigilante v = new Vigilante(carpeta, config.segundosQuieto() * 1000L, archivo -> {
            try {
                String l = linea(ordenador.procesar(archivo), simular);
                if (l != null) out.println(l);
            } catch (IOException e) {
                out.println("Error con " + archivo + ": " + e.getMessage());
            }
        })) {
            v.correr();
        }
    }
}

```

```java
// src/main/java/com/ordenador/cli/NotifySend.java
package com.ordenador.cli;

import com.ordenador.service.Notificador;

import java.io.IOException;

/** Notificación de escritorio (KDE) con notify-send. Si falla, lo escribe en System.err y sigue. */
public final class NotifySend implements Notificador {

    @Override
    public void notificar(String titulo, String cuerpo) {
        try { new ProcessBuilder("notify-send", "-a", "Ordenador", "-i", "folder-downloads", titulo, cuerpo).start(); } catch (IOException e) { System.err.println("No se pudo notificar: " + e.getMessage()); }
    }
}

```

```java
// src/test/java/com/ordenador/cli/MainTest.java
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
        Files.createDirectories(home.resolve("Descargas"));
        Path raro = home.resolve("Descargas").resolve("raro.xyz");
        Files.writeString(raro, "contenido");

        String salida = correr(0, "resumen");
        assertEquals("Queda 1 archivo sin ordenar en Descargas:\n- raro.xyz", salida);
        assertEquals(1, avisos.size());
    }
}

```

<!-- meta: {"segundos": 286.2, "finish_reason": "stop", "usage": {"prompt_tokens": 3508, "completion_tokens": 2150, "total_tokens": 5658, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
