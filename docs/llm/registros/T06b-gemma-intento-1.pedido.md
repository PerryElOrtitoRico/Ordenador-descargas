<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, tokens de entrada 1575, max_tokens 6585, razonamiento no -->

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

# T06b — Resumen de lo que quedó sin ordenar

## Objetivo
Completar `sinOrdenar` y `texto` de `Resumen` y sus dos tests.

## Código que ya existe (no lo cambies)
```java
public final class Nombres {
    public static boolean esTemporal(String nombre);  // .part, .crdownload, .tmp o empieza con "."
}
```

## `sinOrdenar(carpeta)`
1. `if (!Files.isDirectory(carpeta)) return new ArrayList<>();`
2. `try (Stream<Path> s = Files.list(carpeta)) { return s.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).filter(n -> !Nombres.esTemporal(n)).sorted().toList(); }`

## `texto(nombres)`
1. `if (nombres.isEmpty()) return "Descargas está ordenada.";`
2. `StringBuilder sb = new StringBuilder(nombres.size() == 1 ? "Queda 1 archivo sin ordenar en Descargas:" : "Quedan " + nombres.size() + " archivos sin ordenar en Descargas:");`
3. Por cada nombre: `sb.append("\n- ").append(nombre);`
4. `return sb.toString();`

## Tests
- `sinOrdenarSaltaTemporalesYCarpetas`: en `d` crear `b.zip`, `a.pdf`, `x.part`, `.oculto` y la subcarpeta `sub` → `List.of("a.pdf", "b.zip")`.
- `textoVacioUnoYVarios`: los tres casos, con los textos exactos de arriba (por ejemplo `"Quedan 2 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip"`).

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Resumen.java
package com.ordenador.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Lo que quedó sin ordenar en Descargas (para el aviso semanal). */
public final class Resumen {

    private Resumen() { }

    /** Nombres de los archivos regulares de primer nivel que no son temporales, en orden alfabético. Vacía si la carpeta no existe. */
    public static List<String> sinOrdenar(Path carpeta) throws IOException {
        // TODO T06b
        throw new UnsupportedOperationException();
    }

    /**
     * Vacía → "Descargas está ordenada.".
     * Uno → "Queda 1 archivo sin ordenar en Descargas:\n- a.pdf"
     * Varios → "Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt" (sin "\n" al final).
     */
    public static String texto(List<String> nombres) {
        // TODO T06b
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/service/ResumenTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumenTest {

    @Test
    void sinOrdenarSaltaTemporalesYCarpetas(@TempDir Path d) throws Exception {
        // TODO T06b
    }

    @Test
    void textoVacioUnoYVarios() {
        // TODO T06b
    }
}
```

Completá los `// TODO` que pide la tarea T06b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Resumen.java, src/test/java/com/ordenador/service/ResumenTest.java.
