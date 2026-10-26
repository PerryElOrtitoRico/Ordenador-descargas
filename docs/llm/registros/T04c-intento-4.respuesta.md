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
        List<Movimiento> todos = leer();
        if (todos.isEmpty()) {
            return Optional.empty();
        }
        Movimiento m = todos.get(todos.size() - 1);
        
        // Paso 3: Verificar si el destino ya no existe
        if (!Files.exists(m.destino())) {
            throw new IOException("ya no está: " + m.destino());
        }

        Path carpeta = m.origen().getParent();
        Files.createDirectories(carpeta);
        
        // Paso 5: Determinar adonde mover (con nombre libre si el origen ya existe)
        Path adonde = Files.exists(m.origen()) ? Nombres.nombreLibre(carpeta, m.origen().getFileName().toString()) : m.origen();
        
        // Paso 6: Mover el archivo
        Files.move(m.destino(), adonde);
        
        // Paso 7: Reescribir el registro sin el último movimiento
        List<Movimiento> todosMenosUltimo = new ArrayList<>(todos.subList(0, todos.size() - 1));
        Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        StringBuilder contenido = new StringBuilder();
        for (Movimiento mov : todosMenosUltimo) {
            contenido.append(mov.fecha()).append("\t").append(mov.origen()).append("\t").append(mov.destino()).append("\n");
        }
        Files.writeString(tmp, contenido.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
        
        // Paso 8: Retornar el movimiento deshaciendo
        return Optional.of(new Movimiento(LocalDateTime.now(), m.destino(), adonde));
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
        Path origen = d.resolve("a");
        Path destino = d.resolve("b");
        Files.createDirectories(origen);
        Files.createDirectories(destino);
        Path archivoX = destino.resolve("x.pdf");
        Files.writeString(archivoX, "contenido");
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m = new Movimiento(LocalDateTime.now(), origen.resolve("x.pdf"), destino.resolve("x.pdf"));
        r.anotar(m);
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isPresent());
        Path adonde = deshacer.get().destino();
        assertTrue(Files.exists(adonde));
        assertFalse(Files.exists(destino.resolve("x.pdf")));
        List<Movimiento> leido = r.leer();
        assertTrue(leido.isEmpty());
    }

    @Test
    void deshacerNoPisa(@TempDir Path d) throws Exception {
        Path carpeta = d.resolve("a");
        Files.createDirectories(carpeta);
        Path archivoX1 = carpeta.resolve("x.pdf");
        Files.writeString(archivoX1, "contenido1");
        Path destino = d.resolve("b");
        Files.createDirectories(destino);
        Path archivoX2 = destino.resolve("x.pdf");
        Files.writeString(archivoX2, "contenido2");
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m = new Movimiento(LocalDateTime.now(), carpeta.resolve("x.pdf"), destino.resolve("x.pdf"));
        r.anotar(m);
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isPresent());
        Path adonde = deshacer.get().destino();
        assertFalse(Files.exists(carpeta.resolve("x.pdf")));
        assertTrue(Files.exists(adonde));
        List<Movimiento> leido = r.leer();
        assertTrue(leido.isEmpty());
    }

    @Test
    void deshacerSinRegistro(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isEmpty());
    }
}
```

<!-- meta: {"segundos": 241.5, "finish_reason": "stop", "usage": {"prompt_tokens": 3377, "completion_tokens": 2061, "total_tokens": 5438, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
