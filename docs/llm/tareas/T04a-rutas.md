<!-- contrato: 1,5 | archivos: src/main/java/com/ordenador/service/Rutas.java, src/test/java/com/ordenador/service/RutasTest.java -->
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
