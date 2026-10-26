<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 1816, max_tokens 6344 -->

## system

Sos un programador Java cuidadoso. Respondé SOLO con los archivos pedidos, completos (nada de '...'), cada uno en su propio bloque de código. La primera línea de cada bloque es un comentario con la ruta del archivo, por ejemplo `// src/main/java/com/ordenador/service/X.java` (o `# systemd/x.service` si no es Java). Sin explicaciones fuera de los bloques.

## user

Al correr `mvn -q test` hay este error:

```
COMPILATION ERROR:
Clasificador.java:[69,63] extensiones has private access in com.ordenador.model.Regla
Clasificador.java:[70,60] patrones has private access in com.ordenador.model.Regla
Clasificador.java:[71,60] palabras has private access in com.ordenador.model.Regla
Clasificador.java:[76,27] palabras has private access in com.ordenador.model.Regla
Clasificador.java:[78,42] palabras has private access in com.ordenador.model.Regla
```

Archivo actual:

```
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

Archivo actual:

```
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

Corregí el error. Devolvé el archivo corregido completo (o los archivos, si hay que cambiar más de uno), con la ruta en la primera línea. No cambies las firmas públicas.
