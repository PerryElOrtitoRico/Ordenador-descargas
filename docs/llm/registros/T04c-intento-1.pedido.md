<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2585, max_tokens 5575, razonamiento medium -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los `// TODO` que la tarea no nombra quedan exactamente como están.
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
        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        String linea = m.fecha() + "\t" + m.origen() + "\t" + m.destino() + "\n";
        Files.writeString(archivo, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Los movimientos en orden (el más viejo primero). Lista vacía si el archivo no existe.
     * Ignora líneas en blanco y líneas que no tienen 3 partes o cuya fecha no se puede leer (LocalDateTime.parse).
     */
    public List<Movimiento> leer() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        List<Movimiento> movimientos = new ArrayList<>();
        for (String linea : lineas) {
            if (linea.isBlank()) {
                continue;
            }
            String[] partes = linea.split("\t");
            if (partes.length != 3) {
                continue;
            }
            try {
                LocalDateTime fecha = LocalDateTime.parse(partes[0]);
                movimientos.add(new Movimiento(fecha, Path.of(partes[1]), Path.of(partes[2])));
            } catch (DateTimeParseException e) {
                // Se saltea si la fecha no se puede leer
            }
        }
        return movimientos;
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
        List<Movimiento> movimientos = leer();
        if (movimientos.isEmpty()) {
            return Optional.empty();
        }
        Movimiento ultimo = movimientos.get(movimientos.size() - 1);
        Path destinoActual = ultimo.destino();
        if (!Files.exists(destinoActual)) {
            throw new IOException("ya no está: " + destinoActual);
        }
        // TODO: Implementación pendiente para deshacerUltimo
        return Optional.empty();
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
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m1 = new Movimiento(LocalDateTime.of(2026, 10, 25, 10, 0), Path.of("/origen1"), Path.of("/destino1"));
        Movimiento m2 = new Movimiento(LocalDateTime.of(2026, 10, 26, 11, 0), Path.of("/origen2"), Path.of("/destino2"));
        r.anotar(m1);
        r.anotar(m2);
        List<Movimiento> leido = r.leer();
        assertEquals(2, leido.size());
        assertEquals(m1, leido.get(0));
        assertEquals(m2, leido.get(1));
    }

    @Test
    void leerSinArchivoEsVacio(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("no.log"));
        List<Movimiento> movimientos = r.leer();
        assertTrue(movimientos.isEmpty());
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

Completá los `// TODO` que pide la tarea T04c. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Registro.java, src/test/java/com/ordenador/service/RegistroTest.java.
