<!-- contrato: 1,2,4 | archivos: src/main/java/com/ordenador/service/Ordenador.java, src/test/java/com/ordenador/service/OrdenadorTest.java -->
# T05a — Ordenador: decidir y avisar

## Objetivo
Completar **solo** `procesar` y `avisar` de `Ordenador`, y los tests `avisarNoMueve` y `temporalIgnoradoYSinRegla`. `mover` y `ordenarCarpeta` quedan con su `// TODO`.

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

## `procesar(archivo)`, en este orden
1. `String nombre = archivo.getFileName().toString();`
2. `if (!Files.isRegularFile(archivo) || Nombres.esTemporal(nombre)) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);`
3. `Optional<Coincidencia> c = Clasificador.clasificar(nombre, config.reglas());`
4. `if (c.isEmpty()) return new Resultado(archivo, Resultado.Estado.SIN_REGLA, null, null);`
5. `if (c.get().regla().accion() == Accion.AVISAR) return avisar(archivo, c.get());`
6. `return mover(archivo, c.get());`

## `avisar(archivo, c)`
1. `String palabra = c.palabra() == null ? "" : c.palabra();`
2. `String mensaje = c.regla().mensaje().replace("{palabra}", palabra);`
3. `if (!simular) notificador.notificar("Ordenador", archivo.getFileName() + ": " + mensaje);`
4. `return new Resultado(archivo, Resultado.Estado.AVISADO, null, mensaje);`

## Tests (usá `preparar()`, `ordenador(false)` y la lista `avisos` que ya están en el test)
- `avisarNoMueve`: crear `descargas/Fisica_TP1.pdf` con `Files.writeString`; `procesar` → estado `AVISADO`, mensaje `"Subilo a FISICA/teoria"`, `avisos` = `List.of("Fisica_TP1.pdf: Subilo a FISICA/teoria")` y el archivo sigue existiendo.
- `temporalIgnoradoYSinRegla`: `video.mp4.part` → `IGNORADO`; `nada.xyz` → `SIN_REGLA`; los dos siguen en `descargas` y `avisos` queda vacía.
