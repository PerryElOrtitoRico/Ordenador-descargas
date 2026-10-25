package com.ordenador.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Utilidad para obtener las rutas de configuración y registro del programa.
 */
public final class Rutas {
    private Rutas() { }

    /**
     * Obtiene la ruta al archivo de reglas.
     * Prioriza XDG_CONFIG_HOME, si no existe o está vacío usa ~/.config.
     * Añade /ordenador/reglas.json.
     */
    public static Path config(Map<String, String> env, Path home) {
        String xdgConfig = env.get("XDG_CONFIG_HOME");
        if (xdgConfig == null || xdgConfig.isEmpty()) {
            return home.resolve(".config").resolve("ordenador").resolve("reglas.json");
        }
        return Path.of(xdgConfig).resolve("ordenador").resolve("reglas.json");
    }

    /**
     * Obtiene la ruta al archivo de registro de movimientos.
     * Prioriza XDG_DATA_HOME, si no existe o está vacío usa ~/.local/share.
     * Añade /ordenador/movimientos.log.
     */
    public static Path registro(Map<String, String> env, Path home) {
        String xdgData = env.get("XDG_DATA_HOME");
        if (xdgData == null || xdgData.isEmpty()) {
            return home.resolve(".local").resolve("share").resolve("ordenador").resolve("movimientos.log");
        }
        return Path.of(xdgData).resolve("ordenador").resolve("movimientos.log");
    }

    /**
     * Expande una ruta relativa o con '~' usando la carpeta home proporcionada.
     * Las rutas absolutas se devuelven tal cual.
     * Rutas relativas sin '~' (ej: "Descargas") lanzan IllegalArgumentException.
     */
    public static Path expandir(String ruta, Path home) {
        if (ruta.startsWith("~")) {
            // Reemplazar el primer ~ por la ruta home y limpiar barras dobles iniciales si las hubiera
            String expandida = ruta.replaceFirst("^~", home.toString());
            return Path.of(expandida).normalize();
        } else if (ruta.startsWith("/")) {
            // Ruta absoluta, devolver tal cual
            return Path.of(ruta);
        } else {
            // Ruta relativa sin ~ (ej: "Descargas")
            throw new IllegalArgumentException("Ruta relativa no permitida: " + ruta);
        }
    }
}
