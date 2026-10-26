```java
// src/main/java/com/ordenador/service/Resumen.java
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
```

```java
// src/test/java/com/ordenador/service/ResumenTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumenTest {

    @Test
    void sinOrdenarSaltaTemporalesYCarpetas(@TempDir Path d) throws IOException {
        // Crear archivos y directorios de prueba
        Files.createFile(d.resolve("b.zip"));
        Files.createFile(d.resolve("a.pdf"));
        Files.createFile(d.resolve("x.part"));
        Files.createFile(d.resolve(".oculto"));
        Files.createDirectory(d.resolve("sub"));

        // Ejecutar el método
        List<String> resultado = Resumen.sinOrdenar(d);

        // Esperado: "a.pdf" y "b.zip" en orden alfabético
        List<String> esperado = List.of("a.pdf", "b.zip");
        assertEquals(esperado, resultado);
    }

    @Test
    void textoVacioUnoYVarios() {
        // Caso vacío
        List<String> vacio = List.of();
        assertEquals("Descargas está ordenada.", Resumen.texto(vacio));

        // Caso uno
        List<String> uno = List.of("a.pdf");
        assertEquals("Queda 1 archivo sin ordenar en Descargas:\n- a.pdf", Resumen.texto(uno));

        // Caso varios
        List<String> varios = List.of("a.pdf", "b.zip", "c.txt");
        String esperadoVarios = "Quedan 3 archivos sin ordenar en Descargas:\n- a.pdf\n- b.zip\n- c.txt";
        assertEquals(esperadoVarios, Resumen.texto(varios));
    }
}
```

<!-- meta: {"segundos": 57.0, "finish_reason": "stop", "usage": {"prompt_tokens": 1575, "completion_tokens": 961, "total_tokens": 2536, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
