package com.ordenador.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Lo que quedó sin ordenar en Descargas (para el aviso semanal). */
public final class Resumen {

    private Resumen() { }

    /** Nombres de los archivos regulares de primer nivel que no son temporales, en orden alfabético. Vacía si la carpeta no existe. */
    public static List<String> sinOrdenar(Path carpeta) throws IOException {
        if (!Files.isDirectory(carpeta)) return new ArrayList<>();
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> !Nombres.esTemporal(n))
                    .sorted()
                    .toList();
        }
    }

    /**
     * Vacía → "Descargas está ordenada.".
     * Uno → "Queda 1 archivo sin ordenar en Descargas:\n- a.pdf"
     * Varios → "Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt" (sin "\n" al final).
     */
    public static String texto(List<String> nombres) {
        if (nombres.isEmpty()) {
            return "Descargas está ordenada.";
        }
        StringBuilder sb = new StringBuilder(nombres.size() == 1 ? "Queda 1 archivo sin ordenar en Descargas:" : "Quedan " + nombres.size() + " archivos sin ordenar en Descargas:");
        for (String nombre : nombres) {
            sb.append("\n- ").append(nombre);
        }
        return sb.toString();
    }
}
