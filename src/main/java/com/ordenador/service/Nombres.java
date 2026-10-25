package com.ordenador.service;

import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Nombres {
    private static final Pattern ESPACIOS_REPETIDOS = Pattern.compile("\\s+");

    private Nombres() { }

    /** Sin acentos (java.text.Normalizer NFD + quitar \\p{M}), minúsculas,
     *  '_' '-' '.' → espacio, espacios repetidos → uno, sin espacios al principio ni al final. */
    public static String normalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        // Normalización NFD para separar caracteres de sus acentos
        String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        
        // Eliminar caracteres diacríticos (acentos)
        String sinAcentos = descompuesto.replaceAll("\\p{M}", "");
        
        // Pasar a minúsculas con Locale.ROOT para evitar dependencias del sistema
        String minusculas = sinAcentos.toLowerCase(Locale.ROOT);
        
        // Reemplazar '_', '-', '.' por espacio
        String conEspacios = minusculas.replace('_', ' ')
                                       .replace('-', ' ')
                                       .replace('.', ' ');
        
        // Unir espacios repetidos en uno solo
        String unicosEspacios = ESPACIOS_REPETIDOS.matcher(conEspacios).replaceAll(" ");
        
        // Eliminar espacios al principio y al final
        return unicosEspacios.trim();
    }

    /** Lo que sigue al último punto, en minúsculas. "" si no hay punto,
     *  si el único punto es el primer carácter (".bashrc") o si termina en punto. */
    public static String extension(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return "";
        }

        // Encontrar la última ocurrencia de un punto
        int ultimoPunto = nombre.lastIndexOf('.');
        
        // Si no hay puntos, o el único punto es el primero, o termina en punto
        if (ultimoPunto <= 0 || ultimoPunto == nombre.length() - 1) {
            return "";
        }

        // Extraer la extensión y pasar a minúsculas con Locale.ROOT
        String ext = nombre.substring(ultimoPunto + 1).toLowerCase(Locale.ROOT);
        
        return ext;
    }

    /** true si termina en .part, .crdownload o .tmp (sin distinguir mayúsculas) o empieza con '.'. */
    public static boolean esTemporal(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return false;
        }

        String nombreMinusculas = nombre.toLowerCase(Locale.ROOT);
        
        // Comienza con punto
        if (nombreMinusculas.startsWith(".")) {
            return true;
        }
        
        // Termina en .part, .crdownload o .tmp
        if (nombreMinusculas.endsWith(".part") || 
            nombreMinusculas.endsWith(".crdownload") || 
            nombreMinusculas.endsWith(".tmp")) {
            return true;
        }

        return false;
    }

    /** carpeta.resolve(nombre) si no existe ese archivo; si existe, la primera libre de
     *  "base (2).ext", "base (3).ext", … (base = nombre sin la extensión de extension()). */
    public static Path nombreLibre(Path carpeta, String nombre) {
        Path directo = carpeta.resolve(nombre);
        if (!Files.exists(directo, LinkOption.NOFOLLOW_LINKS)) {
            return directo;
        }
        // El nombre se conserva tal cual (mayúsculas, guiones, puntos): solo se agrega " (n)" antes de la extensión.
        String base = nombre;
        String sufijo = "";
        if (!extension(nombre).isEmpty()) {
            int punto = nombre.lastIndexOf('.');
            base = nombre.substring(0, punto);
            sufijo = nombre.substring(punto);
        }
        for (int n = 2; ; n++) {
            Path candidato = carpeta.resolve(base + " (" + n + ")" + sufijo);
            if (!Files.exists(candidato, LinkOption.NOFOLLOW_LINKS)) {
                return candidato;
            }
        }
    }
}
