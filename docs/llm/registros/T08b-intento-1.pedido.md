<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2997, max_tokens 5163, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los `// TODO` que la tarea no nombra quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están, nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los records se leen con sus métodos de acceso: m.origen(), regla.extensiones(). No existen getOrigen() ni getExtensiones().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/ordenador/service/Ejemplo.java
package com.ordenador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# ORDENADOR — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven. Paquete base `com.ordenador`: `model` (datos), `service` (lógica), `cli` (comandos).
- Única dependencia: Gson 2.10.1. Tests con JUnit 5 (`org.junit.jupiter.api.*`). No toques `pom.xml`.
- Nombres y comentarios en español.
- Nada de rutas relativas ni `user.dir`: cada clase recibe las rutas por parámetro (así se testea con `@TempDir`).
- Nunca `Files.delete` sobre archivos del usuario.

## 4. Acciones
- `MOVER`: `destino` es una carpeta (`~` = carpeta personal); se crea si no existe. Si ya hay un archivo con ese nombre, no se pisa: `nombre (2).ext`, `nombre (3).ext`… Se anota en el registro y se notifica `"<nombre> → <ruta destino>"`. Si `diasPapelera > 0`, al llegar se le pone fecha de modificación = ahora; cuando pasan esos días ahí, va a la papelera.
- `AVISAR`: no mueve; notifica `mensaje` con `{palabra}` reemplazado por la palabra de la lista que coincidió (tal como está escrita en la lista).
- Sin regla: el archivo queda donde está.
- **Simular**: no se mueve, no se anota ni se notifica nada; solo se informa qué se haría.

## 6. Comandos
`ordenador vigilar [--simular]` · `ordenar [--simular]` (ordena una vez lo que ya hay) · `deshacer` (el último movimiento) · `resumen` (lo que quedó sin ordenar) · `limpiar [--simular]` (manda a la papelera lo vencido) · `ayuda`.

---

# T08b — Main: ejecutar, ordenar y deshacer

## Objetivo
Completar **solo** `ejecutar`, `linea`, `ordenar` y `deshacer` de `Main`, y el test `ordenarSimulandoNoMueve`. Los `// TODO T08c` quedan como están.

## Código que ya existe (no lo cambies)
```java
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }      // config.carpeta(), config.segundosQuieto()
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }          // m.origen(), m.destino()
public final class ConfigRepo { public static Config cargarOCrear(Path archivo) throws IOException; }
public final class Rutas {
    public static Path config(Map<String, String> env, Path home);
    public static Path registro(Map<String, String> env, Path home);
    public static Path expandir(String ruta, Path home);
}
public final class Registro { public Registro(Path archivo); public Optional<Movimiento> deshacerUltimo() throws IOException; }
public record Resultado(Path archivo, Estado estado, Path destino, String mensaje) {   // r.estado(), r.mensaje(), r.archivo()
    public enum Estado { MOVIDO, AVISADO, SIN_REGLA, IGNORADO }
}
public final class Ordenador {
    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular);
    public Resultado procesar(Path archivo) throws IOException;
    public List<Resultado> ordenarCarpeta() throws IOException;
}
public final class Papelera {
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException;
}
public final class Resumen {
    public static List<String> sinOrdenar(Path carpeta) throws IOException;
    public static String texto(List<String> nombres);
}
public final class Vigilante implements AutoCloseable {
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException;
    public void correr();
}
```

## `ejecutar(args, env, home, out, notificador, tacho)`
1. `Argumentos a;` → `try { a = Argumentos.parsear(args); } catch (IllegalArgumentException e) { out.println(e.getMessage()); out.println(USO); return 2; }`
2. `if (a.comando().equals("ayuda")) { out.println(USO); return 0; }`
3. Dentro de `try { … return 0; } catch (IOException | IllegalArgumentException e) { out.println("Error: " + e.getMessage()); return 1; }`:
   - `Config config = ConfigRepo.cargarOCrear(Rutas.config(env, home));`
   - `Registro registro = new Registro(Rutas.registro(env, home));`
   - `Path carpeta = Rutas.expandir(config.carpeta(), home);`
   - `switch (a.comando())`: `"ordenar"` → `ordenar(config, home, registro, notificador, a.simular(), out)`; `"deshacer"` → `deshacer(registro, out)`; `"resumen"` → `resumen(carpeta, notificador, out)`; `"limpiar"` → `limpiar(config, home, tacho, a.simular(), out)`; `"vigilar"` → `vigilar(config, home, carpeta, registro, notificador, tacho, a.simular(), out)`.

## `linea(r, simular)`
`String pre = simular ? "[simulación] " : "";` y `switch (r.estado())`: `MOVIDO` → `pre + "mover: " + r.mensaje()`; `AVISADO` → `pre + "aviso: " + r.mensaje()`; `SIN_REGLA` → `pre + "sin regla: " + r.archivo().getFileName()`; `IGNORADO` → `null`.

## `ordenar(...)`
`for (Resultado r : new Ordenador(config, home, registro, n, simular).ordenarCarpeta())` → `String l = linea(r, simular); if (l != null) out.println(l);`

## `deshacer(registro, out)`
`Optional<Movimiento> m = registro.deshacerUltimo();` → si está: `out.println("deshecho: " + m.get().origen() + " → " + m.get().destino());`; si no: `out.println("No hay movimientos para deshacer.");`

## Test (usá `correr(...)` y `home` que ya están en el test)
- `ordenarSimulandoNoMueve`: `Files.createDirectories(home.resolve("Descargas"))` y crear `Descargas/foto.png`; `String salida = correr(0, "ordenar", "--simular");` → `salida` contiene `"[simulación] mover: foto.png"`, `Descargas/foto.png` sigue existiendo y `home/Imágenes` no existe.

---

# Archivos actuales

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
        // TODO T08b
        throw new UnsupportedOperationException();
    }

    /** La línea que se imprime por un resultado, o null si no se imprime nada (IGNORADO). */
    static String linea(Resultado r, boolean simular) {
        // TODO T08b
        throw new UnsupportedOperationException();
    }

    private static void ordenar(Config config, Path home, Registro registro, Notificador n, boolean simular, PrintStream out)
            throws IOException {
        // TODO T08b
        throw new UnsupportedOperationException();
    }

    private static void deshacer(Registro registro, PrintStream out) throws IOException {
        // TODO T08b
        throw new UnsupportedOperationException();
    }

    private static void resumen(Path carpeta, Notificador n, PrintStream out) throws IOException {
        // TODO T08c
        throw new UnsupportedOperationException();
    }

    private static void limpiar(Config config, Path home, Papelera.Tacho tacho, boolean simular, PrintStream out)
            throws IOException {
        // TODO T08c
        throw new UnsupportedOperationException();
    }

    private static void vigilar(Config config, Path home, Path carpeta, Registro registro, Notificador n,
                                Papelera.Tacho tacho, boolean simular, PrintStream out) throws IOException {
        // TODO T08c
        throw new UnsupportedOperationException();
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
        // TODO T08b
    }

    @Test
    void resumenConUnArchivo() throws Exception {
        // TODO T08c
    }
}
```

Completá los `// TODO` que pide la tarea T08b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/cli/Main.java, src/test/java/com/ordenador/cli/MainTest.java.
