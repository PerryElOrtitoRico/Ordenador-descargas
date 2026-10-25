<!-- contrato: 1,2,4 | archivos: src/main/java/com/ordenador/service/Ordenador.java, src/test/java/com/ordenador/service/OrdenadorTest.java -->
# T05c — Ordenador: ordenar toda la carpeta

## Objetivo
Completar **solo** `ordenarCarpeta` de `Ordenador` (lo demás ya está: no lo cambies) y el test `ordenarCarpeta`.

## Código que ya existe (no lo cambies)
```java
public final class Rutas {
    public static Path expandir(String ruta, Path home);   // "~/x" → home/x
}
```
Los records se leen así: `config.carpeta()`.

## `ordenarCarpeta()`, en este orden
1. `Path carpeta = Rutas.expandir(config.carpeta(), home);`
2. `if (!Files.isDirectory(carpeta)) return new ArrayList<>();`
3. Juntar **primero** la lista de archivos (antes de mover nada):
   `try (Stream<Path> s = Files.list(carpeta)) { archivos = s.filter(Files::isRegularFile).sorted().toList(); }`
4. Por cada uno, en ese orden: `resultados.add(procesar(p));`
5. `return resultados;`

## Test (usá `preparar()`, `ordenador(false)` y `descargas`)
- `ordenarCarpeta`: crear `descargas/b.png`, `descargas/a.xyz` y la subcarpeta `descargas/sub` con `sub/c.png` adentro.
  `ordenarCarpeta()` devuelve 2 resultados: primero `a.xyz` (`SIN_REGLA`), después `b.png` (`MOVIDO`).
  `sub/c.png` sigue donde estaba.
