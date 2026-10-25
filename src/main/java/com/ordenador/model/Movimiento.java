package com.ordenador.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/** Un archivo que el ordenador movió de origen a destino. */
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }
