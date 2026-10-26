<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2203, max_tokens 5957 -->

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

# Tarea T02

# T02 — Modelo y clasificador de reglas

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Crear el modelo de la sección 2 de la especificación (menos `Movimiento`) y decidir qué regla le toca a un nombre de archivo (sección 3).

## Código que ya existe (T01)
```java
package com.ordenador.service;
public final class Nombres {
    public static String normalizar(String texto);   // "Cálculo_Numérico.PDF" → "calculo numerico pdf"
    public static String extension(String nombre);    // "foto.JPG" → "jpg"; sin extensión → ""
}
```

## Archivos
- Crear `src/main/java/com/ordenador/model/Accion.java`
- Crear `src/main/java/com/ordenador/model/Regla.java`
- Crear `src/main/java/com/ordenador/model/Config.java`
- Crear `src/main/java/com/ordenador/service/Coincidencia.java`
- Crear `src/main/java/com/ordenador/service/Clasificador.java`
- Crear `src/test/java/com/ordenador/service/ClasificadorTest.java`

## Qué escribir
`Accion` y `Config`: exactamente como en la especificación. `Regla`: el mismo record, con constructor compacto que cambia cada lista `null` por `List.of()` y si no, la copia con `List.copyOf(...)`.

```java
package com.ordenador.service;

/** La regla elegida y la palabra de regla.palabras() que coincidió (null si la regla no tiene palabras). */
public record Coincidencia(Regla regla, String palabra) { }

public final class Clasificador {
    private Clasificador() { }

    /** Glob sin distinguir mayúsculas: '*' = cero o más caracteres, '?' = uno; todo lo demás es literal
     *  (usá Pattern.quote para los tramos literales). Tiene que coincidir el nombre entero. */
    public static boolean coincideGlob(String patron, String nombre) { ... }

    /** La primera regla de la lista que coincide con el nombre (sección 3), o Optional.empty(). */
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas) { ... }
}
```

- La palabra que se devuelve es la **primera de la lista** cuya forma normalizada está contenida en `Nombres.normalizar(nombre)`, tal como está escrita en la lista (por ejemplo `"MECANICA DE FLUIDOS"`).
- `extensiones` se compara con `Nombres.extension(nombre)` sin distinguir mayúsculas (`"AppImage"` coincide con `x.appimage`).

Ejemplos:
| patrón | nombre | coincide |
|---|---|---|
| `rutina-*` | `Rutina-Octubre.pdf` | sí |
| `plan-*` | `mi-plan-2.json` | no (tiene que empezar con `plan-`) |
| `a?c.txt` | `abc.txt` | sí |
| `a.c` | `abc` | no (el punto es literal) |
| `informe (1)*` | `informe (1) final.pdf` | sí |

## Tests a escribir (como mínimo)
- Los ejemplos de glob.
- Una regla con `extensiones = ["deb", "AppImage"]` coincide con `x.DEB` y `y.appimage`, no con `z.zip`.
- Regla con `extensiones = ["pdf"]` y `palabras = ["CALCULO NUMERIC", "MECANICA DE FLUIDOS"]`: `"Mecánica_de_Fluidos-TP1.pdf"` → coincide con palabra `"MECANICA DE FLUIDOS"`; `"Mecánica_de_Fluidos.docx"` → no.
- Con dos reglas que coinciden, gana la primera.
- `new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0).extensiones()` es una lista vacía.


---

Hacé la tarea T02 tal como está escrita. Devolvé TODOS los archivos de la sección "Archivos", completos.
