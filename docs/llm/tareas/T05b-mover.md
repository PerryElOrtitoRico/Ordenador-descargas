<!-- contrato: 1,2,4 | archivos: src/main/java/com/ordenador/service/Ordenador.java, src/test/java/com/ordenador/service/OrdenadorTest.java -->
# T05b — Ordenador: mover

## Objetivo
Completar **solo** `mover` de `Ordenador` (`procesar` y `avisar` ya están: no los cambies; `ordenarCarpeta` queda con su `// TODO`) y los tests `mueveAnotaYNotifica`, `noPisa` y `simularNoTocaNada`.

## Código que ya existe (no lo cambies)
```java
package com.ordenador.service;
public final class Nombres {
    public static boolean esTemporal(String nombre);              // .part, .crdownload, .tmp o empieza con "."
    public static Path nombreLibre(Path carpeta, String nombre);  // carpeta/nombre, o "x (2).pdf"… si ya existe; devuelve Path
}
public record Coincidencia(Regla regla, String palabra) { }       // palabra: la que coincidió, o null
public final class Clasificador {
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas);
}
public final class Rutas {
    public static Path expandir(String ruta, Path home);           // "~/x" → home/x
}
public final class Registro {
    public void anotar(Movimiento m) throws IOException;
    public List<Movimiento> leer() throws IOException;
}
```
Los records se leen así: `c.regla()`, `c.palabra()`, `regla.accion()`, `regla.destino()`, `regla.mensaje()`, `regla.diasPapelera()`, `config.carpeta()`, `config.reglas()`.

## `mover(archivo, c)`, en este orden
1. `String nombre = archivo.getFileName().toString();`
2. `Path carpeta = Rutas.expandir(c.regla().destino(), home);`
3. `if (carpeta.equals(archivo.getParent())) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);` (ya está en su lugar)
4. `Path destino = Nombres.nombreLibre(carpeta, nombre);`
5. `String mensaje = nombre + " → " + destino;`
6. `if (simular) return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);` — simulando **no se toca nada**.
7. `Files.createDirectories(carpeta);`
8. `Files.move(archivo, destino);` (sin `REPLACE_EXISTING`)
9. `if (c.regla().diasPapelera() > 0) Files.setLastModifiedTime(destino, FileTime.from(Instant.now()));`
10. `registro.anotar(new Movimiento(LocalDateTime.now(), archivo, destino));`
11. `notificador.notificar("Ordenador", mensaje);`
12. `return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);`

## Tests (usá `preparar()`, `ordenador(...)`, `registro` y `avisos` que ya están en el test)
- `mueveAnotaYNotifica`: `descargas/foto.PNG` → queda en `home.resolve("Imágenes/foto.PNG")`, ya no está en `descargas`, `avisos.size()` es 1 y `registro.leer().size()` es 1.
- `noPisa`: con `home/Imágenes/foto.png` ya creado (contenido `"viejo"`), mover `descargas/foto.png` (contenido `"nuevo"`) → destino `Imágenes/foto (2).png`; el viejo sigue diciendo `"viejo"`.
- `simularNoTocaNada`: con `ordenador(true)`, `descargas/a.jpg` → estado `MOVIDO` y destino `home.resolve("Imágenes/a.jpg")`, pero `a.jpg` sigue en `descargas`, `home/Imágenes` no existe, `avisos` y `registro.leer()` vacíos.
