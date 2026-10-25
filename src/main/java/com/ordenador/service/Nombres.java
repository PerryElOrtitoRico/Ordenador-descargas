package com.ordenador.service;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        if (carpeta == null || nombre == null) {
            return Paths.get(""); 
        }

        // Verificar existencia usando Files.exists
        Path rutaExistente = carpeta.resolve(nombre);
        
        // Si no existe, devolver directamente
        if (!Files.exists(rutaExistente)) {
            return rutaExistente;
        }

        String nombreNormalizado = normalizar(nombre);
        String ext = extension(nombre);
        
        // Base es el nombre sin la extensión. 
        // Ejemplo: "backup.tar.gz" -> ext="gz", base="backup.tar"
        // Ejemplo: "foto.JPG" -> ext="jpg", base="foto"
        
        String base;
        if (ext.isEmpty()) {
            // Si no hay extensión, la base es el nombre completo.
            base = nombreNormalizado;
        } else {
            // Quitar la extensión del nombre normalizado para obtener la base
            int ultimoPunto = nombre.lastIndexOf('.');
            if (ultimoPunto > 0) {
                base = nombre.substring(0, ultimoPunto);
            } else {
                base = nombre;
            }
        }

        String baseNormalizada = normalizar(base);
        
        // Generar nombres candidatos: "base (2).ext", "base (3).ext"...
        int contador = 2;
        while (true) {
            String candidatoNombre;
            if (ext.isEmpty()) {
                candidatoNombre = baseNormalizada + " (" + contador + ")";
            } else {
                // La extensión se mantiene tal cual en el nombre final.
                String extOriginal = nombre.substring(nombre.lastIndexOf('.') + 1);
                candidatoNombre = baseNormalizada + " (" + contador + ")." + extOriginal;
            }

            Path candidato = carpeta.resolve(candidatoNombre);
            
            if (!Files.exists(candidato)) {
                return candidato;
            }
            
            contador++;
        }
    }
}
