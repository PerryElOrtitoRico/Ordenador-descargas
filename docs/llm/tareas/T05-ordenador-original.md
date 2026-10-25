# T05 — Ordenador: aplicar la regla a un archivo

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Dado un archivo de la carpeta vigilada, aplicarle la acción de su regla (sección 4 de la especificación), o solo informar en modo simulación.

## Código que ya existe (T01–T04)
```java
package com.ordenador.model;
public enum Accion { MOVER, AVISAR }
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }

package com.ordenador.service;
public final class Nombres {
    public static boolean esTemporal(String nombre);
    public static Path nombreLibre(Path carpeta, String nombre);   // no pisa: "x (2).pdf"…
}
public record Coincidencia(Regla regla, String palabra) { }       // palabra: la que coincidió, o null
public final class Clasificador {
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas);
}
public final class Rutas {
    public static Path expandir(String ruta, Path home);           // "~/x" → home/x
}
public final class Registro {
    public Registro(Path archivo);
    public void anotar(Movimiento m) throws IOException;
    public List<Movimiento> leer() throws IOException;
}
```

## Archivos
- Crear `src/main/java/com/ordenador/service/Notificador.java`
- Crear `src/main/java/com/ordenador/service/Resultado.java`
- Crear `src/main/java/com/ordenador/service/Ordenador.java`
- Crear `src/test/java/com/ordenador/service/OrdenadorTest.java`

## Qué escribir
```java
package com.ordenador.service;

public interface Notificador {
    void notificar(String titulo, String cuerpo);
}

public record Resultado(Path archivo, Estado estado, Path destino, String mensaje) {
    public enum Estado { MOVIDO, AVISADO, SIN_REGLA, IGNORADO }
}

public final class Ordenador {
    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular) { ... }
    public Resultado procesar(Path archivo) throws IOException { ... }
    /** procesar() de cada archivo regular de primer nivel de Rutas.expandir(config.carpeta(), home),
     *  ordenados por nombre; las subcarpetas no se tocan. Lista vacía si la carpeta no existe. */
    public List<Resultado> ordenarCarpeta() throws IOException { ... }
}
```

`procesar(archivo)`, con `nombre = archivo.getFileName().toString()`:
1. Si no es un archivo regular (`Files.isRegularFile`) o `Nombres.esTemporal(nombre)` → `IGNORADO` (destino y mensaje null).
2. Sin regla (`clasificar` vacío) → `SIN_REGLA` (destino y mensaje null).
3. `AVISAR` → `mensaje = regla.mensaje().replace("{palabra}", palabra)` (palabra null → `""`); si no se simula, `notificador.notificar("Ordenador", nombre + ": " + mensaje)`. Devuelve `AVISADO`, destino null.
4. `MOVER` → `carpeta = Rutas.expandir(regla.destino(), home)`.
   - Si `archivo.getParent()` es igual a `carpeta` → `IGNORADO` (ya está en su lugar).
   - `destino = Nombres.nombreLibre(carpeta, nombre)`, `mensaje = nombre + " → " + destino`.
   - Si se simula: devolver `MOVIDO` con ese destino y mensaje **sin tocar nada**.
   - Si no: `Files.createDirectories(carpeta)`, `Files.move(archivo, destino)` (sin `REPLACE_EXISTING`); si `regla.diasPapelera() > 0`, `Files.setLastModifiedTime(destino, FileTime.from(Instant.now()))`; `registro.anotar(new Movimiento(LocalDateTime.now(), archivo, destino))`; `notificador.notificar("Ordenador", mensaje)`. Devuelve `MOVIDO`.

## Tests a escribir (como mínimo)
Con `@TempDir Path home`, carpeta `home/Descargas`, un `Registro` sobre `home/log.txt` y un notificador falso que guarda los cuerpos en una lista:
- `foto.PNG` con una regla de imágenes a `~/Imágenes` → queda en `home/Imágenes/foto.PNG`, hay una notificación y `registro.leer()` tiene 1 movimiento.
- Si `home/Imágenes/foto.PNG` ya existía → `foto (2).PNG`, y el original no se pisa.
- Simulando: devuelve `MOVIDO` con el destino previsto, pero el archivo sigue en Descargas, `Imágenes` no se creó, no hay notificaciones ni movimientos.
- `AVISAR` con mensaje `"Subilo a {palabra}/teoria"` y palabra `"FISICA"` → mensaje `"Subilo a FISICA/teoria"`, el archivo no se movió.
- `video.mp4.part` → `IGNORADO`; `nada.xyz` → `SIN_REGLA`.
