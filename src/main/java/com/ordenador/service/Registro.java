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
