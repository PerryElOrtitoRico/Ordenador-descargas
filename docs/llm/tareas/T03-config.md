# T03 — Reglas por defecto y archivo de reglas

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas (Gson 2.10.1 ya está), no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Leer y escribir `reglas.json` con Gson, y crearlo con las reglas por defecto si no existe.

## Código que ya existe (T02)
```java
package com.ordenador.model;
public enum Accion { MOVER, AVISAR }
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }
    // el constructor compacto cambia las listas null por List.of()
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
```

## Archivos
- Crear `src/main/java/com/ordenador/service/ReglasPorDefecto.java`
- Crear `src/main/java/com/ordenador/service/ConfigRepo.java`
- Crear `src/test/java/com/ordenador/service/ConfigRepoTest.java`

## Qué escribir
```java
package com.ordenador.service;

public final class ReglasPorDefecto {
    private ReglasPorDefecto() { }
    /** Config("~/Descargas", 5, <las 5 reglas de la tabla, en ese orden>). */
    public static Config crear() { ... }
}

public final class ConfigRepo {
    private ConfigRepo() { }
    /** Si el archivo no existe: crea sus carpetas, guarda crear() y la devuelve. Si existe: leer(archivo). */
    public static Config cargarOCrear(Path archivo) throws IOException { ... }
    /** Lee y valida. Lanza IOException (con la ruta del archivo en el mensaje) si no es JSON válido o no pasa la validación. */
    public static Config leer(Path archivo) throws IOException { ... }
    /** Gson con setPrettyPrinting(), UTF-8. Atómico: escribir "<archivo>.tmp" y Files.move con REPLACE_EXISTING y ATOMIC_MOVE. */
    public static void guardar(Config config, Path archivo) throws IOException { ... }
}
```

Reglas por defecto (`diasPapelera` 0 y `mensaje` null salvo que se indique; las listas que no se nombran, vacías):
| nombre | accion | extensiones | patrones | palabras | destino / mensaje | diasPapelera |
|---|---|---|---|---|---|---|
| `instaladores` | MOVER | `deb`, `AppImage`, `exe` | | | destino `~/Descargas/Instaladores` | 30 |
| `rutinas` | MOVER | `json`, `pdf` | `rutina-*`, `plan-*` | | destino `~/Documentos/Carpeta personal/Gimnasio` | |
| `facultad` | AVISAR | `pdf` | | `CALCULO NUMERIC`, `INTRODUCCION A LA INGENIERIA`, `MECANICA DE FLUIDOS`, `TECNOLOGIA MECANICA` | mensaje `Subilo a Drive, carpeta {palabra}/teoria` (destino null) | |
| `finanzas` | MOVER | `pdf`, `csv`, `xls`, `xlsx` | | `mercado pago`, `mercadopago`, `account statement`, `resumen de cuenta`, `estado de cuenta`, `extracto` | destino `~/Documentos/Carpeta personal/Finanzas/entrada` | |
| `imagenes` | MOVER | `png`, `jpg`, `jpeg`, `webp`, `gif` | | | destino `~/Imágenes` | |

Validación en `leer` (cualquier falla → `IOException`):
- archivo vacío o JSON inválido (`JsonParseException` de Gson); `reglas` null;
- una regla con `nombre` null o en blanco; con `accion` null (Gson pone null si el texto no es `MOVER` ni `AVISAR`; el mensaje nombra la regla);
- una regla `MOVER` sin `destino` (null o en blanco); una regla `AVISAR` sin `mensaje`.
- Si `carpeta` es null se usa `"~/Descargas"`; si `segundosQuieto` ≤ 0 se usa 5 (devolver un `Config` nuevo con esos valores).

## Tests a escribir (como mínimo)
Con `@TempDir Path d`:
- `cargarOCrear(d.resolve("sub/reglas.json"))` crea el archivo y devuelve `ReglasPorDefecto.crear()`.
- `guardar` y después `leer` devuelve un `Config` igual (`assertEquals`); no queda el `.tmp`.
- JSON roto (`"{ reglas: ["`) → `IOException`; `{"reglas":[{"nombre":"x","accion":"COPIAR","destino":"~/x"}]}` → `IOException`.
- `{"reglas":[]}` → carpeta `"~/Descargas"`, segundosQuieto 5.
