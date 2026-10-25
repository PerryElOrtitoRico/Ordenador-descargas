# T01 — Nombres de archivo

## Reglas del proyecto (valen para todas las tareas)
- Java 21, Maven. Código en `src/main/java`, tests en `src/test/java` (JUnit 5: `org.junit.jupiter.api.Test`, `static org.junit.jupiter.api.Assertions.*`).
- Nombres y comentarios en español. Sin dependencias nuevas. No modifiques archivos que la tarea no nombre.
- Al terminar, `mvn -q test` tiene que pasar entero.

## Objetivo
Funciones puras sobre nombres de archivo, que usan todas las demás clases.

## Archivos
- Crear `src/main/java/com/ordenador/service/Nombres.java`
- Crear `src/test/java/com/ordenador/service/NombresTest.java`

## Qué escribir
```java
package com.ordenador.service;

import java.nio.file.Path;

public final class Nombres {
    private Nombres() { }

    /** Sin acentos (java.text.Normalizer NFD + quitar \p{M}), minúsculas,
     *  '_' '-' '.' → espacio, espacios repetidos → uno, sin espacios al principio ni al final. */
    public static String normalizar(String texto) { ... }

    /** Lo que sigue al último punto, en minúsculas. "" si no hay punto,
     *  si el único punto es el primer carácter (".bashrc") o si termina en punto. */
    public static String extension(String nombre) { ... }

    /** true si termina en .part, .crdownload o .tmp (sin distinguir mayúsculas) o empieza con '.'. */
    public static boolean esTemporal(String nombre) { ... }

    /** carpeta.resolve(nombre) si no existe ese archivo; si existe, la primera libre de
     *  "base (2).ext", "base (3).ext", … (base = nombre sin la extensión de extension()). */
    public static Path nombreLibre(Path carpeta, String nombre) { ... }
}
```

Ejemplos:
| llamada | resultado |
|---|---|
| `normalizar("  Cálculo_Numérico--Tema 3.PDF ")` | `"calculo numerico tema 3 pdf"` |
| `normalizar("MECÁNICA DE FLUIDOS")` | `"mecanica de fluidos"` |
| `extension("foto.JPG")` | `"jpg"` |
| `extension("backup.tar.gz")` | `"gz"` |
| `extension("LEEME")`, `extension(".bashrc")`, `extension("raro.")` | `""` |
| `esTemporal("video.mp4.part")`, `esTemporal("x.CRDOWNLOAD")`, `esTemporal(".~lock.doc#")` | `true` |
| `esTemporal("partido.pdf")` | `false` |
| `nombreLibre(c, "a.pdf")` con `a.pdf` y `a (2).pdf` ya en `c` | `c/a (3).pdf` |
| `nombreLibre(c, "LEEME")` con `LEEME` ya en `c` | `c/LEEME (2)` |
| `nombreLibre(c, "backup.tar.gz")` con ese ya en `c` | `c/backup.tar (2).gz` |

- `nombreLibre` no crea nada; si `carpeta` no existe, devuelve `carpeta.resolve(nombre)`.

## Tests a escribir (como mínimo)
Los ejemplos de la tabla. Para `nombreLibre`, usá `@TempDir Path c` y creá los archivos con `Files.createFile`.

## No hagas
- No uses `String.toLowerCase()` sin `Locale.ROOT`.
