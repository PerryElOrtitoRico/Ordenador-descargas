<!-- contrato: 1,2,5 | archivos: src/main/java/com/ordenador/service/Registro.java, src/test/java/com/ordenador/service/RegistroTest.java -->
# T04b — Registro: anotar y leer

## Objetivo
Completar **solo** `anotar` y `leer` de `Registro` (los otros `// TODO` quedan como están) y los tests `anotarYLeer` y `leerSinArchivoEsVacio`.

## Reglas
- `anotar`: si `archivo.getParent()` no es null, `Files.createDirectories(archivo.getParent())`. Después
  `Files.writeString(archivo, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)` con
  `linea = m.fecha() + "\t" + m.origen() + "\t" + m.destino() + "\n"`.
- `leer`: si `!Files.exists(archivo)` → `new ArrayList<>()`. Si no, `Files.readAllLines(archivo, StandardCharsets.UTF_8)` y por cada línea:
  - en blanco (`isBlank()`) → se saltea;
  - `String[] partes = linea.split("\t")`; si `partes.length != 3` → se saltea;
  - `LocalDateTime.parse(partes[0])` dentro de `try`/`catch (DateTimeParseException e)` → si falla, se saltea;
  - si no, se agrega `new Movimiento(fecha, Path.of(partes[1]), Path.of(partes[2]))`.
- `Movimiento` es un record: sus datos se leen con `m.fecha()`, `m.origen()`, `m.destino()`.

## Tests
- `anotarYLeer`: con `Registro r = new Registro(d.resolve("datos/movimientos.log"))`, anotar dos movimientos (`LocalDateTime.of(2026, 10, 25, 10, 0)` y otro) y comprobar `assertEquals(List.of(m1, m2), r.leer())`.
- `leerSinArchivoEsVacio`: `new Registro(d.resolve("no.log")).leer()` es `List.of()`.
