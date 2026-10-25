```java
// src/main/java/com/ordenador/service/Registro.java
package com.ordenador.service;

import com.ordenador.model.Movimiento;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona el registro de movimientos en un archivo de texto.
 */
public final class Registro {
    private final Path archivo;

    public Registro(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Crea las carpetas si hace falta y agrega una línea al final del archivo.
     * Formato: fecha ISO<TAB>origen<TAB>destino\n
     */
    public void anotar(Movimiento m) throws IOException {
        // Crear directorios padres si no existen
        Files.createDirectories(archivo.getParent());

        String linea = m.fecha().toString() + "\t" + m.origen() + "\t" + m.destino() + "\n";
        try (var writer = java.nio.file.Files.newBufferedWriter(archivo, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(linea);
        }
    }

    /**
     * Lee los movimientos guardados en orden cronológico (más viejo primero).
     * Ignora líneas vacías y malformadas.
     */
    public List<Movimiento> leer() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<Movimiento> movimientos = new ArrayList<>();
        try (var reader = java.nio.file.Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split("\t", -1); // -1 para mantener barras si las hubiera, aunque el formato usa tab
                if (partes.length != 3) {
                    continue;
                }

                try {
                    LocalDateTime fecha = LocalDateTime.parse(partes[0]);
                    Path origen = Paths.get(partes[1]);
                    Path destino = Paths.get(partes[2]);
                    movimientos.add(new Movimiento(fecha, origen, destino));
                } catch (Exception e) {
                    // Ignorar líneas con fecha inválida
                    continue;
                }
            }
        }
        return movimientos;
    }

    /**
     * Deshace el último movimiento registrado.
     * - Si no hay movimientos, devuelve Optional.empty().
     * - Si el destino ya no existe, lanza IOException y no cambia el registro.
     * - Si el destino existe:
     *   1. Mueve el archivo de destino a la carpeta origen (resolviendo conflictos de nombre).
     *   2. Reescribe el registro eliminando la última línea (atómicamente usando .tmp).
     *   3. Devuelve el nuevo movimiento con fecha actual y la nueva ruta final.
     */
    public Optional<Movimiento> deshacerUltimo() throws IOException {
        List<Movimiento> movimientos = leer();
        if (movimientos.isEmpty()) {
            return Optional.empty();
        }

        Movimiento ultimo = movimientos.get(movimientos.size() - 1);
        
        // Verificar si el destino ya no existe
        if (!Files.exists(ultimo.destino())) {
            throw new IOException("ya no está: " + ultimo.destino());
        }

        // Calcular la nueva ruta de destino (donde quedará tras deshacer)
        Path nuevaRuta = Rutas.expandir(ultimo.origen().toString(), ultimo.origen().getParent());
        
        // Resolver conflicto de nombre si es necesario
        String nombreFinal;
        if (!Files.exists(nuevaRuta)) {
            nombreFinal = ultimo.destino().getFileName().toString();
        } else {
            nombreFinal = Nombres.nombreLibre(ultimo.origen().getParent(), ultimo.destino().getFileName().toString());
        }

        // Construir el path final completo
        Path destinoFinal = ultimo.origen().getParent().resolve(nombreFinal);

        // Mover el archivo
        Files.move(ultimo.destino(), destinoFinal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Reescribir el registro sin la última línea (atómicamente)
        List<String> lineas = new ArrayList<>();
        try (var reader = java.nio.file.Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.equals(ultimo.fecha().toString() + "\t" + ultimo.origen() + "\t" + ultimo.destino())) {
                    lineas.add(linea);
                }
            }
        }

        // Escribir el archivo temporalmente y luego moverlo para atomicidad
        Path archivoTmp = archivo.resolve(archivo.getFileName().toString() + ".tmp");
        try (var writer = java.nio.file.Files.newBufferedWriter(archivoTmp, StandardCharsets.UTF_8)) {
            for (String l : lineas) {
                writer.write(l);
                writer.newLine();
            }
        }

        Files.move(archivoTmp, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Crear el nuevo movimiento con la fecha actual y la nueva ruta
        Movimiento nuevoMovimiento = new Movimiento(LocalDateTime.now(), ultimo.origen(), destinoFinal);
        return Optional.of(nuevoMovimiento);
    }
}
```

<!-- meta: {"segundos": 213.7, "finish_reason": "stop", "usage": {"prompt_tokens": 1492, "completion_tokens": 1295, "total_tokens": 2787, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
