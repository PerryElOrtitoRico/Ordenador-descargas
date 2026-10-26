package com.ordenador.service;

import java.nio.file.Path;

/** Qué pasó (o qué pasaría, simulando) con un archivo. destino y mensaje pueden ser null. */
public record Resultado(Path archivo, Estado estado, Path destino, String mensaje) {
    public enum Estado { MOVIDO, AVISADO, SIN_REGLA, IGNORADO }
}
