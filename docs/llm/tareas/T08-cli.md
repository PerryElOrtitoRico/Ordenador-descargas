# T08 — Comandos de consola y servicio de systemd

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar. La lógica va en `service`; acá solo se lee la línea de comandos y se llama a lo que ya existe.

## Código que ya existe (T01–T07), todo en `com.ordenador.service` salvo `Config`
```java
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }   // com.ordenador.model
public final class ConfigRepo { public static Config cargarOCrear(Path archivo) throws IOException; }
public final class Rutas {
    public static Path config(Map<String, String> env, Path home);
    public static Path registro(Map<String, String> env, Path home);
    public static Path expandir(String ruta, Path home);
}
public final class Registro {
    public Registro(Path archivo);
    public Optional<Movimiento> deshacerUltimo() throws IOException;   // Movimiento(fecha, origen, destino)
}
public interface Notificador { void notificar(String titulo, String cuerpo); }
public record Resultado(Path archivo, Estado estado, Path destino, String mensaje) {
    public enum Estado { MOVIDO, AVISADO, SIN_REGLA, IGNORADO }
}
public final class Ordenador {
    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular);
    public Resultado procesar(Path archivo) throws IOException;
    public List<Resultado> ordenarCarpeta() throws IOException;
}
public final class Papelera {
    public interface Tacho { void tirar(Path archivo) throws IOException; }
    public static final Tacho GIO;
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

## Archivos
- Crear `src/main/java/com/ordenador/cli/Argumentos.java`
- Crear `src/main/java/com/ordenador/cli/NotifySend.java`
- Crear `src/main/java/com/ordenador/cli/Main.java`
- Crear `src/test/java/com/ordenador/cli/ArgumentosTest.java`
- Crear `systemd/ordenador.service`

## Qué escribir
```java
package com.ordenador.cli;

public record Argumentos(String comando, boolean simular) {
    /** Comandos: vigilar, ordenar, deshacer, resumen, limpiar, ayuda. "--simular" puede ir antes o después
     *  del comando y solo vale con vigilar, ordenar y limpiar. "-h" y "--help" = ayuda.
     *  Lanza IllegalArgumentException si: no hay comando, hay más de uno, el comando o la opción no existen,
     *  o --simular va con otro comando. */
    public static Argumentos parsear(String[] args) { ... }
}

/** Ejecuta: notify-send -a Ordenador -i folder-downloads <titulo> <cuerpo>. Si falla, lo escribe en System.err y sigue. */
public final class NotifySend implements Notificador { ... }

public final class Main {
    public static void main(String[] args) {
        System.exit(ejecutar(args, System.getenv(), Path.of(System.getProperty("user.home")),
                System.out, new NotifySend(), Papelera.GIO));
    }
    /** Devuelve el código de salida: 0 bien, 1 error (IOException o IllegalArgumentException al ejecutar), 2 argumentos inválidos. */
    public static int ejecutar(String[] args, Map<String, String> env, Path home, PrintStream out,
                               Notificador notificador, Papelera.Tacho tacho) { ... }
}
```

`ejecutar`:
- Argumentos inválidos → `out.println(e.getMessage())`, el texto de uso, y devuelve 2. `ayuda` → texto de uso, 0.
- Los demás: `config = ConfigRepo.cargarOCrear(Rutas.config(env, home))`, `registro = new Registro(Rutas.registro(env, home))`, `carpeta = Rutas.expandir(config.carpeta(), home)`, `pre = simular ? "[simulación] " : ""`.
- `ordenar`: `new Ordenador(...).ordenarCarpeta()` y una línea por resultado: MOVIDO → `pre + "mover: " + mensaje`; AVISADO → `pre + "aviso: " + mensaje`; SIN_REGLA → `pre + "sin regla: " + nombre`; IGNORADO → nada.
- `vigilar`: imprime `"Vigilando " + carpeta`; primero `limpiar`; un `Vigilante(carpeta, config.segundosQuieto() * 1000L, ...)` que por cada archivo llama a `procesar` e imprime como en `ordenar` (los errores de un archivo se imprimen y se sigue); `correr()`.
- `limpiar`: `Papelera.limpiar(config, home, Instant.now(), tacho, simular)`; una línea `pre + "a la papelera: " + ruta` por cada una.
- `deshacer`: si hay → `"deshecho: " + origen + " → " + destino` del movimiento devuelto; si no → `"No hay movimientos para deshacer."`.
- `resumen`: `texto = Resumen.texto(Resumen.sinOrdenar(carpeta))`, `out.println(texto)` y, si la lista no está vacía, `notificador.notificar("Ordenador", texto)`.

`systemd/ordenador.service` (servicio de usuario; no se instala en esta tarea):
```ini
[Unit]
Description=Ordenador de Descargas
After=graphical-session.target
PartOf=graphical-session.target

[Service]
ExecStart=/usr/bin/java -jar %h/.local/lib/ordenador/ordenador.jar vigilar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=graphical-session.target
```

## Tests a escribir (como mínimo)
- `parsear(["ordenar", "--simular"])` y `parsear(["--simular", "ordenar"])` → `("ordenar", true)`; `["deshacer"]` → `("deshacer", false)`; `["-h"]` → ayuda.
- Lanzan `IllegalArgumentException`: `[]`, `["borrar"]`, `["ordenar", "resumen"]`, `["deshacer", "--simular"]`, `["ordenar", "--rapido"]`.
