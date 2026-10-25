# T04 — Rutas del programa y registro de movimientos

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Saber dónde van los archivos del programa (sección 5) y llevar el registro de movimientos para poder deshacer el último.

## Código que ya existe (T01)
```java
package com.ordenador.service;
public final class Nombres {
    /** carpeta.resolve(nombre) si está libre; si no, "base (2).ext", "base (3).ext"… */
    public static Path nombreLibre(Path carpeta, String nombre);
}
```

## Archivos
- Crear `src/main/java/com/ordenador/model/Movimiento.java` (exactamente como en la especificación)
- Crear `src/main/java/com/ordenador/service/Rutas.java`
- Crear `src/main/java/com/ordenador/service/Registro.java`
- Crear `src/test/java/com/ordenador/service/RutasTest.java`
- Crear `src/test/java/com/ordenador/service/RegistroTest.java`

## Qué escribir
```java
package com.ordenador.service;

public final class Rutas {
    private Rutas() { }
    /** env.get("XDG_CONFIG_HOME") (si es null o vacía: home/.config) + "/ordenador/reglas.json". */
    public static Path config(Map<String, String> env, Path home) { ... }
    /** env.get("XDG_DATA_HOME") (si es null o vacía: home/.local/share) + "/ordenador/movimientos.log". */
    public static Path registro(Map<String, String> env, Path home) { ... }
    /** "~" → home; "~/x/y" → home/x/y; una ruta absoluta queda igual;
     *  cualquier otra (relativa, "~otro") → IllegalArgumentException con la ruta en el mensaje. */
    public static Path expandir(String ruta, Path home) { ... }
}

public final class Registro {
    public Registro(Path archivo) { ... }
    /** Crea las carpetas si hace falta y agrega al final la línea
     *  fecha.toString() + "\t" + origen + "\t" + destino + "\n" (UTF-8, StandardOpenOption.CREATE y APPEND). */
    public void anotar(Movimiento m) throws IOException { ... }
    /** Los movimientos en orden (el más viejo primero). Lista vacía si el archivo no existe.
     *  Ignora líneas en blanco y líneas que no tienen 3 partes o cuya fecha no se puede leer (LocalDateTime.parse). */
    public List<Movimiento> leer() throws IOException { ... }
    /** Deshace el último movimiento leído:
     *  - si no hay ninguno → Optional.empty();
     *  - si m.destino() ya no existe → IOException ("ya no está: <destino>") y el registro no cambia;
     *  - si no: crea la carpeta de m.origen() si falta y mueve m.destino() a m.origen()
     *    (o a Nombres.nombreLibre(carpeta de origen, nombre de origen) si ahí ya hay otro archivo);
     *    reescribe el registro sin esa última línea (atómico: "<archivo>.tmp" + Files.move REPLACE_EXISTING);
     *    devuelve Optional.of(new Movimiento(ahora, m.destino(), adondeQuedo)). */
    public Optional<Movimiento> deshacerUltimo() throws IOException { ... }
}
```

## Tests a escribir (como mínimo)
- `Rutas.config(Map.of(), Path.of("/home/f"))` → `/home/f/.config/ordenador/reglas.json`; con `XDG_CONFIG_HOME=/x` → `/x/ordenador/reglas.json`; con `XDG_DATA_HOME=""` → `/home/f/.local/share/ordenador/movimientos.log`.
- `expandir("~/Imágenes", home)`, `expandir("~", home)`, `expandir("/tmp/a", home)`; `expandir("Descargas", home)` lanza `IllegalArgumentException`.
- Con `@TempDir`: anotar dos movimientos y `leer()` los devuelve iguales y en orden.
- Mover un archivo de `a/x.pdf` a `b/x.pdf`, anotarlo, `deshacerUltimo()` → `a/x.pdf` existe, `b/x.pdf` no, y `leer()` queda vacío.
- Si al deshacer ya hay otro `a/x.pdf`, vuelve como `a/x (2).pdf`.
- `deshacerUltimo()` sin registro → `Optional.empty()`.
