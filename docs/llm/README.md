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
│   ├── T04-registro.md           rutas XDG + registro de movimientos y deshacer     · media
│   ├── T05-ordenador.md          aplicar la regla (mover / avisar / simular)        · difícil
│   ├── T06-papelera-resumen.md   instaladores vencidos a la papelera + resumen      · fácil
│   ├── T07-vigilante.md          WatchService + esperar a que el archivo se quede quieto · difícil
│   └── T08-cli.md                comandos de consola + servicio de systemd          · media
├── evaluacion/             ← NO se le muestra al LLM
│   └── java/com/ordenador/…/T0x…OcultoTest.java   50 tests ocultos
└── registros/              ← pedidos y respuestas crudas de cada intento (en la rama del LLM)
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

| Tarea | ¿Compila? | Tests propios | Ocultos | Correcciones | Tiempo | Quién | Notas |
|---|---|---|---|---|---|---|---|
| T01 | | | /6 | | | | |
| T02 | | | /7 | | | | |
| T03 | | | /6 | | | | |
| T04 | | | /7 | | | | |
| T05 | | | /7 | | | | |
| T06 | | | /5 | | | | |
| T07 | | | /5 | | | | |
| T08 | | | /7 | | | | |

"Tiempo" es lo que tardó el LLM en responder (sumando las correcciones). Qué mirar además de los números:
- si respetó las firmas exactas;
- si tocó archivos que no debía o agregó dependencias;
- si inventó APIs del código existente;
- si los tests propios prueban algo de verdad.
