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
        String comando = null;
        boolean simular = false;

        for (String a : args) {
            if (a.equals("--simular")) {
                simular = true;
                continue;
            }
            if (a.equals("-h") || a.equals("--help")) {
                a = "ayuda";
            }
            if (a.startsWith("-")) {
                throw new IllegalArgumentException("Opción desconocida: " + a);
            }
            if (!COMANDOS.contains(a)) {
                throw new IllegalArgumentException("Comando desconocido: " + a);
            }
            if (comando != null) {
                throw new IllegalArgumentException("Un solo comando por vez: " + comando + " y " + a);
            }
            comando = a;
        }

        if (comando == null) {
            throw new IllegalArgumentException("Falta el comando");
        }

        if (simular && !CON_SIMULAR.contains(comando)) {
            throw new IllegalArgumentException("--simular no va con " + comando);
        }

        return new Argumentos(comando, simular);
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
        // Caso 1: Comando y --simular al final
        Argumentos arg1 = Argumentos.parsear(new String[]{"ordenar", "--simular"});
        assertEquals("ordenar", arg1.comando());
        assertTrue(arg1.simular());

        // Caso 2: --simular al principio
        Argumentos arg2 = Argumentos.parsear(new String[]{"--simular", "ordenar"});
        assertEquals("ordenar", arg2.comando());
        assertTrue(arg2.simular());

        // Caso 3: Comando sin opciones
        Argumentos arg3 = Argumentos.parsear(new String[]{"deshacer"});
        assertEquals("deshacer", arg3.comando());
        assertFalse(arg3.simular());

        // Caso 4: Ayuda con -h
        Argumentos arg4 = Argumentos.parsear(new String[]{"-h"});
        assertEquals("ayuda", arg4.comando());
        assertFalse(arg4.simular());
    }

    @Test
    void invalidos() {
        // Caso 1: Sin argumentos
        assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(new String[]{}));

        // Caso 2: Comando desconocido
        assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(new String[]{"borrar"}));

        // Caso 3: Múltiples comandos
        assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(new String[]{"ordenar", "resumen"}));

        // Caso 4: --simular con comando no compatible (deshacer)
        assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(new String[]{"deshacer", "--simular"}));

        // Caso 5: Opción desconocida
        assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(new String[]{"ordenar", "--rapido"}));
    }
}
```

<!-- meta: {"segundos": 57.8, "finish_reason": "stop", "usage": {"prompt_tokens": 1323, "completion_tokens": 1010, "total_tokens": 2333, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
