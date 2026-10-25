package com.ordenador.service;

import com.ordenador.model.Regla;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Decide qué regla le toca a un nombre de archivo (sección 3 de la especificación). */
public final class Clasificador {

    private Clasificador() { }

    /**
     * Glob sin distinguir mayúsculas: '*' = cero o más caracteres, '?' = uno; todo lo demás es literal.
     * Tiene que coincidir el nombre entero.
     */
    public static boolean coincideGlob(String patron, String nombre) {
        StringBuilder regex = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        for (char c : patron.toCharArray()) {
            if (c == '*' || c == '?') {
                if (!literal.isEmpty()) {
                    regex.append(Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                regex.append(c == '*' ? ".*" : ".");
            } else {
                literal.append(c);
            }
        }
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
        }
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL;
        return Pattern.compile(regex.toString(), flags).matcher(nombre).matches();
    }

    /** La primera regla de la lista que coincide con el nombre, o Optional.empty(). */
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas) {
        String extension = Nombres.extension(nombre);
        String normalizado = Nombres.normalizar(nombre);
        for (Regla regla : reglas) {
            if (!regla.extensiones().isEmpty()
                    && regla.extensiones().stream().noneMatch(e -> e.equalsIgnoreCase(extension))) {
                continue;
            }
            if (!regla.patrones().isEmpty()
                    && regla.patrones().stream().noneMatch(p -> coincideGlob(p, nombre))) {
                continue;
            }
            String palabra = null;
            if (!regla.palabras().isEmpty()) {
                palabra = regla.palabras().stream()
                        .filter(p -> normalizado.contains(Nombres.normalizar(p)))
                        .findFirst().orElse(null);
                if (palabra == null) {
                    continue;
                }
            }
            return Optional.of(new Coincidencia(regla, palabra));
        }
        return Optional.empty();
    }
}
