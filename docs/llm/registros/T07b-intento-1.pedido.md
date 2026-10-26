<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2046, max_tokens 6114, razonamiento no -->

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

## 3. Cuándo una regla coincide con un nombre de archivo
- Cada lista **no vacía** es una condición; tienen que cumplirse todas. Dentro de una lista basta con que coincida un elemento. Una regla con las tres listas vacías coincide con todo.
  - `extensiones`: la extensión del nombre (sin punto) es igual a alguna, sin distinguir mayúsculas.
  - `patrones`: glob sobre el nombre completo (`*` = cualquier cosa, `?` = un carácter), sin distinguir mayúsculas.
  - `palabras`: el nombre normalizado contiene la palabra normalizada.
- **Normalizar** = quitar acentos, pasar a minúsculas, cambiar `_`, `-` y `.` por espacio, juntar espacios repetidos en uno y recortar.
- Se aplica **la primera** regla que coincide, en el orden del archivo.
- **Temporales** (nunca se tocan): nombres que terminan en `.part`, `.crdownload` o `.tmp` (sin distinguir mayúsculas) o que empiezan con punto.

---

# T07b — Vigilante: WatchService

## Objetivo
Completar `correr` y `descargaEnCurso` de `Vigilante` y los `// TODO` de sus dos tests. El constructor y `close()` ya están.

## Código que ya existe (no lo cambies)
```java
public final class Nombres {
    public static boolean esTemporal(String nombre);   // .part, .crdownload, .tmp o empieza con "."
}
public final class Pendientes {
    public void tocar(Path archivo, long ahoraMs);
    public List<Path> listos(long ahoraMs);   // los que no cambiaron hace quietoMs o más; los saca
}
```

## `descargaEnCurso(archivo)`
`String n = archivo.getFileName().toString(); return Files.exists(archivo.resolveSibling(n + ".part")) || Files.exists(archivo.resolveSibling(n + ".crdownload"));`

## `correr()`
```text
try {
    while (true) {
        WatchKey key = watchService.poll(200, TimeUnit.MILLISECONDS);
        if (key != null) {
            for (WatchEvent<?> ev : key.pollEvents()) {
                if (ev.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                Path ruta = carpeta.resolve((Path) ev.context());
                if (!Nombres.esTemporal(ruta.getFileName().toString())) pendientes.tocar(ruta, System.currentTimeMillis());
            }
            key.reset();
        }
        for (Path ruta : pendientes.listos(System.currentTimeMillis())) {
            if (descargaEnCurso(ruta)) pendientes.tocar(ruta, System.currentTimeMillis());   // sigue esperando
            else if (Files.isRegularFile(ruta)) alEstarListo.accept(ruta);
        }
    }
} catch (ClosedWatchServiceException e) {
    // se cerró con close(): terminar
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```
Escribilo en Java tal cual (es el cuerpo completo del método).

## Tests
- `descargaEnCurso`: con `x.pdf` solo → `false`; con `x.pdf` y `x.pdf.part` → `true`.
- `avisaUnaVezYNoPorTemporales`: completá el `// TODO` del test: `Files.writeString(d.resolve("b.txt.part"), "x")`, `Path a = Files.writeString(d.resolve("a.txt"), "hola")`; esperar en un bucle (`Thread.sleep(50)`, hasta 5 s) a que `listos` tenga 1 elemento; después `Thread.sleep(800)` y `assertEquals(List.of(a), new ArrayList<>(listos))`.

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Vigilante.java
package com.ordenador.service;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Vigila una carpeta (sin subcarpetas) y avisa cuando un archivo lleva quietoMs sin cambiar. */
public final class Vigilante implements AutoCloseable {

    private final Path carpeta;
    private final Pendientes pendientes;
    private final Consumer<Path> alEstarListo;
    private final WatchService watchService;

    /** Abre un WatchService y registra la carpeta para ENTRY_CREATE y ENTRY_MODIFY. */
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException {
        this.carpeta = carpeta;
        this.pendientes = new Pendientes(quietoMs);
        this.alEstarListo = alEstarListo;
        this.watchService = FileSystems.getDefault().newWatchService();
        carpeta.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /** Bucle hasta close() o interrupción del hilo. */
    public void correr() {
        // TODO T07b
        throw new UnsupportedOperationException();
    }

    /** true si existe al lado "<nombre>.part" o "<nombre>.crdownload" (Firefox crea el archivo final vacío mientras baja el .part). */
    public static boolean descargaEnCurso(Path archivo) {
        // TODO T07b
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }
}
```

```java
// src/test/java/com/ordenador/service/VigilanteTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VigilanteTest {

    @Test
    void descargaEnCurso(@TempDir Path d) throws Exception {
        // TODO T07b
    }

    @Test
    @Timeout(15)
    void avisaUnaVezYNoPorTemporales(@TempDir Path d) throws Exception {
        List<Path> listos = Collections.synchronizedList(new ArrayList<>());
        Vigilante v = new Vigilante(d, 300, listos::add);
        Thread hilo = new Thread(v::correr);
        hilo.start();
        try {
            Thread.sleep(200);
            // TODO T07b: crear b.txt.part y a.txt; esperar hasta 5 s a que llegue a.txt; comprobar que llegó una sola vez y nada más
        } finally {
            v.close();
            hilo.join(3000);
        }
        assertFalse(hilo.isAlive());
    }
}
```

Completá los `// TODO` que pide la tarea T07b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Vigilante.java, src/test/java/com/ordenador/service/VigilanteTest.java.
