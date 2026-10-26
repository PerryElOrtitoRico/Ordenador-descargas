<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 1319, max_tokens 6841 -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo esos cuerpos.
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

## 5. Archivos del programa
- Reglas: `$XDG_CONFIG_HOME/ordenador/reglas.json` (si no está definida: `~/.config/ordenador/reglas.json`). Si no existe, se crea con las reglas por defecto.
- Registro de movimientos: `$XDG_DATA_HOME/ordenador/movimientos.log` (si no: `~/.local/share/ordenador/movimientos.log`). Una línea por movimiento: `fecha ISO<TAB>origen<TAB>destino`.

---

# T04a — Rutas del programa

## Objetivo
Completar los tres métodos de `Rutas` (marcados `// TODO`) y sus dos tests.

## Reglas
- `config` y `registro`: si la variable de entorno es `null` o `""`, se usa la carpeta por defecto de `home`. Usá `Path.of(...)` y `resolve(...)`.
- `expandir`:
  - `"~"` → `home`.
  - `"~/x/y"` → `home.resolve("x/y")`.
  - empieza con `/` → `Path.of(ruta)`.
  - cualquier otra cosa (`"Descargas"`, `"~otro/x"`) → `throw new IllegalArgumentException("Ruta no válida (tiene que empezar con ~/ o /): " + ruta)`.

## Ejemplos (para los tests), con `HOME = Path.of("/home/f")`
| llamada | resultado |
|---|---|
| `Rutas.config(Map.of(), HOME)` | `/home/f/.config/ordenador/reglas.json` |
| `Rutas.config(Map.of("XDG_CONFIG_HOME", "/x"), HOME)` | `/x/ordenador/reglas.json` |
| `Rutas.registro(Map.of("XDG_DATA_HOME", ""), HOME)` | `/home/f/.local/share/ordenador/movimientos.log` |
| `Rutas.expandir("~/Imágenes", HOME)` | `/home/f/Imágenes` |
| `Rutas.expandir("~", HOME)` | `/home/f` |
| `Rutas.expandir("/tmp/a", HOME)` | `/tmp/a` |
| `Rutas.expandir("Descargas", HOME)` | `IllegalArgumentException` (usá `assertThrows`) |

---

# Archivos actuales

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
        // TODO
    }

    @Test
    void expandir() {
        // TODO
    }
}
```

Completá los `// TODO` que pide la tarea T04a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Rutas.java, src/test/java/com/ordenador/service/RutasTest.java.
