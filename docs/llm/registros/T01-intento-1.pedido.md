<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2084, max_tokens 6076 -->

## system

Sos un programador Java cuidadoso. Respondé SOLO con los archivos pedidos, completos (nada de '...'), cada uno en su propio bloque de código. La primera línea de cada bloque es un comentario con la ruta del archivo, por ejemplo `// src/main/java/com/ordenador/service/X.java` (o `# systemd/x.service` si no es Java). Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto

# ORDENADOR — Especificación (contrato)

*2026-10-25 · Se le da al LLM en todas las tareas. Si una tarea y este documento no coinciden, manda este documento.*

Programa de consola en Java para Debian + KDE: vigila `~/Descargas` y mueve cada archivo nuevo a su lugar según reglas. **Nunca borra nada**: solo mueve (`Files.move`) o manda a la papelera (`gio trash`).

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

## 3. Cuándo una regla coincide con un nombre de archivo
- Cada lista **no vacía** es una condición; tienen que cumplirse todas. Dentro de una lista basta con que coincida un elemento. Una regla con las tres listas vacías coincide con todo.
  - `extensiones`: la extensión del nombre (sin punto) es igual a alguna, sin distinguir mayúsculas.
  - `patrones`: glob sobre el nombre completo (`*` = cualquier cosa, `?` = un carácter), sin distinguir mayúsculas.
  - `palabras`: el nombre normalizado contiene la palabra normalizada.
- **Normalizar** = quitar acentos, pasar a minúsculas, cambiar `_`, `-` y `.` por espacio, juntar espacios repetidos en uno y recortar.
- Se aplica **la primera** regla que coincide, en el orden del archivo.
- **Temporales** (nunca se tocan): nombres que terminan en `.part`, `.crdownload` o `.tmp` (sin distinguir mayúsculas) o que empiezan con punto.

## 4. Acciones
- `MOVER`: `destino` es una carpeta (`~` = carpeta personal); se crea si no existe. Si ya hay un archivo con ese nombre, no se pisa: `nombre (2).ext`, `nombre (3).ext`… Se anota en el registro y se notifica `"<nombre> → <ruta destino>"`. Si `diasPapelera > 0`, al llegar se le pone fecha de modificación = ahora; cuando pasan esos días ahí, va a la papelera.
- `AVISAR`: no mueve; notifica `mensaje` con `{palabra}` reemplazado por la palabra de la lista que coincidió (tal como está escrita en la lista).
- Sin regla: el archivo queda donde está.
- **Simular**: no se mueve, no se anota ni se notifica nada; solo se informa qué se haría.

## 5. Archivos del programa
- Reglas: `$XDG_CONFIG_HOME/ordenador/reglas.json` (si no está definida: `~/.config/ordenador/reglas.json`). Si no existe, se crea con las reglas por defecto.
- Registro de movimientos: `$XDG_DATA_HOME/ordenador/movimientos.log` (si no: `~/.local/share/ordenador/movimientos.log`). Una línea por movimiento: `fecha ISO<TAB>origen<TAB>destino`.

## 6. Comandos
`ordenador vigilar [--simular]` · `ordenar [--simular]` (ordena una vez lo que ya hay) · `deshacer` (el último movimiento) · `resumen` (lo que quedó sin ordenar) · `limpiar [--simular]` (manda a la papelera lo vencido) · `ayuda`.

## 7. Tareas
T01 `Nombres` · T02 modelo + `Clasificador` · T03 reglas por defecto + `ConfigRepo` · T04 `Rutas` + `Registro` · T05 `Ordenador` · T06 `Papelera` + `Resumen` · T07 `Pendientes` + `Vigilante` · T08 `cli`.


---

# Tarea T01

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


---

Hacé la tarea T01 tal como está escrita. Devolvé TODOS los archivos de la sección "Archivos", completos.
