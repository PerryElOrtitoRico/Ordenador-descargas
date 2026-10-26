<!-- contrato: 1,6 | archivos: src/main/java/com/ordenador/cli/Argumentos.java, src/test/java/com/ordenador/cli/ArgumentosTest.java -->
# T08a — Argumentos de la línea de comandos

## Objetivo
Completar `parsear` de `Argumentos` y sus dos tests.

## `parsear(args)`, en este orden
1. `String comando = null; boolean simular = false;`
2. `for (String a : args)`:
   - `if (a.equals("--simular")) { simular = true; continue; }`
   - `if (a.equals("-h") || a.equals("--help")) a = "ayuda";`
   - `if (a.startsWith("-")) throw new IllegalArgumentException("Opción desconocida: " + a);`
   - `if (!COMANDOS.contains(a)) throw new IllegalArgumentException("Comando desconocido: " + a);`
   - `if (comando != null) throw new IllegalArgumentException("Un solo comando por vez: " + comando + " y " + a);`
   - `comando = a;`
3. `if (comando == null) throw new IllegalArgumentException("Falta el comando");`
4. `if (simular && !CON_SIMULAR.contains(comando)) throw new IllegalArgumentException("--simular no va con " + comando);`
5. `return new Argumentos(comando, simular);`

## Tests
- `validos`: `parsear(new String[]{"ordenar", "--simular"})` y `{"--simular", "ordenar"}` → `new Argumentos("ordenar", true)`; `{"deshacer"}` → `new Argumentos("deshacer", false)`; `{"-h"}` → comando `"ayuda"`.
- `invalidos`: lanzan `IllegalArgumentException` (`assertThrows`): `{}`, `{"borrar"}`, `{"ordenar", "resumen"}`, `{"deshacer", "--simular"}`, `{"ordenar", "--rapido"}`.
