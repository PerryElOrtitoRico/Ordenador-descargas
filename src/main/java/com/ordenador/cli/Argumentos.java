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
