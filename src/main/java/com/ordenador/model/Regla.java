package com.ordenador.model;

import java.util.List;

/**
 * Una regla del archivo reglas.json. Las listas vacías no ponen condición;
 * una lista null se toma como vacía.
 */
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) {

    public Regla {
        extensiones = extensiones == null ? List.of() : List.copyOf(extensiones);
        patrones = patrones == null ? List.of() : List.copyOf(patrones);
        palabras = palabras == null ? List.of() : List.copyOf(palabras);
    }
}
