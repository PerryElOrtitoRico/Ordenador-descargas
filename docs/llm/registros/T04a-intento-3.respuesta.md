```java
// src/main/java/com/ordenador/service/Rutas.java
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
        if (ruta.startsWith("~/")) {
            String resto = ruta.substring(2); // Se quita el "~" y el "/" inicial
            return home.resolve(resto);
        } else if (ruta.startsWith("/")) {
            return Path.of(ruta);
        } else {
            throw new IllegalArgumentException("Ruta no válida (tiene que empezar con ~/ o /): " + ruta);
        }
    }
}
```

```java
// src/test/java/com/ordenador/service/RutasTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RutasTest {

    private static final Path HOME = Path.of("/home/f");

    @Test
    void configYRegistroConYSinXdg() {
        // Sin variables de entorno: usa carpetas por defecto en home
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of(), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of(), HOME));

        // Con XDG_CONFIG_HOME definida
        assertEquals(Path.of("/x/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", "/x"), HOME));

        // Con XDG_DATA_HOME vacía (debe usar la por defecto)
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", ""), HOME));
    }

    @Test
    void expandir() {
        // Ruta con ~
        assertEquals(Path.of("/home/f/Imágenes"), Rutas.expandir("~/Imágenes", HOME));
        // Solo ~
        assertEquals(Path.of("/home/f"), Rutas.expandir("~", HOME));
        // Ruta absoluta
        assertEquals(Path.of("/tmp/a"), Rutas.expandir("/tmp/a", HOME));
        // Ruta relativa (no válida)
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", HOME));
        // Ruta con ~ pero no al inicio (no válida)
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("~otro/x", HOME));
    }
}
```

<!-- meta: {"segundos": 86.3, "finish_reason": "stop", "usage": {"prompt_tokens": 1980, "completion_tokens": 911, "total_tokens": 2891, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
