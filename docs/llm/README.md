# ORDENADOR — paquete para un LLM local

Este paquete especifica el ORDENADOR (ordenador de `~/Descargas`) para que lo programe **un LLM local** (qwen3.5-9b en LM Studio), con Claude diseñando y evaluando. Trae tests ocultos para medir qué tan bien lo hizo. **No contiene la implementación**: el código del LLM va en la rama `llm/primera-version`.

Es el mismo método que el registro de sesiones del RUTINEITOR (`ProyectoRutina/docs/registro-sesiones/`). La idea original está en Obsidian: `Trabajo/Proyectos/Ordenador de descargas/ORDENADOR.md` y `Cómo funcionaría.md`.

```
docs/llm/
├── README.md               ← este archivo (método y planilla)
├── 00-especificacion.md    ← el contrato: va en TODOS los pedidos
├── tareas/                 ← una por conversación con el LLM, en orden
│   ├── T01-nombres.md            normalizar, extensión, temporales, "nombre (2)"    · fácil
│   ├── T02-clasificador.md       modelo + qué regla le toca a un archivo (glob)     · media
│   ├── T03-config.md             reglas por defecto + reglas.json con Gson          · media
│   ├── T04-…-original.md, T05-…, T06-…, T07-…, T08-…   versión original (método 1)
│   └── T04a … T08c               subtareas del método esqueleto (una o dos funciones cada una)
├── evaluacion/             ← NO se le muestra al LLM
│   └── java/com/ordenador/…/T0x…OcultoTest.java   50 tests ocultos
├── registros/              ← pedidos, respuestas crudas y razonamientos de cada intento
└── mejoras-qwen.md         ← retrospectiva por iteración y comparación qwen / Gemma
```

Dependencias: T01 no depende de nada; T02 usa T01; T03 usa T02; T04 usa T01; T05 usa T01–T04; T06 usa T01–T04; T07 usa T01; T08 usa todo.

## 1. LLM local

- LM Studio, CLI en `~/.lmstudio/bin/lms`, API compatible con OpenAI en `http://localhost:1234/v1`.
- `~/.lmstudio/bin/lms server start` y `~/.lmstudio/bin/lms load qwen/qwen3.5-9b --context-length 8192 -y`. Es el único modelo cargado (la GPU es de 6 GB).
- **Contexto: 8192 tokens.** Por eso cada tarea es chica y trae copiadas solo las **firmas** del código que necesita, no los archivos enteros. Tokens de entrada medidos (contrato + tarea + instrucciones): T01 2084 · T02 2203 · T03 2431 · T04 2321 · T05 2549 · T06 2379 · T07 2265 · T08 2844. Lo que sobra del contexto queda para la respuesta.

## 2. Cómo se corre cada tarea

Todo en la rama `llm/primera-version` (sale de `main`; el primer commit agrega el `pom.xml`, que hizo Claude).

1. **Conversación nueva** por tarea: un pedido a `/v1/chat/completions` (modelo `qwen/qwen3.5-9b`, temperatura 0.2) con un mensaje de sistema corto (formato de respuesta) y, como mensaje del usuario, el contrato + la tarea. Se le piden los archivos completos, cada uno en un bloque de código con la ruta en la primera línea.
2. El pedido y la respuesta cruda se guardan en `registros/T0x-intento-N.pedido.md` y `.respuesta.md` (al final de la respuesta, en un comentario, el tiempo y los tokens).
3. Se extraen los bloques a sus rutas y se corre `mvn -q -o test`.
4. Si falla: un pedido nuevo con **solo** el error recortado y el archivo afectado (intento N+1). Máximo **3 correcciones** por tarea.
5. Se copian los tests ocultos de esa tarea a `src/test/java`, se corren, se anota el resultado y se sacan (no quedan en la rama).
6. Si después de 3 correcciones no compila, o pasa menos de la mitad de los ocultos, la tarea la hace Claude: primero se commitea el intento del LLM como `test: T0x intento de qwen3.5-9b (k/n ocultos)` y después `feat: T0x … (hecho por Claude)`.
7. Un commit por tarea: `feat: T0x <qué hace>`.

Para evaluar todos los ocultos juntos:
```bash
cp -r docs/llm/evaluacion/java/com src/test/java/
mvn -o test -Dtest='T0*OcultoTest' -Dsurefire.failIfNoSpecifiedTests=false
# resultado por tarea: target/surefire-reports/com.ordenador.*.T0x*OcultoTest.txt
git checkout -- src/test/java && git clean -fd src/test/java   # sacarlos de nuevo
```

Verificado al armar el paquete:
- los tests ocultos compilan contra clases vacías con las firmas exactas de las tareas;
- con esas clases vacías fallan 49 de 50 (el que pasa, `T07…terminaAlInterrumpir`, solo comprueba que el hilo termina: con la clase vacía termina porque `correr()` lanza una excepción).

## 3. Planilla de resultados

**Método**: *original* = contrato + tarea completa, correcciones con solo el error (sección 2). *Esqueleto* = Claude escribe cada archivo con package, imports, firmas y `// TODO`; la subtarea da los pasos como código; cada corrección repite contrato (extracto) + subtarea + archivo + error (ver `mejoras-qwen.md`). Ocultos de T01–T03 sobre 6/7/6; los de T04–T08 se repartieron por subtarea (en total siguen siendo 50).

| Tarea | Método | Modelo | ¿Compila? | Tests propios | Ocultos | Correcciones | Tiempo LLM | Quién | Notas |
|---|---|---|---|---|---|---|---|---|---|
| T01 | original | qwen | no (su test, tras 3 correcciones) | no compilaban | 5/6 | 3 | 11 min | Claude | `nombreLibre` normalizaba el nombre (`LEEME` → `leeme (2)`); test con carpetas temporales a mano |
| T02 | original | qwen | no | — | 0/7 | 3 | 8 min | Claude | `regla.extensiones` sin `()`; glob sin traducir `*`/`?`; en las correcciones reescribió `Regla` con getters |
| T03 | original | qwen | no contra las firmas | 10/11 | 0/6 | 3 | 8 min | Claude | `ReglasPorDefecto` bien; reescribió `Config` como clase con getters. Se conservó casi todo su `ConfigRepo` |
| T04 | original | qwen | no | — | 0/7 | 2 | 15 min | — | cortado por el cambio de método; rehecho como T04a–c |
| T04a | esqueleto | qwen | sí | 2/2 | 2/2 | 3 | 5 min | qwen | un error por vuelta; repitió la respuesta cuando la tarea era ambigua |
| T04b | esqueleto | qwen | sí | 5/5 | 2/2 | 2 | 12 min | qwen | lo arregló con razonamiento encendido |
| T04c | esqueleto | qwen | sí | 4/5 → 5/5 | 3/3 | 3 | 27 min | qwen + 1 aserción de Claude | había borrado un import del esqueleto; razonamiento medio sin contexto |
| T05a | esqueleto | qwen | sí | 1/2 → 2/2 | 2/2 | 1 | 11 min | qwen + 1 test de Claude | completó también los `// TODO` de T05b–c (descartados) |
| T05b | esqueleto | qwen (y Gemma) | sí | 3/6 → 6/6 | 4/4 | 1 | 9,5 min (Gemma 10) | qwen + 3 aserciones de Claude | método perfecto al primer intento; Gemma también 4/4 pero no compiló al primer intento |
| T05c | esqueleto | qwen (y Gemma) | sí | 1/1 | 1/1 | 0 | 4,4 min (Gemma 3,3) | qwen | perfecto en los dos modelos |
| T06a | esqueleto | qwen | sí | 0/3 → tests de Claude | 3/3 | 2 | 18 min | qwen + tests de Claude | `Process` en try-with-resources por un paso ambiguo |
| T06b | esqueleto | Gemma | sí | 2/2 | 2/2 | 0 | 1 min | Gemma | perfecto al primer intento |
| T07a | esqueleto | Gemma | sí | 2/2 | 2/2 | 0 | 1 min | Gemma | perfecto al primer intento |
| T07b | esqueleto | qwen | sí | 2/2 | 3/3 | 0 | 2 min | qwen | WatchService perfecto al primer intento |
| T08a | esqueleto | Gemma | sí | 2/2 | 2/2 | 0 | 1 min | Gemma | perfecto al primer intento |
| T08b | esqueleto | qwen | sí | 1/1 | 4/4 | 2 | 13 min | qwen | `switch` con flechas y `return`; se arregló dando el cuerpo en Java |
| T08c | esqueleto | qwen | sí | 1/2 → 2/2 | 1/1 | 0 | 5 min | qwen + 1 aserción de Claude | código bien al primer intento |

**Comparación de métodos.** Original (T01–T04): 0 de 4 tareas terminadas por el LLM; 5 de 26 ocultos pasaban con su código (19 %); 11 correcciones, ninguna arregló nada. Esqueleto (T04a–T08c): 13 de 13 subtareas con el código del LLM; **31 de 31 ocultos** (100 %) con su código; 14 correcciones, la mitad útiles; 5 subtareas perfectas al primer intento y otras 3 (T05b, T06a-2, T08c) con el código bien y solo su test mal. Lo que siguió fallando con el esqueleto fueron los **tests propios** (5 subtareas con aserciones mal que el LLM no arregla; las corrigió Claude).

Al final, en `llm/primera-version`: 98 tests en verde (48 propios + 50 ocultos), `mvn -q package` da `target/ordenador.jar`, y la prueba en una carpeta personal falsa (`-Duser.home=<temporal>`, con `notify-send` falso) funcionó con `ordenar --simular`, `limpiar --simular`, `resumen`, `deshacer`, `ordenar` (real, solo dentro de la carpeta falsa) y `vigilar --simular`.

"Tiempo" es lo que tardó el LLM en responder (sumando las correcciones). Qué mirar además de los números:
- si respetó las firmas exactas;
- si tocó archivos que no debía o agregó dependencias;
- si inventó APIs del código existente;
- si los tests propios prueban algo de verdad.
