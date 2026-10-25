# T06 — Papelera de instaladores y resumen semanal

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
- Mandar a la papelera lo que lleva `diasPapelera` días en la carpeta de su regla (por ejemplo los instaladores, a los 30 días). **Nunca borrar**: se usa `gio trash`.
- Listar lo que quedó sin ordenar en Descargas.

## Código que ya existe (T01–T04)
```java
package com.ordenador.model;
public enum Accion { MOVER, AVISAR }
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }

package com.ordenador.service;
public final class Nombres {
    public static boolean esTemporal(String nombre);  // .part, .crdownload, .tmp o empieza con "."
}
public final class Rutas {
    public static Path expandir(String ruta, Path home);  // "~/x" → home/x
}
```

## Archivos
- Crear `src/main/java/com/ordenador/service/Papelera.java`
- Crear `src/main/java/com/ordenador/service/Resumen.java`
- Crear `src/test/java/com/ordenador/service/PapeleraTest.java`
- Crear `src/test/java/com/ordenador/service/ResumenTest.java`

## Qué escribir
```java
package com.ordenador.service;

public final class Papelera {
    private Papelera() { }

    /** Quien manda un archivo a la papelera. En los tests se usa uno falso que solo anota. */
    public interface Tacho {
        void tirar(Path archivo) throws IOException;
    }

    /** Ejecuta: gio trash <ruta absoluta> (ProcessBuilder, esperar con waitFor).
     *  Si el código de salida no es 0 → IOException con la ruta. */
    public static final Tacho GIO = archivo -> { ... };

    /** Archivos regulares de primer nivel de la carpeta cuya fecha de modificación es
     *  anterior o igual a ahora − dias (Duration.ofDays). Ordenados por nombre.
     *  Lista vacía si dias <= 0 o la carpeta no existe. */
    public static List<Path> vencidos(Path carpeta, int dias, Instant ahora) throws IOException { ... }

    /** Para cada regla MOVER con diasPapelera > 0: vencidos(Rutas.expandir(destino, home), diasPapelera, ahora).
     *  Si no se simula, tacho.tirar(...) de cada uno. Devuelve todos los vencidos encontrados, en orden de regla. */
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException { ... }
}

public final class Resumen {
    private Resumen() { }
    /** Nombres de los archivos regulares de primer nivel que no son temporales, en orden alfabético.
     *  Lista vacía si la carpeta no existe. */
    public static List<String> sinOrdenar(Path carpeta) throws IOException { ... }
    /** Vacía → "Descargas está ordenada.".
     *  Uno → "Queda 1 archivo sin ordenar en Descargas:\n- a.pdf"
     *  Varios → "Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt" (sin "\n" al final). */
    public static String texto(List<String> nombres) { ... }
}
```

## Tests a escribir (como mínimo)
Con `@TempDir Path home` y la carpeta `home/Descargas/Instaladores`:
- Un archivo con fecha de modificación de hace 31 días (`Files.setLastModifiedTime`) y otro de hace 2 días: `vencidos(..., 30, Instant.now())` devuelve solo el primero.
- `limpiar` con una regla MOVER `~/Descargas/Instaladores` y `diasPapelera` 30, y un `Tacho` falso que guarda las rutas en una lista: lo vencido llega al tacho; simulando, el tacho no recibe nada pero se devuelve la misma lista.
- Una regla con `diasPapelera` 0 no se limpia nunca.
- `sinOrdenar` con `b.zip`, `a.pdf`, `x.part`, `.oculto` y una subcarpeta → `["a.pdf", "b.zip"]`.
- Los tres casos de `texto`.

## No hagas
- No uses `Files.delete` ni `File.delete`.
