// src/main/java/com/ordenador/model/Regla.java
package com.ordenador.model;

import java.util.List;

/** Representa una regla para clasificar archivos. */
public final class Regla {

    private final List<String> extensiones;
    private final List<String> patrones;
    private final List<String> palabras;

    public Regla(List<String> extensiones, List<String> patrones, List<String> palabras) {
        this.extensiones = extensiones;
        this.patrones = patrones;
        this.palabras = palabras;
    }

    /** Obtiene la lista de extensiones permitidas. */
    public List<String> getExtensiones() {
        return extensiones;
    }

    /** Obtiene la lista de patrones glob para el nombre del archivo. */
    public List<String> getPatrones() {
        return patrones;
    }

    /** Obtiene la lista de palabras que deben estar contenidas en el nombre del archivo. */
    public List<String> getPalabras() {
        return palabras;
    }
}

<!-- meta: {"segundos": 48.6, "finish_reason": "stop", "usage": {"prompt_tokens": 1439, "completion_tokens": 243, "total_tokens": 1682, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
