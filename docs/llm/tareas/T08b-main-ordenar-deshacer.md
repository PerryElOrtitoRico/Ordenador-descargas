<!-- contrato: 1,4,6 | archivos: src/main/java/com/ordenador/cli/Main.java, src/test/java/com/ordenador/cli/MainTest.java -->
# T08b — Main: ejecutar, ordenar y deshacer

## Objetivo
Completar **solo** `ejecutar`, `linea`, `ordenar` y `deshacer` de `Main`, y el test `ordenarSimulandoNoMueve`. Los `// TODO T08c` quedan como están.

## Código que ya existe (no lo cambies)
```java
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }      // config.carpeta(), config.segundosQuieto()
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }          // m.origen(), m.destino()
public final class ConfigRepo { public static Config cargarOCrear(Path archivo) throws IOException; }
public final class Rutas {
    public static Path config(Map<String, String> env, Path home);
    public static Path registro(Map<String, String> env, Path home);
    public static Path expandir(String ruta, Path home);
}
public final class Registro { public Registro(Path archivo); public Optional<Movimiento> deshacerUltimo() throws IOException; }
public record Resultado(Path archivo, Estado estado, Path destino, String mensaje) {   // r.estado(), r.mensaje(), r.archivo()
    public enum Estado { MOVIDO, AVISADO, SIN_REGLA, IGNORADO }
}
public final class Ordenador {
    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular);
    public Resultado procesar(Path archivo) throws IOException;
    public List<Resultado> ordenarCarpeta() throws IOException;
}
public final class Papelera {
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException;
}
public final class Resumen {
    public static List<String> sinOrdenar(Path carpeta) throws IOException;
    public static String texto(List<String> nombres);
}
public final class Vigilante implements AutoCloseable {
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException;
    public void correr();
}
```

## `ejecutar(args, env, home, out, notificador, tacho)`
1. `Argumentos a;` → `try { a = Argumentos.parsear(args); } catch (IllegalArgumentException e) { out.println(e.getMessage()); out.println(USO); return 2; }`
2. `if (a.comando().equals("ayuda")) { out.println(USO); return 0; }`
3. Dentro de `try { … return 0; } catch (IOException | IllegalArgumentException e) { out.println("Error: " + e.getMessage()); return 1; }`:
   - `Config config = ConfigRepo.cargarOCrear(Rutas.config(env, home));`
   - `Registro registro = new Registro(Rutas.registro(env, home));`
   - `Path carpeta = Rutas.expandir(config.carpeta(), home);`
   - `switch (a.comando())`: `"ordenar"` → `ordenar(config, home, registro, notificador, a.simular(), out)`; `"deshacer"` → `deshacer(registro, out)`; `"resumen"` → `resumen(carpeta, notificador, out)`; `"limpiar"` → `limpiar(config, home, tacho, a.simular(), out)`; `"vigilar"` → `vigilar(config, home, carpeta, registro, notificador, tacho, a.simular(), out)`.

## `linea(r, simular)`
`String pre = simular ? "[simulación] " : "";` y `switch (r.estado())`: `MOVIDO` → `pre + "mover: " + r.mensaje()`; `AVISADO` → `pre + "aviso: " + r.mensaje()`; `SIN_REGLA` → `pre + "sin regla: " + r.archivo().getFileName()`; `IGNORADO` → `null`.

## `ordenar(...)`
`for (Resultado r : new Ordenador(config, home, registro, n, simular).ordenarCarpeta())` → `String l = linea(r, simular); if (l != null) out.println(l);`

## `deshacer(registro, out)`
`Optional<Movimiento> m = registro.deshacerUltimo();` → si está: `out.println("deshecho: " + m.get().origen() + " → " + m.get().destino());`; si no: `out.println("No hay movimientos para deshacer.");`

## Test (usá `correr(...)` y `home` que ya están en el test)
- `ordenarSimulandoNoMueve`: `Files.createDirectories(home.resolve("Descargas"))` y crear `Descargas/foto.png`; `String salida = correr(0, "ordenar", "--simular");` → `salida` contiene `"[simulación] mover: foto.png"`, `Descargas/foto.png` sigue existiendo y `home/Imágenes` no existe.
