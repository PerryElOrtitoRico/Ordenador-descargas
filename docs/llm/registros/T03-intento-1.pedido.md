<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2431, max_tokens 5729 -->

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

# Tarea T03

# T03 — Reglas por defecto y archivo de reglas

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas (Gson 2.10.1 ya está), no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Leer y escribir `reglas.json` con Gson, y crearlo con las reglas por defecto si no existe.

## Código que ya existe (T02)
```java
package com.ordenador.model;
public enum Accion { MOVER, AVISAR }
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }
    // el constructor compacto cambia las listas null por List.of()
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
```

## Archivos
- Crear `src/main/java/com/ordenador/service/ReglasPorDefecto.java`
- Crear `src/main/java/com/ordenador/service/ConfigRepo.java`
- Crear `src/test/java/com/ordenador/service/ConfigRepoTest.java`

## Qué escribir
```java
package com.ordenador.service;

public final class ReglasPorDefecto {
    private ReglasPorDefecto() { }
    /** Config("~/Descargas", 5, <las 5 reglas de la tabla, en ese orden>). */
    public static Config crear() { ... }
}

public final class ConfigRepo {
    private ConfigRepo() { }
    /** Si el archivo no existe: crea sus carpetas, guarda crear() y la devuelve. Si existe: leer(archivo). */
    public static Config cargarOCrear(Path archivo) throws IOException { ... }
    /** Lee y valida. Lanza IOException (con la ruta del archivo en el mensaje) si no es JSON válido o no pasa la validación. */
    public static Config leer(Path archivo) throws IOException { ... }
    /** Gson con setPrettyPrinting(), UTF-8. Atómico: escribir "<archivo>.tmp" y Files.move con REPLACE_EXISTING y ATOMIC_MOVE. */
    public static void guardar(Config config, Path archivo) throws IOException { ... }
}
```

Reglas por defecto (`diasPapelera` 0 y `mensaje` null salvo que se indique; las listas que no se nombran, vacías):
| nombre | accion | extensiones | patrones | palabras | destino / mensaje | diasPapelera |
|---|---|---|---|---|---|---|
| `instaladores` | MOVER | `deb`, `AppImage`, `exe` | | | destino `~/Descargas/Instaladores` | 30 |
| `rutinas` | MOVER | `json`, `pdf` | `rutina-*`, `plan-*` | | destino `~/Documentos/Carpeta personal/Gimnasio` | |
| `facultad` | AVISAR | `pdf` | | `CALCULO NUMERIC`, `INTRODUCCION A LA INGENIERIA`, `MECANICA DE FLUIDOS`, `TECNOLOGIA MECANICA` | mensaje `Subilo a Drive, carpeta {palabra}/teoria` (destino null) | |
| `finanzas` | MOVER | `pdf`, `csv`, `xls`, `xlsx` | | `mercado pago`, `mercadopago`, `account statement`, `resumen de cuenta`, `estado de cuenta`, `extracto` | destino `~/Documentos/Carpeta personal/Finanzas/entrada` | |
| `imagenes` | MOVER | `png`, `jpg`, `jpeg`, `webp`, `gif` | | | destino `~/Imágenes` | |

Validación en `leer` (cualquier falla → `IOException`):
- archivo vacío o JSON inválido (`JsonParseException` de Gson); `reglas` null;
- una regla con `nombre` null o en blanco; con `accion` null (Gson pone null si el texto no es `MOVER` ni `AVISAR`; el mensaje nombra la regla);
- una regla `MOVER` sin `destino` (null o en blanco); una regla `AVISAR` sin `mensaje`.
- Si `carpeta` es null se usa `"~/Descargas"`; si `segundosQuieto` ≤ 0 se usa 5 (devolver un `Config` nuevo con esos valores).

## Tests a escribir (como mínimo)
Con `@TempDir Path d`:
- `cargarOCrear(d.resolve("sub/reglas.json"))` crea el archivo y devuelve `ReglasPorDefecto.crear()`.
- `guardar` y después `leer` devuelve un `Config` igual (`assertEquals`); no queda el `.tmp`.
- JSON roto (`"{ reglas: ["`) → `IOException`; `{"reglas":[{"nombre":"x","accion":"COPIAR","destino":"~/x"}]}` → `IOException`.
- `{"reglas":[]}` → carpeta `"~/Descargas"`, segundosQuieto 5.


---

Hacé la tarea T03 tal como está escrita. Devolvé TODOS los archivos de la sección "Archivos", completos.
