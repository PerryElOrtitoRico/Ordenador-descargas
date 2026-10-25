# T07 — Vigilar la carpeta y esperar a que el archivo deje de cambiar

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas (para vigilar se usa `java.nio.file.WatchService`), no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Avisar cuando un archivo de la carpeta lleva un rato sin cambiar (terminó de bajarse), sin avisar por temporales ni por archivos que el navegador todavía está bajando.

## Código que ya existe (T01)
```java
package com.ordenador.service;
public final class Nombres {
    public static boolean esTemporal(String nombre);  // .part, .crdownload, .tmp o empieza con "."
}
```

## Archivos
- Crear `src/main/java/com/ordenador/service/Pendientes.java`
- Crear `src/main/java/com/ordenador/service/Vigilante.java`
- Crear `src/test/java/com/ordenador/service/PendientesTest.java`
- Crear `src/test/java/com/ordenador/service/VigilanteTest.java`

## Qué escribir
```java
package com.ordenador.service;

/** Lógica pura, sin reloj propio: el tiempo se pasa en milisegundos. No es thread-safe. */
public final class Pendientes {
    public Pendientes(long quietoMs) { ... }
    /** Anota que el archivo cambió en ahoraMs (si ya estaba, actualiza la hora). */
    public void tocar(Path archivo, long ahoraMs) { ... }
    /** Saca y devuelve los archivos cuyo último cambio fue hace quietoMs o más
     *  (ahoraMs - ultimo >= quietoMs), ordenados por ruta. */
    public List<Path> listos(long ahoraMs) { ... }
    public int cantidad() { ... }
}

public final class Vigilante implements AutoCloseable {
    /** Abre un WatchService y registra la carpeta para ENTRY_CREATE y ENTRY_MODIFY (sin subcarpetas). */
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException { ... }

    /** Bucle hasta close() o interrupción del hilo:
     *  1. watchService.poll(200, MILLISECONDS); por cada evento (salteando OVERFLOW), ruta = carpeta.resolve(contexto);
     *     si !Nombres.esTemporal(nombre) → pendientes.tocar(ruta, System.currentTimeMillis()); después key.reset().
     *  2. Por cada ruta de pendientes.listos(System.currentTimeMillis()):
     *     si descargaEnCurso(ruta) → volver a tocarla (sigue esperando);
     *     si no, si Files.isRegularFile(ruta) → alEstarListo.accept(ruta).
     *  ClosedWatchServiceException o InterruptedException → salir del bucle sin lanzar nada
     *  (en la interrupción, volver a marcar Thread.currentThread().interrupt()). */
    public void correr() { ... }

    /** true si existe al lado "<nombre>.part" o "<nombre>.crdownload"
     *  (Firefox crea el archivo final vacío mientras baja el .part). */
    public static boolean descargaEnCurso(Path archivo) { ... }

    @Override public void close() throws IOException { ... }
}
```

## Tests a escribir (como mínimo)
- `Pendientes(1000)`: `tocar(a, 0)`; `listos(999)` vacío; `listos(1000)` = `[a]`; después `cantidad()` = 0.
- Tocar de nuevo reinicia la espera: `tocar(a, 0)`, `tocar(a, 800)`, `listos(1500)` vacío, `listos(1800)` = `[a]`.
- `descargaEnCurso`: con `@TempDir`, `x.pdf` y `x.pdf.part` → `true`; solo `x.pdf` → `false`.
- `Vigilante` con `@TempDir`, `quietoMs` 300, corriendo en un `Thread` aparte, un consumer que agrega a una lista sincronizada:
  crear `a.txt` → en menos de 5 s llega `a.txt` una sola vez; crear `b.txt.part` → no llega nada por él.
  Al final `close()` y `join` del hilo.
