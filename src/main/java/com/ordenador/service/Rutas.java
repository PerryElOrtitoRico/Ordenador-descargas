package com.ordenador.service;

import java.nio.file.Path;
import java.util.Map;

/** Dónde guarda el programa sus archivos (sección 5 de la especificación). */
public final class Rutas {

    private Rutas() { }

    /** env.get("XDG_CONFIG_HOME") (si es null o vacía: home/.config) + "/ordenador/reglas.json". */
    public static Path config(Map<String, String> env, Path home) {
        // TODO
        throw new UnsupportedOperationException();
    }

    /** env.get("XDG_DATA_HOME") (si es null o vacía: home/.local/share) + "/ordenador/movimientos.log". */
    public static Path registro(Map<String, String> env, Path home) {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * "~" → home; "~/x/y" → home/x/y; una ruta absoluta queda igual;
     * cualquier otra (relativa, "~otro") → IllegalArgumentException con la ruta en el mensaje.
     */
    public static Path expandir(String ruta, Path home) {
        // TODO
        throw new UnsupportedOperationException();
    }
}
