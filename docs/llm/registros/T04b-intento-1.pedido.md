<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 1904, max_tokens 6256 -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo esos cuerpos.
Reglas:
- No cambies package, imports que ya están, nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los records se leen con sus métodos de acceso: m.origen(), regla.extensiones(). No existen getOrigen() ni getExtensiones().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/ordenador/service/Ejemplo.java
package com.ordenador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# ORDENADOR — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven. Paquete base `com.ordenador`: `model` (datos), `service` (lógica), `cli` (comandos).
- Única dependencia: Gson 2.10.1. Tests con JUnit 5 (`org.junit.jupiter.api.*`). No toques `pom.xml`.
- Nombres y comentarios en español.
- Nada de rutas relativas ni `user.dir`: cada clase recibe las rutas por parámetro (así se testea con `@TempDir`).
- Nunca `Files.delete` sobre archivos del usuario.

## 2. Modelo (`com.ordenador.model`)
```java
public enum Accion { MOVER, AVISAR }

public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }

public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }

public record Movimiento(java.time.LocalDateTime fecha, java.nio.file.Path origen, java.nio.file.Path destino) { }
```
En `Regla`, una lista `null` se toma como lista vacía.

## 5. Archivos del programa
- Reglas: `$XDG_CONFIG_HOME/ordenador/reglas.json` (si no está definida: `~/.config/ordenador/reglas.json`). Si no existe, se crea con las reglas por defecto.
- Registro de movimientos: `$XDG_DATA_HOME/ordenador/movimientos.log` (si no: `~/.local/share/ordenador/movimientos.log`). Una línea por movimiento: `fecha ISO<TAB>origen<TAB>destino`.

---

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

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Registro.java
package com.ordenador.service;

import com.ordenador.model.Movimiento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Registro de movimientos (movimientos.log): una línea "fecha ISO\torigen\tdestino" por movimiento. */
public final class Registro {

    private final Path archivo;

    public Registro(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Crea las carpetas si hace falta y agrega al final la línea
     * fecha.toString() + "\t" + origen + "\t" + destino + "\n" (UTF-8, StandardOpenOption.CREATE y APPEND).
     */
    public void anotar(Movimiento m) throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Los movimientos en orden (el más viejo primero). Lista vacía si el archivo no existe.
     * Ignora líneas en blanco y líneas que no tienen 3 partes o cuya fecha no se puede leer (LocalDateTime.parse).
     */
    public List<Movimiento> leer() throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }

    /**
     * Deshace el último movimiento leído:
     * - si no hay ninguno → Optional.empty();
     * - si m.destino() ya no existe → IOException ("ya no está: <destino>") y el registro no cambia;
     * - si no: crea la carpeta de m.origen() si falta y mueve m.destino() a m.origen()
     *   (o a Nombres.nombreLibre(carpeta de origen, nombre de origen) si ahí ya hay otro archivo);
     *   reescribe el registro sin esa última línea (atómico: "<archivo>.tmp" + Files.move REPLACE_EXISTING);
     *   devuelve Optional.of(new Movimiento(ahora, m.destino(), adondeQuedo)).
     */
    public Optional<Movimiento> deshacerUltimo() throws IOException {
        // TODO
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/service/RegistroTest.java
package com.ordenador.service;

import com.ordenador.model.Movimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistroTest {

    @Test
    void anotarYLeer(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void leerSinArchivoEsVacio(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void deshacerVuelveAlOrigen(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void deshacerNoPisa(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void deshacerSinRegistro(@TempDir Path d) throws Exception {
        // TODO
    }
}
```

Completá los `// TODO` que pide la tarea T04b. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Registro.java, src/test/java/com/ordenador/service/RegistroTest.java.
