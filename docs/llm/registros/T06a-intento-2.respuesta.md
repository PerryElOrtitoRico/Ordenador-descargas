```java
// src/main/java/com/ordenador/service/Papelera.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Manda a la papelera lo que lleva diasPapelera días en la carpeta de su regla. Nunca borra. */
public final class Papelera {

    private Papelera() { }

    /** Quien manda un archivo a la papelera. En los tests se usa uno falso que solo anota. */
    public interface Tacho {
        void tirar(Path archivo) throws IOException;
    }

    /** Ejecuta: gio trash <ruta absoluta>. Si el código de salida no es 0 → IOException con la ruta. */
    public static final Tacho GIO = archivo -> {
        Process p = new ProcessBuilder("gio", "trash", archivo.toAbsolutePath().toString()).inheritIO().start();
        int codigo;
        try {
            codigo = p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido al mandar a la papelera: " + archivo, e);
        }
        if (codigo != 0) throw new IOException("gio trash falló (" + codigo + "): " + archivo);
    };

    /**
     * Archivos regulares de primer nivel de la carpeta cuya fecha de modificación es anterior o igual
     * a ahora − dias. Ordenados por nombre. Lista vacía si dias <= 0 o la carpeta no existe.
     */
    public static List<Path> vencidos(Path carpeta, int dias, Instant ahora) throws IOException {
        if (dias <= 0 || !Files.isDirectory(carpeta)) return new ArrayList<>();
        Instant limite = ahora.minus(Duration.ofDays(dias));
        try (Stream<Path> s = Files.list(carpeta)) {
            List<Path> lista = new ArrayList<>();
            for (Path p : s.sorted().toList()) {
                if (Files.isRegularFile(p) && !Files.getLastModifiedTime(p).toInstant().isAfter(limite)) {
                    lista.add(p);
                }
            }
            return lista;
        }
    }

    /**
     * Para cada regla MOVER con diasPapelera > 0: vencidos(Rutas.expandir(destino, home), diasPapelera, ahora).
     * Si no se simula, tacho.tirar(...) de cada uno. Devuelve todos los vencidos encontrados, en orden de regla.
     */
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException {
        List<Path> todos = new ArrayList<>();
        for (Regla r : config.reglas()) {
            if (r.accion() == Accion.MOVER && r.diasPapelera() > 0) {
                List<Path> v = vencidos(Rutas.expandir(r.destino(), home), r.diasPapelera(), ahora);
                todos.addAll(v);
                if (!simular) {
                    for (Path p : v) tacho.tirar(p);
                }
            }
        }
        return todos;
    }
}
```

```java
// src/test/java/com/ordenador/service/PapeleraTest.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PapeleraTest {

    static final Instant AHORA = Instant.parse("2026-10-25T12:00:00Z");

    /** Crea carpeta/nombre con fecha de modificación de hace diasAtras días. */
    static Path archivo(Path carpeta, String nombre, int diasAtras) throws Exception {
        Files.createDirectories(carpeta);
        Path p = Files.writeString(carpeta.resolve(nombre), "x");
        Files.setLastModifiedTime(p, FileTime.from(AHORA.minus(Duration.ofDays(diasAtras))));
        return p;
    }

    @Test
    void vencidosSoloLosViejos(@TempDir Path home) throws Exception {
        Path descargas = Rutas.expandir("~/Descargas", home);
        Path instaladores = Files.createDirectory(descargas.resolve("Instaladores"));
        Path viejo = archivo(instaladores, "viejo.deb", 31);
        Path nuevo = archivo(instaladores, "nuevo.deb", 2);

        List<Path> resultado = Papelera.vencidos(instaladores, 30, AHORA);
        assertEquals(1, resultado.size());
        assertEquals(viejo, resultado.get(0));
    }

    @Test
    void limpiarUsaElTachoYRespetaSimular(@TempDir Path home) throws Exception {
        Config config = new Config("~/Descargas", 5, List.of(new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30)));
        List<Path> tirados = new ArrayList<>();
        Papelera.Tacho t = tirados::add;

        Path descargas = Rutas.expandir("~/Descargas", home);
        Path instaladores = Files.createDirectory(descargas.resolve("Instaladores"));
        Path viejo = archivo(instaladores, "viejo.deb", 31);

        List<Path> resultadoSimular = Papelera.limpiar(config, home, AHORA, t, true);
        assertEquals(1, resultadoSimular.size());
        assertTrue(tirados.isEmpty());

        List<Path> resultadoReal = Papelera.limpiar(config, home, AHORA, t, false);
        assertEquals(1, resultadoReal.size());
        assertEquals(viejo, resultadoReal.get(0));
        assertEquals(1, tirados.size());
        assertEquals(viejo, tirados.get(0));
    }

    @Test
    void diasCeroNoSeLimpia(@TempDir Path home) throws Exception {
        Config config = new Config("~/Descargas", 5, List.of(new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 0)));
        List<Path> tirados = new ArrayList<>();
        Papelera.Tacho t = tirados::add;

        Path descargas = Rutas.expandir("~/Descargas", home);
        Path instaladores = Files.createDirectory(descargas.resolve("Instaladores"));
        archivo(instaladores, "viejo.deb", 31);

        List<Path> resultado = Papelera.limpiar(config, home, AHORA, t, false);
        assertTrue(resultado.isEmpty());
        assertTrue(tirados.isEmpty());
    }
}
```

<!-- meta: {"segundos": 247.5, "finish_reason": "stop", "usage": {"prompt_tokens": 3362, "completion_tokens": 1656, "total_tokens": 5018, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
