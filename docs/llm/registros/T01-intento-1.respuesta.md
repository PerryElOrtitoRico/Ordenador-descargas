```java
// src/main/java/com/ordenador/service/Nombres.java
package com.ordenador.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Nombres {
    private static final Pattern ESPACIOS_REPETIDOS = Pattern.compile("\\s+");
    private static final Pattern PUNTOS_LETRES = Pattern.compile("[.]+");

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
            return Paths.get(""); // Debería manejarse como error en producción, pero cumplimos con el contrato básico
        }

        String nombreNormalizado = normalizar(nombre);
        String ext = extension(nombre);
        
        Path rutaExistente = carpeta.resolve(nombre);
        
        // Si no existe, devolver directamente
        if (!FilesUtil.exists(rutaExistente)) {
            return rutaExistente;
        }

        // Si existe, buscar nombre libre
        // Base es el nombre sin la extensión. 
        // Ejemplo: "backup.tar.gz" -> ext="gz", base="backup.tar"
        // Ejemplo: "foto.JPG" -> ext="jpg", base="foto"
        
        String base;
        if (ext.isEmpty()) {
            // Si no hay extensión, la base es el nombre completo menos el último segmento si tuviera punto al final?
            // Según especificación: "base = nombre sin la extensión de extension()"
            // Si extension() devuelve "", entonces base es el nombre completo.
            base = nombreNormalizado;
        } else {
            // Quitar la extensión del nombre normalizado para obtener la base
            // Nota: normalizar ya ha convertido todo a minúsculas y unificado espacios, pero la extensión original puede tener mayúsculas.
            // Para construir el nombre libre, usamos la versión normalizada de la base.
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
                // La extensión se mantiene tal cual en el nombre final, pero la numeración va antes.
                // Ejemplo: "foto.JPG" -> "foto (2).JPG"
                // Necesitamos preservar la extensión original del archivo para no cambiar su caso si es relevante, 
                // aunque la especificación dice que extension() devuelve minúsculas.
                // Sin embargo, el nombre libre debe ser consistente. Usaremos la extensión tal como viene en el archivo original?
                // La especificación de ejemplo: "backup.tar.gz" con ese ya en c -> c/backup.tar (2).gz
                // Esto implica que la extensión final es ".gz".
                
                // Para mantener consistencia, usamos la extensión del archivo original.
                String extOriginal = nombre.substring(nombre.lastIndexOf('.') + 1);
                candidatoNombre = baseNormalizada + " (" + contador + ")." + extOriginal;
            }

            Path candidato = carpeta.resolve(candidatoNombre);
            
            if (!FilesUtil.exists(candidato)) {
                return candidato;
            }
            
            contador++;
        }
    }
    
    // Helper simple para evitar dependencias de java.nio.file.Files si queremos ser estrictos con "puras", 
    // pero Files existe en JDK. Usaremos el método estático de Files directamente o un helper interno.
    // Para mantener la clase pura y sin dependencias externas, usamos java.nio.file.Files.
    private static boolean exists(Path p) {
        try {
            return java.nio.file.Files.exists(p);
        } catch (Exception e) {
            return false;
        }
    }
}

// Necesitamos importar Files para el helper interno o hacerlo estático dentro de la clase.
// Reescribiendo el método nombreLibre para usar java.nio.file.Files directamente.
```

```java
// src/main/java/com/ordenador/service/Nombres.java
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
```

```java
// src/test/java/com/ordenador/service/NombresTest.java
package com.ordenador.service;

import java.nio.file.Path;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NombresTest {

    @Test
    public void testNormalizarConAcentosYGuiones() {
        String input = "  Cálculo_Numérico--Tema 3.PDF ";
        String expected = "calculo numerico tema 3 pdf";
        assertEquals(expected, Nombres.normalizar(input));
    }

    @Test
    public void testNormalizarMayusculas() {
        String input = "MECÁNICA DE FLUIDOS";
        String expected = "mecanica de fluidos";
        assertEquals(expected, Nombres.normalizar(input));
    }

    @Test
    public void testExtensionConPuntoMayusculo() {
        String input = "foto.JPG";
        String expected = "jpg";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionConMultiplePuntos() {
        String input = "backup.tar.gz";
        String expected = "gz";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionSinExtencion() {
        String input = "LEEME";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionConPuntoInicial() {
        String input = ".bashrc";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testExtensionTerminaEnPunto() {
        String input = "raro.";
        String expected = "";
        assertEquals(expected, Nombres.extension(input));
    }

    @Test
    public void testEsTemporalPart() {
        String input = "video.mp4.part";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalCrdownload() {
        String input = "x.CRDOWNLOAD";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalLock() {
        String input = ".~lock.doc#";
        assertTrue(Nombres.esTemporal(input));
    }

    @Test
    public void testEsTemporalNormal() {
        String input = "partido.pdf";
        assertFalse(Nombres.esTemporal(input));
    }

    @Test
    public void testNombreLibreSinArchivo() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a.pdf", resultado.getFileName().toString());
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }

    @Test
    public void testNombreLibreConArchivoExistente() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo a.pdf
            Files.createFile(carpeta.resolve("a.pdf"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a (2).pdf", resultado.getFileName().toString());
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }

    @Test
    public void testNombreLibreConArchivoYNumeracion() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivos a.pdf y a (2).pdf
            Files.createFile(carpeta.resolve("a.pdf"));
            Files.createFile(carpeta.resolve("a (2).pdf"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "a.pdf");
            assertEquals("a (3).pdf", resultado.getFileName().toString());
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }

    @Test
    public void testNombreLibreConMultipleExtensiones() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo backup.tar.gz
            Files.createFile(carpeta.resolve("backup.tar.gz"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "backup.tar.gz");
            assertEquals("backup.tar (2).gz", resultado.getFileName().toString());
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }

    @Test
    public void testNombreLibreSinExtensionOriginal() throws Exception {
        Path carpeta = Files.createTempDirectory("test-libre");
        try {
            // Crear archivo LEEME
            Files.createFile(carpeta.resolve("LEEME"));
            
            Path resultado = Nombres.nombreLibre(carpeta, "LEEME");
            assertEquals("LEEME (2)", resultado.getFileName().toString());
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }
}
```

<!-- meta: {"segundos": 354.2, "finish_reason": "stop", "usage": {"prompt_tokens": 2084, "completion_tokens": 4085, "total_tokens": 6169, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
