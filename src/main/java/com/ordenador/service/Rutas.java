package com.ordenador.service;

import java.nio.file.Path;
import java.util.Map;

/** Dónde guarda el programa sus archivos (sección 5 de la especificación). */
public final class Rutas {

    private Rutas() { }

    /** env.get("XDG_CONFIG_HOME") (si es null o vacía: home/.config) + "/ordenador/reglas.json". */
    public static Path config(Map<String, String> env, Path home) {
        String xdgConfig = env.get("XDG_CONFIG_HOME");
        Path base;
        if (xdgConfig == null || xdgConfig.isEmpty()) {
            base = home.resolve(".config");
        } else {
            base = Path.of(xdgConfig);
        }
        return base.resolve("ordenador").resolve("reglas.json");
    }

    /** env.get("XDG_DATA_HOME") (si es null o vacía: home/.local/share) + "/ordenador/movimientos.log". */
    public static Path registro(Map<String, String> env, Path home) {
        String xdgData = env.get("XDG_DATA_HOME");
        Path base;
        if (xdgData == null || xdgData.isEmpty()) {
            base = home.resolve(".local").resolve("share");
        } else {
            base = Path.of(xdgData);
        }
        return base.resolve("ordenador").resolve("movimientos.log");
    }

    /**
     * "~" → home; "~/x/y" → home/x/y; una ruta absoluta queda igual;
     * cualquier otra (relativa, "~otro") → IllegalArgumentException con la ruta en el mensaje.
     */
    public static Path expandir(String ruta, Path home) {
        if (ruta.equals("~")) {
            return home;
        } else if (ruta.startsWith("~/")) {
            String resto = ruta.substring(2); // Se quita el "~" y el "/" inicial
            return home.resolve(resto);
        } else if (ruta.startsWith("/")) {
            return Path.of(ruta);
        } else {
            throw new IllegalArgumentException("Ruta no válida (usá ~, ~/carpeta o una ruta absoluta): " + ruta);
        }
    }
}
