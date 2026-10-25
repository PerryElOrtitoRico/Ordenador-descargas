<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2321, max_tokens 5839 -->

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

# Tarea T04

# T04 — Rutas del programa y registro de movimientos

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Saber dónde van los archivos del programa (sección 5) y llevar el registro de movimientos para poder deshacer el último.

## Código que ya existe (T01)
```java
package com.ordenador.service;
public final class Nombres {
    /** carpeta.resolve(nombre) si está libre; si no, "base (2).ext", "base (3).ext"… */
    public static Path nombreLibre(Path carpeta, String nombre);
}
```

## Archivos
- Crear `src/main/java/com/ordenador/model/Movimiento.java` (exactamente como en la especificación)
- Crear `src/main/java/com/ordenador/service/Rutas.java`
- Crear `src/main/java/com/ordenador/service/Registro.java`
- Crear `src/test/java/com/ordenador/service/RutasTest.java`
- Crear `src/test/java/com/ordenador/service/RegistroTest.java`

## Qué escribir
```java
package com.ordenador.service;

public final class Rutas {
    private Rutas() { }
    /** env.get("XDG_CONFIG_HOME") (si es null o vacía: home/.config) + "/ordenador/reglas.json". */
    public static Path config(Map<String, String> env, Path home) { ... }
    /** env.get("XDG_DATA_HOME") (si es null o vacía: home/.local/share) + "/ordenador/movimientos.log". */
    public static Path registro(Map<String, String> env, Path home) { ... }
    /** "~" → home; "~/x/y" → home/x/y; una ruta absoluta queda igual;
     *  cualquier otra (relativa, "~otro") → IllegalArgumentException con la ruta en el mensaje. */
    public static Path expandir(String ruta, Path home) { ... }
}

public final class Registro {
    public Registro(Path archivo) { ... }
    /** Crea las carpetas si hace falta y agrega al final la línea
     *  fecha.toString() + "\t" + origen + "\t" + destino + "\n" (UTF-8, StandardOpenOption.CREATE y APPEND). */
    public void anotar(Movimiento m) throws IOException { ... }
    /** Los movimientos en orden (el más viejo primero). Lista vacía si el archivo no existe.
     *  Ignora líneas en blanco y líneas que no tienen 3 partes o cuya fecha no se puede leer (LocalDateTime.parse). */
    public List<Movimiento> leer() throws IOException { ... }
    /** Deshace el último movimiento leído:
     *  - si no hay ninguno → Optional.empty();
     *  - si m.destino() ya no existe → IOException ("ya no está: <destino>") y el registro no cambia;
     *  - si no: crea la carpeta de m.origen() si falta y mueve m.destino() a m.origen()
     *    (o a Nombres.nombreLibre(carpeta de origen, nombre de origen) si ahí ya hay otro archivo);
     *    reescribe el registro sin esa última línea (atómico: "<archivo>.tmp" + Files.move REPLACE_EXISTING);
     *    devuelve Optional.of(new Movimiento(ahora, m.destino(), adondeQuedo)). */
    public Optional<Movimiento> deshacerUltimo() throws IOException { ... }
}
```

## Tests a escribir (como mínimo)
- `Rutas.config(Map.of(), Path.of("/home/f"))` → `/home/f/.config/ordenador/reglas.json`; con `XDG_CONFIG_HOME=/x` → `/x/ordenador/reglas.json`; con `XDG_DATA_HOME=""` → `/home/f/.local/share/ordenador/movimientos.log`.
- `expandir("~/Imágenes", home)`, `expandir("~", home)`, `expandir("/tmp/a", home)`; `expandir("Descargas", home)` lanza `IllegalArgumentException`.
- Con `@TempDir`: anotar dos movimientos y `leer()` los devuelve iguales y en orden.
- Mover un archivo de `a/x.pdf` a `b/x.pdf`, anotarlo, `deshacerUltimo()` → `a/x.pdf` existe, `b/x.pdf` no, y `leer()` queda vacío.
- Si al deshacer ya hay otro `a/x.pdf`, vuelve como `a/x (2).pdf`.
- `deshacerUltimo()` sin registro → `Optional.empty()`.


---

Hacé la tarea T04 tal como está escrita. Devolvé TODOS los archivos de la sección "Archivos", completos.
