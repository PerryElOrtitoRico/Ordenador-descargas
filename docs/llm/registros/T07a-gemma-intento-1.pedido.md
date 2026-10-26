<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, tokens de entrada 1390, max_tokens 6770, razonamiento no -->

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

---

# T07a — Pendientes: esperar a que un archivo se quede quieto

## Objetivo
Completar `tocar`, `listos` y `cantidad` de `Pendientes` (usan el mapa `ultimoCambio` que ya está declarado) y sus dos tests.

## Métodos
- `tocar`: `ultimoCambio.put(archivo, ahoraMs);`
- `listos(ahoraMs)`:
  1. `List<Path> listos = new ArrayList<>();`
  2. `for (Map.Entry<Path, Long> e : ultimoCambio.entrySet())` → `if (ahoraMs - e.getValue() >= quietoMs) listos.add(e.getKey());`
  3. `for (Path p : listos) ultimoCambio.remove(p);` (después del recorrido, no adentro)
  4. `listos.sort(null);` y `return listos;`
- `cantidad`: `return ultimoCambio.size();`

## Tests (usan `A` y `B` que ya están en el test)
- `esperaElTiempoQuieto`: `Pendientes p = new Pendientes(1000); p.tocar(A, 0);` → `p.listos(999)` es `List.of()`, `p.listos(1000)` es `List.of(A)` y después `p.cantidad()` es 0.
- `tocarDeNuevoReiniciaLaEspera`: `tocar(A, 0)`, `tocar(A, 800)` → `listos(1500)` vacío y `listos(1800)` = `List.of(A)`; además con `tocar(B, 0)` y `tocar(A, 0)`, `listos(1000)` = `List.of(A, B)` (ordenado).

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Pendientes.java
package com.ordenador.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Archivos que cambiaron hace poco y todavía no se avisaron. Lógica pura: el tiempo se pasa en ms. No es thread-safe. */
public final class Pendientes {

    private final long quietoMs;
    private final Map<Path, Long> ultimoCambio = new HashMap<>();

    public Pendientes(long quietoMs) {
        this.quietoMs = quietoMs;
    }

    /** Anota que el archivo cambió en ahoraMs (si ya estaba, actualiza la hora). */
    public void tocar(Path archivo, long ahoraMs) {
        // TODO T07a
        throw new UnsupportedOperationException();
    }

    /** Saca y devuelve los archivos cuyo último cambio fue hace quietoMs o más (ahoraMs - ultimo >= quietoMs), ordenados por ruta. */
    public List<Path> listos(long ahoraMs) {
        // TODO T07a
        throw new UnsupportedOperationException();
    }

    public int cantidad() {
        // TODO T07a
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/service/PendientesTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PendientesTest {

    static final Path A = Path.of("/d/a.pdf");
    static final Path B = Path.of("/d/b.pdf");

    @Test
    void esperaElTiempoQuieto() {
        // TODO T07a
    }

    @Test
    void tocarDeNuevoReiniciaLaEspera() {
        // TODO T07a
    }
}
```

Completá los `// TODO` que pide la tarea T07a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Pendientes.java, src/test/java/com/ordenador/service/PendientesTest.java.
