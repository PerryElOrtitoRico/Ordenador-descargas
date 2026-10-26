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
