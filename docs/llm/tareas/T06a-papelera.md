<!-- contrato: 1,2,4 | archivos: src/main/java/com/ordenador/service/Papelera.java, src/test/java/com/ordenador/service/PapeleraTest.java -->
# T06a — Papelera de lo vencido

## Objetivo
Completar `GIO`, `vencidos` y `limpiar` de `Papelera` y sus tres tests. **Nunca borrar**: no uses `Files.delete` ni `File.delete`.

## Código que ya existe (no lo cambies)
```java
public final class Rutas {
    public static Path expandir(String ruta, Path home);   // "~/x" → home/x
}
```
Los records se leen así: `config.reglas()`, `regla.accion()`, `regla.destino()`, `regla.diasPapelera()`.

## `GIO` (cuerpo de la lambda)
1. `Process p;` dentro de `try`: `p = new ProcessBuilder("gio", "trash", archivo.toAbsolutePath().toString()).inheritIO().start();`
2. `int codigo = p.waitFor();` — si hay `InterruptedException`: `Thread.currentThread().interrupt(); throw new IOException("Interrumpido al mandar a la papelera: " + archivo, e);`
3. `if (codigo != 0) throw new IOException("gio trash falló (" + codigo + "): " + archivo);`

## `vencidos(carpeta, dias, ahora)`
1. `if (dias <= 0 || !Files.isDirectory(carpeta)) return new ArrayList<>();`
2. `Instant limite = ahora.minus(Duration.ofDays(dias));`
3. `try (Stream<Path> s = Files.list(carpeta)) {` filtrar `Files::isRegularFile` y los que cumplen `!Files.getLastModifiedTime(p).toInstant().isAfter(limite)` (como `getLastModifiedTime` lanza `IOException`, hacelo en un `for` sobre `s.sorted().toList()`, no dentro de una lambda) `}`
4. Devolver la lista (ya queda ordenada por nombre por el `sorted()`).

## `limpiar(config, home, ahora, tacho, simular)`
1. `List<Path> todos = new ArrayList<>();`
2. `for (Regla r : config.reglas())`: si `r.accion() == Accion.MOVER && r.diasPapelera() > 0`:
   `List<Path> v = vencidos(Rutas.expandir(r.destino(), home), r.diasPapelera(), ahora);` → `todos.addAll(v);` y, si `!simular`, `for (Path p : v) tacho.tirar(p);`
3. `return todos;`

## Tests (usá `AHORA` y `archivo(...)` que ya están en el test; `@TempDir Path home`)
- `vencidosSoloLosViejos`: en `home/Descargas/Instaladores`, `viejo.deb` de hace 31 días y `nuevo.deb` de hace 2 → `vencidos(..., 30, AHORA)` es `List.of(viejo)`.
- `limpiarUsaElTachoYRespetaSimular`: `Config("~/Descargas", 5, List.of(new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30)))` y un tacho falso `List<Path> tirados = new ArrayList<>(); Papelera.Tacho t = tirados::add;`. Con `simular = true`: devuelve `[viejo]` y `tirados` vacía; con `false`: `tirados` = `[viejo]`, y `viejo` **sigue existiendo** (el tacho falso no mueve nada).
- `diasCeroNoSeLimpia`: la misma regla con `diasPapelera` 0 → `limpiar` devuelve lista vacía.
