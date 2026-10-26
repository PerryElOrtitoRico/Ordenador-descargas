<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, tokens de entrada 1323, max_tokens 6837, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los `// TODO` que la tarea no nombra quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están, nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los records se leen con sus métodos de acceso: m.origen(), regla.extensiones(). No existen getOrigen() ni getExtensiones().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/ordenador/service/Ejemplo.java
package com.ordenador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# ORDENADOR — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven. Paquete base `com.ordenador`: `model` (datos), `service` (lógica), `cli` (comandos).
- Única dependencia: Gson 2.10.1. Tests con JUnit 5 (`org.junit.jupiter.api.*`). No toques `pom.xml`.
- Nombres y comentarios en español.
- Nada de rutas relativas ni `user.dir`: cada clase recibe las rutas por parámetro (así se testea con `@TempDir`).
- Nunca `Files.delete` sobre archivos del usuario.

## 6. Comandos
`ordenador vigilar [--simular]` · `ordenar [--simular]` (ordena una vez lo que ya hay) · `deshacer` (el último movimiento) · `resumen` (lo que quedó sin ordenar) · `limpiar [--simular]` (manda a la papelera lo vencido) · `ayuda`.

---

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

---

# Archivos actuales

```java
// src/main/java/com/ordenador/cli/Argumentos.java
package com.ordenador.cli;

import java.util.Set;

/** Lo que pidió el usuario en la línea de comandos. */
public record Argumentos(String comando, boolean simular) {

    static final Set<String> COMANDOS = Set.of("vigilar", "ordenar", "deshacer", "resumen", "limpiar", "ayuda");
    static final Set<String> CON_SIMULAR = Set.of("vigilar", "ordenar", "limpiar");

    /**
     * "--simular" puede ir antes o después del comando y solo vale con vigilar, ordenar y limpiar. "-h" y "--help" = ayuda.
     * IllegalArgumentException si no hay comando, hay más de uno, el comando o la opción no existen, o --simular va con otro comando.
     */
    public static Argumentos parsear(String[] args) {
        // TODO T08a
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/cli/ArgumentosTest.java
package com.ordenador.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentosTest {

    @Test
    void validos() {
        // TODO T08a
    }

    @Test
    void invalidos() {
        // TODO T08a
    }
}
```

Completá los `// TODO` que pide la tarea T08a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/cli/Argumentos.java, src/test/java/com/ordenador/cli/ArgumentosTest.java.
