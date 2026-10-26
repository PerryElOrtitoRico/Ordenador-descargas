```java
// src/main/java/com/ordenador/model/Regla.java
package com.ordenador.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Define una regla para clasificar y mover archivos.
 * Una lista null se interpreta como lista vacía.
 */
public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) {

    /**
     * Constructor compacto que asegura que las listas nunca sean null.
     * Si se pasa null, se crea una lista vacía. Si se pasa una lista, se copia para evitar mutaciones externas.
     */
    public Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                 List<String> palabras, String destino, String mensaje, int diasPapelera) {
        this.nombre = nombre;
        this.accion = accion;
        // Si es null, se convierte en lista vacía; si no, se copia para inmutabilidad interna
        this.extensiones = (extensiones == null) ? List.of() : List.copyOf(extensiones);
        this.patrones = (patrones == null) ? List.of() : List.copyOf(patrones);
        // Si es null, se convierte en lista vacía; si no, se copia para inmutabilidad interna
        this.palabras = (palabras == null) ? List.of() : List.copyOf(palabras);
        this.destino = destino;
        this.mensaje = mensaje;
        this.diasPapelera = diasPapelera;
    }

    // Getters públicos para permitir el acceso desde Clasificador.java
    public List<String> getExtensiones() {
        return extensiones;
    }

    public List<String> getPatrones() {
        return patrones;
    }

    public List<String> getPalabras() {
        return palabras;
    }
}
```

<!-- meta: {"segundos": 92.6, "finish_reason": "stop", "usage": {"prompt_tokens": 1816, "completion_tokens": 447, "total_tokens": 2263, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
