<!-- contrato: 1 | archivos: src/main/java/com/ordenador/service/Pendientes.java, src/test/java/com/ordenador/service/PendientesTest.java -->
# T07a — Pendientes: esperar a que un archivo se quede quieto

## Objetivo
Completar `tocar`, `listos` y `cantidad` de `Pendientes` (usan el mapa `ultimoCambio` que ya está declarado) y sus dos tests.

## Métodos
- `tocar`: `ultimoCambio.put(archivo, ahoraMs);`
- `listos(ahoraMs)`:
  1. `List<Path> listos = new ArrayList<>();`
  2. `for (Map.Entry<Path, Long> e : ultimoCambio.entrySet())` → `if (ahoraMs - e.getValue() >= quietoMs) listos.add(e.getKey());`
  3. `for (Path p : listos) ultimoCambio.remove(p);` (después del recorrido, no adentro)
  4. `listos.sort(null);` y `return listos;`
- `cantidad`: `return ultimoCambio.size();`

## Tests (usan `A` y `B` que ya están en el test)
- `esperaElTiempoQuieto`: `Pendientes p = new Pendientes(1000); p.tocar(A, 0);` → `p.listos(999)` es `List.of()`, `p.listos(1000)` es `List.of(A)` y después `p.cantidad()` es 0.
- `tocarDeNuevoReiniciaLaEspera`: `tocar(A, 0)`, `tocar(A, 800)` → `listos(1500)` vacío y `listos(1800)` = `List.of(A)`; además con `tocar(B, 0)` y `tocar(A, 0)`, `listos(1000)` = `List.of(A, B)` (ordenado).
