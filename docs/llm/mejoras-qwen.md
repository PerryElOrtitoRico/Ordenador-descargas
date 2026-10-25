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
