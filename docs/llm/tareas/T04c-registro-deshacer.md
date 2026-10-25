<!-- contrato: 1,2,5 | archivos: src/main/java/com/ordenador/service/Registro.java, src/test/java/com/ordenador/service/RegistroTest.java -->
# T04c — Registro: deshacer el último movimiento

## Objetivo
Completar **solo** `deshacerUltimo` de `Registro` (`anotar` y `leer` ya están hechos: no los cambies) y los tests `deshacerVuelveAlOrigen`, `deshacerNoPisa` y `deshacerSinRegistro`.

## Código que ya existe
```java
package com.ordenador.service;
public final class Nombres {
    /** carpeta.resolve(nombre) si está libre; si no, "base (2).ext", "base (3).ext"… Devuelve un Path. */
    public static Path nombreLibre(Path carpeta, String nombre);
}
```

## Pasos de `deshacerUltimo`
1. `List<Movimiento> todos = leer();` si está vacía → `return Optional.empty();`
2. `Movimiento m = todos.get(todos.size() - 1);`
3. Si `!Files.exists(m.destino())` → `throw new IOException("ya no está: " + m.destino());` (sin tocar el registro).
4. `Path carpeta = m.origen().getParent();` → `Files.createDirectories(carpeta);`
5. `Path adonde = Files.exists(m.origen()) ? Nombres.nombreLibre(carpeta, m.origen().getFileName().toString()) : m.origen();`
6. `Files.move(m.destino(), adonde);`
7. Reescribir el registro con `todos` menos el último: cada uno como `fecha + "\t" + origen + "\t" + destino + "\n"`, escrito en
   `Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp")` y después `Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING)`.
8. `return Optional.of(new Movimiento(LocalDateTime.now(), m.destino(), adonde));`

## Tests
- `deshacerVuelveAlOrigen`: crear `d/b/x.pdf`, anotar `new Movimiento(LocalDateTime.now(), d.resolve("a/x.pdf"), d.resolve("b/x.pdf"))`, `deshacerUltimo()` → `d/a/x.pdf` existe, `d/b/x.pdf` no, y `leer()` vacío.
- `deshacerNoPisa`: igual, pero con `d/a/x.pdf` ya creado antes de deshacer → vuelve como `d/a/x (2).pdf`.
- `deshacerSinRegistro`: `deshacerUltimo()` sin nada anotado → `Optional.empty()`.
