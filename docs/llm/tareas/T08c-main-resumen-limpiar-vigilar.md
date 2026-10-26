<!-- contrato: 1,4,6 | archivos: src/main/java/com/ordenador/cli/Main.java, src/main/java/com/ordenador/cli/NotifySend.java, src/test/java/com/ordenador/cli/MainTest.java -->
# T08c — Main: resumen, limpiar, vigilar y notificaciones

## Objetivo
Completar `resumen`, `limpiar` y `vigilar` de `Main`, `notificar` de `NotifySend` y el test `resumenConUnArchivo`. Lo demás de `Main` (`ejecutar`, `linea`, `ordenar`, `deshacer`) ya está: no lo cambies.

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

## `resumen(carpeta, n, out)`
`List<String> nombres = Resumen.sinOrdenar(carpeta); String texto = Resumen.texto(nombres); out.println(texto); if (!nombres.isEmpty()) n.notificar("Ordenador", texto);`

## `limpiar(config, home, tacho, simular, out)`
`String pre = simular ? "[simulación] " : "";` → `for (Path p : Papelera.limpiar(config, home, Instant.now(), tacho, simular)) out.println(pre + "a la papelera: " + p);`

## `vigilar(...)`
1. `out.println("Vigilando " + carpeta + (simular ? " (simulación: no se mueve nada)" : ""));`
2. `limpiar(config, home, tacho, simular, out);`
3. `Ordenador ordenador = new Ordenador(config, home, registro, n, simular);`
4. `try (Vigilante v = new Vigilante(carpeta, config.segundosQuieto() * 1000L, archivo -> { … })) { v.correr(); }` donde la lambda hace:
   `try { String l = linea(ordenador.procesar(archivo), simular); if (l != null) out.println(l); } catch (IOException e) { out.println("Error con " + archivo + ": " + e.getMessage()); }`

## `NotifySend.notificar(titulo, cuerpo)`
`try { new ProcessBuilder("notify-send", "-a", "Ordenador", "-i", "folder-downloads", titulo, cuerpo).start(); } catch (IOException e) { System.err.println("No se pudo notificar: " + e.getMessage()); }`

## Test (usá `correr(...)`, `home` y `avisos` que ya están en el test)
- `resumenConUnArchivo`: crear `home/Descargas/raro.xyz`; `correr(0, "resumen")` contiene `"Queda 1 archivo sin ordenar en Descargas:\n- raro.xyz"` y `avisos.size()` es 1.
