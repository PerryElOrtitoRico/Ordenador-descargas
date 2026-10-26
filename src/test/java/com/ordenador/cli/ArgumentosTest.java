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
