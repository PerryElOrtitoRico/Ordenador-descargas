<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2480, max_tokens 5680, razonamiento no -->

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

## 2. Modelo (`com.ordenador.model`)
```java
public enum Accion { MOVER, AVISAR }

public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }

public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }

public record Movimiento(java.time.LocalDateTime fecha, java.nio.file.Path origen, java.nio.file.Path destino) { }
```
En `Regla`, una lista `null` se toma como lista vacía.

## 4. Acciones
- `MOVER`: `destino` es una carpeta (`~` = carpeta personal); se crea si no existe. Si ya hay un archivo con ese nombre, no se pisa: `nombre (2).ext`, `nombre (3).ext`… Se anota en el registro y se notifica `"<nombre> → <ruta destino>"`. Si `diasPapelera > 0`, al llegar se le pone fecha de modificación = ahora; cuando pasan esos días ahí, va a la papelera.
- `AVISAR`: no mueve; notifica `mensaje` con `{palabra}` reemplazado por la palabra de la lista que coincidió (tal como está escrita en la lista).
- Sin regla: el archivo queda donde está.
- **Simular**: no se mueve, no se anota ni se notifica nada; solo se informa qué se haría.

---

# T06a — Papelera de lo vencido

## Objetivo
Completar `GIO`, `vencidos` y `limpiar` de `Papelera` y sus tres tests. **Nunca borrar**: no uses `Files.delete` ni `File.delete`.

## Código que ya existe (no lo cambies)
```java
public final class Rutas {
    public static Path expandir(String ruta, Path home);   // "~/x" → home/x
}
```
Los records se leen así: `config.reglas()`, `regla.accion()`, `regla.destino()`, `regla.diasPapelera()`.

## `GIO` (cuerpo de la lambda)
1. `Process p;` dentro de `try`: `p = new ProcessBuilder("gio", "trash", archivo.toAbsolutePath().toString()).inheritIO().start();`
2. `int codigo = p.waitFor();` — si hay `InterruptedException`: `Thread.currentThread().interrupt(); throw new IOException("Interrumpido al mandar a la papelera: " + archivo, e);`
3. `if (codigo != 0) throw new IOException("gio trash falló (" + codigo + "): " + archivo);`

## `vencidos(carpeta, dias, ahora)`
1. `if (dias <= 0 || !Files.isDirectory(carpeta)) return new ArrayList<>();`
2. `Instant limite = ahora.minus(Duration.ofDays(dias));`
3. `try (Stream<Path> s = Files.list(carpeta)) {` filtrar `Files::isRegularFile` y los que cumplen `!Files.getLastModifiedTime(p).toInstant().isAfter(limite)` (como `getLastModifiedTime` lanza `IOException`, hacelo en un `for` sobre `s.sorted().toList()`, no dentro de una lambda) `}`
4. Devolver la lista (ya queda ordenada por nombre por el `sorted()`).

## `limpiar(config, home, ahora, tacho, simular)`
1. `List<Path> todos = new ArrayList<>();`
2. `for (Regla r : config.reglas())`: si `r.accion() == Accion.MOVER && r.diasPapelera() > 0`:
   `List<Path> v = vencidos(Rutas.expandir(r.destino(), home), r.diasPapelera(), ahora);` → `todos.addAll(v);` y, si `!simular`, `for (Path p : v) tacho.tirar(p);`
3. `return todos;`

## Tests (usá `AHORA` y `archivo(...)` que ya están en el test; `@TempDir Path home`)
- `vencidosSoloLosViejos`: en `home/Descargas/Instaladores`, `viejo.deb` de hace 31 días y `nuevo.deb` de hace 2 → `vencidos(..., 30, AHORA)` es `List.of(viejo)`.
- `limpiarUsaElTachoYRespetaSimular`: `Config("~/Descargas", 5, List.of(new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30)))` y un tacho falso `List<Path> tirados = new ArrayList<>(); Papelera.Tacho t = tirados::add;`. Con `simular = true`: devuelve `[viejo]` y `tirados` vacía; con `false`: `tirados` = `[viejo]`, y `viejo` **sigue existiendo** (el tacho falso no mueve nada).
- `diasCeroNoSeLimpia`: la misma regla con `diasPapelera` 0 → `limpiar` devuelve lista vacía.

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Papelera.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Manda a la papelera lo que lleva diasPapelera días en la carpeta de su regla. Nunca borra. */
public final class Papelera {

    private Papelera() { }

    /** Quien manda un archivo a la papelera. En los tests se usa uno falso que solo anota. */
    public interface Tacho {
        void tirar(Path archivo) throws IOException;
    }

    /** Ejecuta: gio trash <ruta absoluta>. Si el código de salida no es 0 → IOException con la ruta. */
    public static final Tacho GIO = archivo -> {
        // TODO T06a
        throw new UnsupportedOperationException();
    };

    /**
     * Archivos regulares de primer nivel de la carpeta cuya fecha de modificación es anterior o igual
     * a ahora − dias. Ordenados por nombre. Lista vacía si dias <= 0 o la carpeta no existe.
     */
    public static List<Path> vencidos(Path carpeta, int dias, Instant ahora) throws IOException {
        // TODO T06a
        throw new UnsupportedOperationException();
    }

    /**
     * Para cada regla MOVER con diasPapelera > 0: vencidos(Rutas.expandir(destino, home), diasPapelera, ahora).
     * Si no se simula, tacho.tirar(...) de cada uno. Devuelve todos los vencidos encontrados, en orden de regla.
     */
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException {
        // TODO T06a
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/service/PapeleraTest.java
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
```

Completá los `// TODO` que pide la tarea T06a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Papelera.java, src/test/java/com/ordenador/service/PapeleraTest.java.
