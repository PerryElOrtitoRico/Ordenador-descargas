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
| T04c-1 | 2583 / 2672 (732 razon.) | 369 | `deshacerUltimo` bien escrito, pero no compila: en T04b había **borrado el import `StandardCopyOption`** del esqueleto (no se usaba todavía) y ahora lo necesita. | Limpia imports "sin usar" aunque el system prompt diga que no; el chequeo de firmas (`javap`) no lo ve. | Claude agrega un chequeo de imports contra el esqueleto. Corrección con razonamiento medio. |
| T04c-2 | 3263 / 4895 (**todo** razonamiento) | 695 | `finish_reason: length`: se gastó todo el contexto pensando y **no devolvió código**. En el razonamiento duda entre "no cambies imports" y "podés agregar imports". | Razonamiento `medium` sin tope + regla del prompt que parece contradictoria. | Razonamiento apagado para errores de compilación (son mecánicos). → funcionó (T04c-3). |
| T04c-3 | 3265 / 1947 | 328 | Compila, imports completos, **ocultos 3/3**. Falla su test `deshacerNoPisa`: afirma que `a/x.pdf` no existe cuando el test mismo lo creó antes. | Error en la lógica del test propio, no del código. | Corrección con el error y la línea. |
| T04c-4 | 3377 / 2061 | 242 | Respuesta igual en lo que importa: el test sigue con la misma aserción. | Cuando el error está en el test, insiste: para él la aserción "es lo que pide la tarea". | — (3 correcciones): Claude arregla esa aserción del test. |

Velocidad medida hasta acá: entre 6 y 13 tokens de salida por segundo (T04c: 7,2 · 5,9 · 8,5), lejos de los ~21 esperados.

### Prueba de PARALLEL (después de T04c)
Mismo pedido (el de T04a-1, 1128 tokens de entrada, 500 de salida, temperatura 0), dos veces cada uno:
- PARALLEL 4: 12,2 y 11,2 tok/s (contando también el procesamiento de la entrada). GPU: 4,9 GB usados.
- PARALLEL 1 (`lms unload --all` + `lms load qwen/qwen3.5-9b --context-length 8192 --parallel 1 -y`): 10,3 y 10,5 tok/s. GPU: 4,8 GB.

Conclusión: PARALLEL no era la causa de la lentitud (con un solo pedido a la vez no hay reparto). Queda cargado con PARALLEL 1 (no cambia nada y evita dudas). La velocidad real de este equipo con qwen3.5-9b y contexto 8192 es ~10–12 tok/s, no 21: hay que planificar unos 3–5 minutos por respuesta de 2000 tokens.
| T05a-1 | 2730 / 2311 | 298 | `procesar` y `avisar` **idénticos a los pasos** de la tarea (ocultos 2/2). Pero completó también `mover` y `ordenarCarpeta` (con errores de compilación) y los tests de T05b–c, y puso los dos archivos **en un solo bloque**. | "Completá solo los que nombra la tarea" no alcanza si el archivo tiene otros `// TODO` a la vista: los toma como parte del pedido. | Claude descarta lo que está fuera de alcance; el extractor separa archivos dentro de un mismo bloque. Corrección del test. |
| T05a-2 | ~2900 / ~2400 | ~300 | Volvió a completar todo y no compila (`IOException` dentro de una lambda en `ordenarCarpeta`). Su test de T05a usaba `ordenarCarpeta`, que es de T05c. | Mismo problema de alcance. | Se termina a mano (procesar/avisar de qwen, test `temporalIgnoradoYSinRegla` de Claude). **Para T05b–c**: los métodos de otras subtareas ya no dicen `// TODO` sino `// lo completa otra subtarea (T05c): no lo cambies`. |
| T05b-qwen-1 | 3241 / 2148 | 227 | **`mover` correcto al primer intento, ocultos 4/4, firmas e imports iguales y `ordenarCarpeta` sin tocar** (el marcador nuevo funcionó). Fallan 3 tests propios: esperan el texto `"foto.PNG → home/Imágenes/foto.PNG"` (la palabra `home` literal en vez de la carpeta temporal). | Escribió la ruta "como se ve en la tarea" en lugar de armarla con `home.resolve(...)`. | Corrección con el error. |
| T05b-qwen-2 | 4337 / 2153 | 346 | Los mismos 3 tests fallando, sin cambios en las aserciones. | Igual que T04c-4: no corrige sus propios tests. | Claude cambia las 3 aserciones a `"… → " + home.resolve(...)`. |

### Comparación con Gemma 4 E4B (`google/gemma-4-e4b-qat`) en T05b
Mismo pedido (mismo system prompt, extracto del contrato, subtarea y esqueleto), contexto 8192, PARALLEL 1, sin razonamiento. Se descargó qwen (`lms unload --all`) y se cargó Gemma.

| | qwen3.5-9b | Gemma 4 E4B |
|---|---|---|
| Velocidad (pedido de prueba, 500 tokens) | 10,3–12,2 tok/s | 14,7–14,9 tok/s (no los ~45 de la memoria: la GPU de 6 GB no lo deja entero) |
| Intento 1 | 227 s, 2148 tokens; compila; ocultos **4/4**; 3 tests propios mal | 407 s, 3009 tokens (más comentarios: copió cada paso como comentario); **no compila**: le falta `import com.ordenador.model.Movimiento` en el test; `mover` correcto (ocultos 4/4 con el test propio aparte) |
| Corrección 1 | 346 s; tests propios igual de mal | 198 s, 3021 tokens, entrada 4910 (**7931 de 8192**: casi sin contexto libre); compila; 2 tests propios mal (`"/" + ruta` y una ruta esperada equivocada) |
| Firmas / alcance | iguales; no tocó T05c | iguales; no tocó T05c |

Conclusión de T05b: con un esqueleto y pasos de código, los dos escriben el método bien; ninguno escribe bien los tests que verifican textos con rutas. Gemma es algo más rápido por token pero escribe ~40 % más tokens, así que tardó más; y sus respuestas largas lo dejan cerca del límite de contexto en las correcciones. Se queda la versión de qwen.
| T05c-gemma-1 | 3513 / 2936 | 197 | **Perfecto al primer intento**: compila, tests propios bien, ocultos 1/1, firmas e imports iguales, solo tocó `ordenarCarpeta`. 14,9 tok/s. | Subtarea chica con pasos de código: la copia. | Comparar con qwen en el mismo pedido. |
| T05c-qwen-1 | 3289 / 2628 | 266 | **Perfecto al primer intento** también (ocultos 1/1, tests propios bien). 9,9 tok/s. | Ídem. | Se queda la versión de qwen (es el modelo del experimento); resultado empatado. |

Conclusión de la comparación (T05b + T05c): misma calidad cuando el pedido es un esqueleto con pasos de código; Gemma es ~35 % más rápido por token pero suele escribir más (comentarios por cada paso), así que el tiempo total depende de la subtarea (T05b: qwen ganó; T05c: Gemma ganó). Gemma falló un import en el test de T05b; qwen no. Criterio para lo que queda: **Gemma para subtareas mecánicas cortas** (texto, listas, parseo de argumentos) y **qwen para las que usan APIs de Java menos comunes o hilos** (WatchService, procesos).

## Método esqueleto también en T06–T08
Por los resultados de T04–T05, T06–T08 se hacen con el mismo método (esqueleto + subtareas), aunque el cambio se había pedido solo para T04–T05.

| Iteración | Entrada / salida | Segundos | Resultado | Causa probable | Cambio para la siguiente → ¿funcionó? |
|---|---|---|---|---|---|
| T06a-1 | 2480 / 1651 | 152 | No compila: `try (Process p = …)` (`Process` no es `AutoCloseable`). `vencidos` y `limpiar` bien. | **Prompt ambiguo de Claude**: el paso decía "`Process p;` dentro de `try`: …", y lo leyó como try-with-resources. | El recorte de errores de compilación también muestra la línea de código. |
| T06a-2 | 3362 / 1656 | 248 | Compila; **ocultos 3/3**. Fallan sus 3 tests: `Files.createDirectory` sin crear antes `Descargas`. | Error del test propio otra vez. | Probar razonamiento `low` para un error de test. |
| T06a-3 | 3728 / 4430 (4344 razon.) | 702 | `finish_reason: length` con razonamiento **low**: no devolvió código, y el extractor escribió un `Papelera.java` cortado (11 líneas). | Con entrada >3000 tokens no queda lugar para pensar, ni siquiera en `low`. | El extractor ya no escribe nada si la respuesta está cortada. Razonamiento solo con entrada < ~2500 tokens. Claude escribe los tests (regla: los tests propios que fallan los arregla Claude). |
| T06b-gemma-1 | 1575 / 961 | 57 | **Perfecto al primer intento** con Gemma (criterio: subtarea mecánica de texto y listas): compila, tests propios bien, ocultos 2/2, firmas e imports iguales. 16,9 tok/s. | Subtarea chica y pasos de código. | Seguir con Gemma en T07a (colecciones) y T08a (argumentos). |
| T07a-gemma-1 | 1390 / 1015 | 65 | **Perfecto al primer intento** con Gemma: ocultos 2/2, tests propios bien. 15,7 tok/s. | Colecciones simples con pasos de código. | T07b (WatchService e hilos) con qwen, por el criterio. |
| T07b-1 | 2046 / 1174 | 116 | **Perfecto al primer intento** con qwen:  con WatchService, ocultos 3/3 (incluido el caso Firefox  y el archivo que sigue cambiando), tests propios bien. | El cuerpo entero venía como pseudocódigo casi Java: lo pasó a Java sin errores. | — |
