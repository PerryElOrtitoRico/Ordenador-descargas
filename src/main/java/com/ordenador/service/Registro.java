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
