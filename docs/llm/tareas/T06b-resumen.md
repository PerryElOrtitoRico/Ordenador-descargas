<!-- contrato: 1,3 | archivos: src/main/java/com/ordenador/service/Resumen.java, src/test/java/com/ordenador/service/ResumenTest.java -->
# T06b — Resumen de lo que quedó sin ordenar

## Objetivo
Completar `sinOrdenar` y `texto` de `Resumen` y sus dos tests.

## Código que ya existe (no lo cambies)
```java
public final class Nombres {
    public static boolean esTemporal(String nombre);  // .part, .crdownload, .tmp o empieza con "."
}
```

## `sinOrdenar(carpeta)`
1. `if (!Files.isDirectory(carpeta)) return new ArrayList<>();`
2. `try (Stream<Path> s = Files.list(carpeta)) { return s.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).filter(n -> !Nombres.esTemporal(n)).sorted().toList(); }`

## `texto(nombres)`
1. `if (nombres.isEmpty()) return "Descargas está ordenada.";`
2. `StringBuilder sb = new StringBuilder(nombres.size() == 1 ? "Queda 1 archivo sin ordenar en Descargas:" : "Quedan " + nombres.size() + " archivos sin ordenar en Descargas:");`
3. Por cada nombre: `sb.append("\n- ").append(nombre);`
4. `return sb.toString();`

## Tests
- `sinOrdenarSaltaTemporalesYCarpetas`: en `d` crear `b.zip`, `a.pdf`, `x.part`, `.oculto` y la subcarpeta `sub` → `List.of("a.pdf", "b.zip")`.
- `textoVacioUnoYVarios`: los tres casos, con los textos exactos de arriba (por ejemplo `"Quedan 2 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip"`).
