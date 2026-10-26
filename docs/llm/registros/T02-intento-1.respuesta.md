```java
// src/main/java/com/ordenador/model/Accion.java
package com.ordenador.model;

/** Representa las acciones posibles que puede realizar el sistema con un archivo. */
public enum Accion {
    MOVER,
    AVISAR
}
```

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
}
```

```java
// src/main/java/com/ordenador/model/Config.java
package com.ordenador.model;

import java.util.List;

/** Configuración global del sistema de ordenamiento. */
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
```

```java
// src/main/java/com/ordenador/service/Coincidencia.java
package com.ordenador.service;

import com.ordenador.model.Regla;

/** La regla elegida y la palabra de regla.palabras() que coincidió (null si la regla no tiene palabras). */
public record Coincidencia(Regla regla, String palabra) { }
```

```java
// src/main/java/com/ordenador/service/Clasificador.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Regla;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Lógica para clasificar archivos según las reglas definidas. */
public final class Clasificador {

    private Clasificador() { }

    /**
     * Compara un patrón glob con un nombre de archivo sin distinguir mayúsculas.
     * '*' = cero o más caracteres, '?' = uno; todo lo demás es literal (usando Pattern.quote).
     * Tiene que coincidir el nombre entero.
     */
    public static boolean coincideGlob(String patron, String nombre) {
        if (patron == null || nombre == null) {
            return false;
        }

        // Normalizamos ambos a minúsculas para comparar sin distinguir mayúsculas
        String patronNormalizado = patron.toLowerCase();
        String nombreNormalizado = nombre.toLowerCase();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < patronNormalizado.length(); i++) {
            char c = patronNormalizado.charAt(i);
            if (c == '*' || c == '?') {
                // Los caracteres glob no necesitan escapar, se usan como wildcard
                sb.append(c);
            } else {
                // Todo lo demás es literal, debe escaparse si tiene significado especial en regex
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }

        String patronRegex = sb.toString();
        Pattern pattern = Pattern.compile("^" + patronRegex + "$");

        return pattern.matcher(nombreNormalizado).matches();
    }

    /**
     * Determina la primera regla de la lista que coincide con el nombre del archivo.
     * Una regla coincide si cumple todas sus listas no vacías (extensiones, patrones, palabras).
     * Dentro de cada lista basta con que coincida un elemento.
     * Devuelve Optional.empty() si ninguna regla coincide.
     */
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas) {
        if (nombre == null || reglas == null || reglas.isEmpty()) {
            return Optional.empty();
        }

        // Normalizamos el nombre una vez para reutilizarlo en las comprobaciones
        String nombreNormalizado = Nombres.normalizar(nombre);
        String extensionNombre = Nombres.extension(nombre);

        for (Regla regla : reglas) {
            if (regla == null) {
                continue;
            }

            boolean coincideExtensiones = verificarLista(regla.extensiones, extensionNombre);
            boolean coincidePatrones = verificarLista(regla.patrones, nombreNormalizado);
            boolean coincidePalabras = verificarLista(regla.palabras, nombreNormalizado);

            if (coincideExtensiones && coincidePatrones && coincidePalabras) {
                // Encontramos la primera regla que coincide
                String palabraCoincidente = null;
                if (!regla.palabras.isEmpty()) {
                    // Buscamos la primera palabra de la lista cuya forma normalizada está contenida en nombreNormalizado
                    for (String p : regla.palabras) {
                        String pNormalizada = Nombres.normalizar(p);
                        if (nombreNormalizado.contains(pNormalizada)) {
                            palabraCoincidente = p; // Devolvemos la palabra tal como está escrita en la lista
                            break;
                        }
                    }
                }

                return Optional.of(new Coincidencia(regla, palabraCoincidente));
            }
        }

        return Optional.empty();
    }

    /**
     * Verifica si alguna de las cadenas en la lista coincide con el valor objetivo.
     * Para extensiones: comparación directa sin distinguir mayúsculas.
     * Para patrones y palabras: se usa coincidencia glob o contains según corresponda.
     */
    private static boolean verificarLista(List<String> lista, String valor) {
        if (lista == null || lista.isEmpty()) {
            return true; // Lista vacía es condición trivialmente cumplida
        }

        for (String elemento : lista) {
            if (elemento == null) {
                continue;
            }

            String elementoNormalizado = Nombres.normalizar(elemento);

            if (Nombres.extension(valor).equalsIgnoreCase(elemento)) {
                // Coincidencia por extensión (ya hecha en el bucle principal, pero aquí para consistencia lógica si fuera necesario)
                return true;
            } else if (elementoNormalizado.equals(valor)) {
                // Coincidencia exacta normalizada (para palabras completas o patrones glob simples)
                return true;
            } else if (valor.contains(elementoNormalizado)) {
                // Para palabras: el nombre normalizado debe contener la palabra normalizada
                return true;
            } else if (coincideGlob(elemento, valor)) {
                // Para patrones: usamos la lógica de glob
                return true;
            }
        }

        return false;
    }
}
```

```java
// src/test/java/com/ordenador/service/ClasificadorTest.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests para la clase Clasificador. */
public class ClasificadorTest {

    @Test
    void testCoincideGlobEjemplos() {
        // rutina-* coincide con Rutina-Octubre.pdf (ignora mayúsculas)
        assertTrue(Clasificador.coincideGlob("rutina-*", "Rutina-Octubre.pdf"));
        assertFalse(Clasificador.coincideGlob("rutina-*", "foto.jpg"));

        // plan-* debe empezar con plan-, mi-plan-2.json no coincide porque empieza con 'mi'
        assertFalse(Clasificador.coincideGlob("plan-*", "mi-plan-2.json"));
        assertTrue(Clasificador.coincideGlob("plan-*", "plan-2024.json"));

        // a?c.txt: ? es un carácter, abc.txt sí coincide
        assertTrue(Clasificador.coincideGlob("a?c.txt", "abc.txt"));
        assertTrue(Clasificador.coincideGlob("a?c.txt", "axc.txt"));
        assertFalse(Clasificador.coincideGlob("a?c.txt", "ac.txt")); // falta un carácter
        assertFalse(Clasificador.coincideGlob("a?c.txt", "abxc.txt")); // más de un carácter

        // a.c: el punto es literal, no coincide con abc (no tiene punto)
        assertFalse(Clasificador.coincideGlob("a.c", "abc"));
        assertTrue(Clasificador.coincideGlob("a.c", "a.c"));
    }

    @Test
    void testReglaExtensiones() {
        // Regla con extensiones = ["deb", "AppImage"]
        List<String> exts = List.of("deb", "AppImage");
        Regla regla = new Regla("paquete", Accion.MOVER, exts, null, null, "/ruta/paquetes", null, 0);

        // x.DEB coincide (ignora mayúsculas)
        assertTrue(Clasificador.clasificar("x.DEB", List.of(regla)).isPresent());
        // y.appimage coincide (ignora mayúsculas)
        assertTrue(Clasificador.clasificar("y.appimage", List.of(regla)).isPresent());
        // z.zip no coincide
        assertFalse(Clasificador.clasificar("z.zip", List.of(regla)).isPresent());
    }

    @Test
    void testReglaPalabras() {
        // Regla con extensiones = ["pdf"] y palabras = ["CALCULO NUMERIC", "MECANICA DE FLUIDOS"]
        List<String> exts = List.of("pdf");
        List<String> palabras = List.of("CALCULO NUMERIC", "MECANICA DE FLUIDOS");
        Regla regla = new Regla("estudio", Accion.AVISAR, exts, null, palabras, null, "Estudiando: {palabra}", 0);

        // Mecánica_de_Fluidos-TP1.pdf coincide con palabra "MECANICA DE FLUIDOS"
        String nombre1 = "Mecánica_de_Fluidos-TP1.pdf";
        Optional<Coincidencia> resultado1 = Clasificador.clasificar(nombre1, List.of(regla));
        assertTrue(resultado1.isPresent());
        assertEquals("MECANICA DE FLUIDOS", resultado1.get().palabra());

        // Mecánica_de_Fluidos.docx no coincide (extensión distinta)
        String nombre2 = "Mecánica_de_Fluidos.docx";
        assertFalse(Clasificador.clasificar(nombre2, List.of(regla)).isPresent());

        // Calculo_Numerico.pdf coincide con palabra "CALCULO NUMERIC"
        String nombre3 = "Calculo_Numerico.pdf";
        Optional<Coincidencia> resultado3 = Clasificador.clasificar(nombre3, List.of(regla));
        assertTrue(resultado3.isPresent());
        assertEquals("CALCULO NUMERIC", resultado3.get().palabra());
    }

    @Test
    void testPrimeraReglaGana() {
        // Dos reglas que coinciden, gana la primera.
        Regla regla1 = new Regla("primero", Accion.MOVER, List.of("txt"), null, null, "/ruta/primero", "msg1", 0);
        Regla regla2 = new Regla("segundo", Accion.AVISAR, List.of("txt"), null, null, "/ruta/segundo", "msg2", 0);

        String nombre = "documento.txt";
        Optional<Coincidencia> resultado = Clasificador.clasificar(nombre, List.of(regla1, regla2));
        assertTrue(resultado.isPresent());
        assertEquals("primero", resultado.get().regla().nombre());
    }

    @Test
    void testReglaConListasNulas() {
        // new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0).extensiones() es una lista vacía.
        Regla regla = new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0);
        assertNotNull(regla.extensiones());
        assertTrue(regla.extensiones().isEmpty());

        // Una regla con todas las listas vacías coincide con todo (si no hay otras reglas que coincidan antes)
        Regla reglaVacia = new Regla("todo", Accion.AVISAR, null, null, null, null, "Mensaje genérico", 0);
        Optional<Coincidencia> resultado = Clasificador.clasificar("cualquier_archivo.pdf", List.of(reglaVacia));
        assertTrue(resultado.isPresent());
    }

    @Test
    void testSinReglas() {
        // Si no hay reglas, devuelve Optional.empty()
        assertFalse(Clasificador.clasificar("archivo.txt", List.of()).isPresent());
    }
}
```

<!-- meta: {"segundos": 325.6, "finish_reason": "stop", "usage": {"prompt_tokens": 2203, "completion_tokens": 3090, "total_tokens": 5293, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
