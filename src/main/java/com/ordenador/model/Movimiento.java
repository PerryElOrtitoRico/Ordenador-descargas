package com.ordenador.model;

import java.time.LocalDateTime;
import java.nio.file.Path;

/**
 * Representa un movimiento realizado por el programa.
 */
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }
