# T02 — Modelo y clasificador de reglas

## Reglas del proyecto
Las mismas de T01: Java 21, Maven, JUnit 5, nombres y comentarios en español, sin dependencias nuevas, no modificar archivos que la tarea no nombre, `mvn -q test` en verde al terminar.

## Objetivo
Crear el modelo de la sección 2 de la especificación (menos `Movimiento`) y decidir qué regla le toca a un nombre de archivo (sección 3).

## Código que ya existe (T01)
```java
package com.ordenador.service;
public final class Nombres {
    public static String normalizar(String texto);   // "Cálculo_Numérico.PDF" → "calculo numerico pdf"
    public static String extension(String nombre);    // "foto.JPG" → "jpg"; sin extensión → ""
}
```

## Archivos
- Crear `src/main/java/com/ordenador/model/Accion.java`
- Crear `src/main/java/com/ordenador/model/Regla.java`
- Crear `src/main/java/com/ordenador/model/Config.java`
- Crear `src/main/java/com/ordenador/service/Coincidencia.java`
- Crear `src/main/java/com/ordenador/service/Clasificador.java`
- Crear `src/test/java/com/ordenador/service/ClasificadorTest.java`

## Qué escribir
`Accion` y `Config`: exactamente como en la especificación. `Regla`: el mismo record, con constructor compacto que cambia cada lista `null` por `List.of()` y si no, la copia con `List.copyOf(...)`.

```java
package com.ordenador.service;

/** La regla elegida y la palabra de regla.palabras() que coincidió (null si la regla no tiene palabras). */
public record Coincidencia(Regla regla, String palabra) { }

public final class Clasificador {
    private Clasificador() { }

    /** Glob sin distinguir mayúsculas: '*' = cero o más caracteres, '?' = uno; todo lo demás es literal
     *  (usá Pattern.quote para los tramos literales). Tiene que coincidir el nombre entero. */
    public static boolean coincideGlob(String patron, String nombre) { ... }

    /** La primera regla de la lista que coincide con el nombre (sección 3), o Optional.empty(). */
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas) { ... }
}
```

- La palabra que se devuelve es la **primera de la lista** cuya forma normalizada está contenida en `Nombres.normalizar(nombre)`, tal como está escrita en la lista (por ejemplo `"MECANICA DE FLUIDOS"`).
- `extensiones` se compara con `Nombres.extension(nombre)` sin distinguir mayúsculas (`"AppImage"` coincide con `x.appimage`).

Ejemplos:
| patrón | nombre | coincide |
|---|---|---|
| `rutina-*` | `Rutina-Octubre.pdf` | sí |
| `plan-*` | `mi-plan-2.json` | no (tiene que empezar con `plan-`) |
| `a?c.txt` | `abc.txt` | sí |
| `a.c` | `abc` | no (el punto es literal) |
| `informe (1)*` | `informe (1) final.pdf` | sí |

## Tests a escribir (como mínimo)
- Los ejemplos de glob.
- Una regla con `extensiones = ["deb", "AppImage"]` coincide con `x.DEB` y `y.appimage`, no con `z.zip`.
- Regla con `extensiones = ["pdf"]` y `palabras = ["CALCULO NUMERIC", "MECANICA DE FLUIDOS"]`: `"Mecánica_de_Fluidos-TP1.pdf"` → coincide con palabra `"MECANICA DE FLUIDOS"`; `"Mecánica_de_Fluidos.docx"` → no.
- Con dos reglas que coinciden, gana la primera.
- `new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0).extensiones()` es una lista vacía.
