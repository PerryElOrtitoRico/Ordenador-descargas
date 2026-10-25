# Mejoras del pedido a qwen3.5-9b — retrospectiva por iteración

Una entrada por pedido (tarea o corrección), en orden. Cada una: qué pasó, causa probable con evidencia, y el cambio que se prueba en el pedido siguiente (y si funcionó). Las reglas que se confirman pasan a `~/Escritorio/Hobbies/programacion/Proyectos/qwen-aprendizajes.md`.

Parámetros fijos salvo que se diga: modelo `qwen/qwen3.5-9b` (LM Studio, contexto 8192, PARALLEL 4), temperatura 0.2, `max_tokens` = 8192 − entrada − 32, razonamiento apagado (la API devuelve `reasoning_tokens: 0`).

## Método original (T01–T04): contrato + tarea completa; correcciones con solo el error y el archivo

Las entradas de esta parte se escribieron juntas, al pedir el usuario la retrospectiva (después de T04-3); el registro crudo de cada una está en `registros/`.

| Iteración | Entrada / salida (tokens) | Segundos | Resultado | Causa probable | Cambio probado en la siguiente |
|---|---|---|---|---|---|
| T01-1 | 2084 / 4085 | 354 | Compila el código; el test propio falla (borra carpetas temporales hechas a mano con `Files.deleteIfExists` sobre carpeta no vacía). `Nombres.java` repetido dos veces en la respuesta. `nombreLibre` normaliza la base (`LEEME` → `leeme (2)`). | Ignoró "usá `@TempDir`" de la tarea; "base = nombre sin extensión" lo leyó como nombre normalizado (la palabra *normalizar* estaba cerca). | Corrección con solo el error y el test. |
| T01-2 | 1519 / 1209 | 97 | Respuesta **sin bloque de código** (la ruta en la primera línea, sin ```` ``` ````). Agregó un borrado recursivo con `forEach(this::borrar)` que lanza `Exception`: no compila. | El system prompt pide bloques pero no muestra un ejemplo; en las correcciones el mensaje no tiene ningún bloque de muestra para imitar. | Extractor tolerante a respuestas sin bloques (cambio del lado de Claude). |
| T01-3, T01-4 | ~1420 / 1214 | 101, 107 | Devolvió el **mismo archivo** dos veces, sin tocar la línea del error, aunque en T01-4 se le citó la línea. | Con solo el error, no entiende "excepción comprobada en lambda": le falta el patrón de solución. | — (3 correcciones agotadas: lo hizo Claude) |
| T02-1 | 2203 / 3090 | 326 | No compila: `regla.extensiones` (campo privado del record) en vez de `regla.extensiones()`. Glob sin traducir `*` ni `?`. | No conoce bien los records de Java 16+ (acceso con `x()`); la tarea mostraba el record pero no un ejemplo de uso. | Corrección con error + `Clasificador.java`. |
| T02-2 | 1439 / 243 | 49 | En vez de arreglar `Clasificador`, **reescribió `Regla`** (que no se le mostró) como clase con getters. | Sin ver `Regla`, inventa su forma. | Mandar también `Regla.java` en la corrección. |
| T02-3, T02-4 | ~1850 / 445 | 93, 35 | Agregó getters a `Regla` y no tocó `Clasificador`, dos veces. | Prefiere cambiar la clase de datos antes que el código que la usa; "no cambies las firmas" no alcanza. | — (lo hizo Claude) |
| T03-1 | 2431 / 2340 | 308 | Sin bloques de código y con los 3 archivos pegados uno tras otro. `ReglasPorDefecto` perfecta; `ConfigRepo` usa `config.getReglas()` y `Config.crear()`. | Igual que T02: getters inventados para un record. | Extractor que separa archivos por la línea de ruta. |
| T03-2, T03-3 | ~1260 / 855, 253 | 72, 23 | Siguió con `getReglas()` y después **reescribió `Config`** como clase con getters y `crear()`. | Mismo patrón que T02-2. | — |
| T03-4 | 1243 / 1055 | 99 | Arregló su test; todo compila contra su `Config` inventado, pero los ocultos no compilan contra el `Config` del contrato (0/6). | — | — (lo hizo Claude) |
| T04-1 | 2321 / 3576 | 548 | Faltan imports (`StandardOpenOption` lo importa de `java.io`) y usa el `Path` de `nombreLibre` como `String`. | Imports a ciegas; la firma de `nombreLibre` estaba en la tarea pero no la respetó. Velocidad: 6,5 tok/s (esperado ~21). | Corrección. |
| T04-2, T04-3 | ~1600 / 1295 | 120, 214 | Arregló imports; el `Path`/`String` sigue igual las dos veces (respuesta idéntica en tokens). | Repite la misma salida cuando el error no cambia. | **Cambio de método** (lo pidió el usuario): esqueleto con firmas, contrato + tarea en cada corrección, subtareas de un método. |

Conclusiones del método original:
- Los errores no son de lógica difícil sino de **forma**: records (`x()` vs `getX()`), imports, firmas de código existente. Con 3 correcciones casi nunca los arregla, y prefiere reescribir el archivo de datos antes que su propio código.
- Con solo el error, repite la misma respuesta. Le falta el contexto (el contrato y las firmas) en la corrección.
- El formato pedido ("bloque con la ruta en la primera línea") lo cumple a medias: 2 de 15 respuestas sin bloques.
- Velocidad: entre 6 y 12 tokens por segundo, no 21 (ver la propuesta sobre PARALLEL en el informe).

## Método esqueleto (T04–T05): Claude escribe el archivo con firmas e imports; qwen solo completa los `// TODO`

Cambios respecto del método original, todos juntos desde T04a-1: (1) Claude escribe el esqueleto (package, imports, firmas, javadoc, `// TODO`) y lo compila; (2) el pedido lleva un extracto del contrato (solo las secciones que usa la subtarea), la subtarea y los archivos actuales; (3) system prompt nuevo: completar solo los `// TODO`, no tocar firmas ni otros archivos, "los records se leen con `x()`, no `getX()`", y **un ejemplo** del bloque con la ruta; (4) subtareas de un método o una clase chica; (5) cada corrección repite contrato + subtarea + archivo actual + error. Las firmas se comprueban con `javap -p` contra el esqueleto.

| Iteración | Entrada / salida | Segundos | Resultado | Causa probable | Cambio para la siguiente → ¿funcionó? |
|---|---|---|---|---|---|
| T04a-1 | 1319 / 900 | 70 | **Compila, firmas iguales, formato correcto.** Falla 1 test propio: `expandir("~/Imágenes")` da `/Imágenes` (`substring(1)` deja la barra) y falta el caso `"~"`. | La tarea decía el resultado (`home.resolve("x/y")`) pero no cómo cortar el `~/`. | Corrección con contexto completo (método nuevo). |
| T04a-2 | 1938 / 911 | 83 | Arregló justo el error que se le mostró (`substring(2)`). Ahora falla `expandir("~")`. | Corrige **un error por vuelta**: el informe de JUnit muestra solo la primera aserción que falla. | Misma corrección con el error nuevo. → funcionó a medias (ver T04a-3). |
| T04a-3 | 1980 / 911 | 86 | Respuesta **idéntica** a la anterior. | El mensaje de la excepción que le dictó la tarea ("tiene que empezar con ~/ o /") contradice la regla de `"~"`: el prompt mismo era ambiguo. | Reescribir la regla de la tarea como pasos de código en orden (`if (ruta.equals("~")) return home;` …) y un mensaje que no contradiga. |
| T04a-4 | 2006 / 932 | 88 | **Compila, tests propios 2/2, ocultos 2/2.** | Los pasos en código, en orden, los copia bien. | → funcionó. Para T04b y T04c se escriben las reglas como pasos de código desde el primer pedido. |
| T04b-1 | 1904 / 1575 | 220 | Compila, firmas iguales. `anotar` crea la carpeta de `m.origen()` en vez de la de `archivo` → `NoSuchFileException` (1 test propio, 1/2 ocultos). Además empezó `deshacerUltimo`, que no le tocaba. | Confundió `archivo` con el `Movimiento`; el error recortado mostraba solo marcos de `java.base`, no la línea de su código. | El recorte del error agrega los dos primeros marcos de `com.ordenador` **con la línea de código** (`← línea: …`). |
| T04b-2 | ~2000 / ~1575 | ~200 | Igual que antes: el error señala `writeString` (línea 34) y no tocó el `createDirectories` de la línea 31. | La línea que falla no es la que tiene el error; sin pensar, no conecta "no existe la carpeta" con la línea de arriba. | Probar **razonamiento encendido** (`reasoning_effort: "medium"`; `chat_template_kwargs.enable_thinking` no hace nada en LM Studio). |
| T04b-3 | 2905 / 1787 (467 de razonamiento) | 285 | **Arreglado**: tests propios 5/5 (3 vacíos de T04c), ocultos 2/2. En el razonamiento relee la regla de la tarea y ve que era `archivo.getParent()`. | Con razonamiento compara su código con la regla escrita. | → funcionó. Para T04c: razonamiento desde el primer pedido, y en el system prompt "los `// TODO` que la tarea no nombra quedan como están" (en T04b-1 empezó `deshacerUltimo`). |
