package com.ordenador.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Archivos que cambiaron hace poco y todavía no se avisaron. Lógica pura: el tiempo se pasa en ms. No es thread-safe. */
public final class Pendientes {

    private final long quietoMs;
    private final Map<Path, Long> ultimoCambio = new HashMap<>();

    public Pendientes(long quietoMs) {
        this.quietoMs = quietoMs;
    }

    /** Anota que el archivo cambió en ahoraMs (si ya estaba, actualiza la hora). */
    public void tocar(Path archivo, long ahoraMs) {
        // TODO T07a
        throw new UnsupportedOperationException();
    }

    /** Saca y devuelve los archivos cuyo último cambio fue hace quietoMs o más (ahoraMs - ultimo >= quietoMs), ordenados por ruta. */
    public List<Path> listos(long ahoraMs) {
        // TODO T07a
        throw new UnsupportedOperationException();
    }

    public int cantidad() {
        // TODO T07a
        throw new UnsupportedOperationException();
    }
}
