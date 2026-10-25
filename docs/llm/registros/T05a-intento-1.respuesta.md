```java
// src/main/java/com/ordenador/service/Ordenador.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Movimiento;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Aplica a cada archivo la acción de su regla (sección 4 de la especificación). */
public final class Ordenador {

    private final Config config;
    private final Path home;
    private final Registro registro;
    private final Notificador notificador;
    private final boolean simular;

    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular) {
        this.config = config;
        this.home = home;
        this.registro = registro;
        this.notificador = notificador;
        this.simular = simular;
    }

    /**
     * Decide qué hacer con el archivo:
     * no es archivo regular o es temporal → IGNORADO; sin regla → SIN_REGLA;
     * regla AVISAR → avisar(...); regla MOVER → mover(...).
     */
    public Resultado procesar(Path archivo) throws IOException {
        String nombre = archivo.getFileName().toString();
        if (!Files.isRegularFile(archivo) || Nombres.esTemporal(nombre)) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Optional<Coincidencia> c = Clasificador.clasificar(nombre, config.reglas());
        if (c.isEmpty()) return new Resultado(archivo, Resultado.Estado.SIN_REGLA, null, null);
        if (c.get().regla().accion() == Accion.AVISAR) return avisar(archivo, c.get());
        return mover(archivo, c.get());
    }

    /** Regla AVISAR: no mueve; notifica (si no se simula) y devuelve AVISADO con el mensaje. */
    private Resultado avisar(Path archivo, Coincidencia c) {
        String palabra = c.palabra() == null ? "" : c.palabra();
        String mensaje = c.regla().mensaje().replace("{palabra}", palabra);
        if (!simular) notificador.notificar("Ordenador", archivo.getFileName() + ": " + mensaje);
        return new Resultado(archivo, Resultado.Estado.AVISADO, null, mensaje);
    }

    /** Regla MOVER: mueve sin pisar, anota y notifica; simulando, solo devuelve lo que haría. */
    private Resultado mover(Path archivo, Coincidencia c) throws IOException {
        Path destino = Rutas.expandir(c.regla().destino(), home);
        Files.createDirectories(destino);
        String nombreDestino = Nombres.nombreLibre(destino, archivo.getFileName().toString());
        Path rutaDestino = destino.resolve(nombreDestino);
        
        if (!simular) {
            Files.move(archivo, rutaDestino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Movimiento m = new Movimiento(LocalDateTime.now(), archivo, rutaDestino);
            registro.anotar(m);
            notificador.notificar("Ordenador", archivo.getFileName() + " → " + rutaDestino.toString());
        }
        
        // Si es papelera > 0, establecer fecha de modificación ahora para que pase el tiempo
        if (c.regla().diasPapelera() > 0) {
            FileTime now = FileTime.from(Instant.now());
            Files.setLastModifiedTime(rutaDestino, now);
        }

        return new Resultado(archivo, Resultado.Estado.MOVIDO, rutaDestino, null);
    }

    /**
     * procesar() de cada archivo regular de primer nivel de Rutas.expandir(config.carpeta(), home),
     * ordenados por nombre; las subcarpetas no se tocan. Lista vacía si la carpeta no existe.
     */
    public List<Resultado> ordenarCarpeta() throws IOException {
        Path carpeta = Rutas.expandir(config.carpeta(), home);
        if (!Files.exists(carpeta)) return new ArrayList<>();
        
        List<Resultado> resultados = new ArrayList<>();
        try (Stream<Path> stream = Files.list(carpeta)) {
            stream.filter(Files::isRegularFile)
                  .sorted(Path::getFileName)
                  .forEach(p -> {
                      try {
                          resultados.add(procesar(p));
                      } catch (IOException e) {
                          throw new RuntimeException(e);
                      }
                  });
        }
        return resultados;
    }
}

// src/test/java/com/ordenador/service/OrdenadorTest.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdenadorTest {

    @TempDir Path home;
    Path descargas;
    Registro registro;
    List<String> avisos;

    /** Imágenes → ~/Imágenes; PDF con "FISICA" → aviso. */
    static final List<Regla> REGLAS = List.of(
            new Regla("img", Accion.MOVER, List.of("png", "jpg"), null, null, "~/Imágenes", null, 0),
            new Regla("fac", Accion.AVISAR, List.of("pdf"), null, List.of("FISICA"), null, "Subilo a {palabra}/teoria", 0));

    @BeforeEach
    void preparar() throws Exception {
        descargas = Files.createDirectories(home.resolve("Descargas"));
        registro = new Registro(home.resolve("log.txt"));
        avisos = new ArrayList<>();
    }

    /** Un Ordenador sobre home/Descargas con REGLAS y un notificador falso que guarda los cuerpos en avisos. */
    Ordenador ordenador(boolean simular) {
        return new Ordenador(new Config("~/Descargas", 5, REGLAS), home, registro, (t, c) -> avisos.add(c), simular);
    }

    @Test
    void avisarNoMueve() throws Exception {
        Path archivo = descargas.resolve("Fisica_TP1.pdf");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.AVISADO, r.estado());
        assertEquals("Subilo a FISICA/teoria", r.mensaje());
        assertTrue(Files.exists(archivo));
        assertEquals(List.of("Fisica_TP1.pdf: Subilo a FISICA/teoria"), avisos);
    }

    @Test
    void temporalIgnoradoYSinRegla() throws Exception {
        Path temporal = descargas.resolve("video.mp4.part");
        Files.writeString(temporal, "contenido");
        
        Path sinRegla = descargas.resolve("nada.xyz");
        Files.writeString(sinRegla, "contenido");
        
        List<Resultado> resultados = ordenador(false).ordenarCarpeta();
        
        assertEquals(2, resultados.size());
        
        Resultado r1 = resultados.get(0);
        Resultado r2 = resultados.get(1);
        
        // El orden depende de la ordenación por nombre: "nada.xyz" < "video.mp4.part"
        assertEquals(Resultado.Estado.SIN_REGLA, r1.estado());
        assertEquals(Resultado.Estado.IGNORADO, r2.estado());
        
        assertTrue(Files.exists(descargas.resolve("nada.xyz")));
        assertTrue(Files.exists(descargas.resolve("video.mp4.part")));
        assertEquals(List.of(), avisos);
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertTrue(Files.exists(r.rutaDestino()));
        assertFalse(Files.exists(archivo));
    }

    @Test
    void noPisa() throws Exception {
        Path destino = home.resolve("Imágenes");
        Files.createDirectories(destino);
        Path existente = destino.resolve("foto.png");
        Files.writeString(existente, "original");
        
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "nuevo");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        // El existente debe mantener su contenido original
        assertEquals("original", Files.readString(existente));
    }

    @Test
    void simularNoTocaNada() throws Exception {
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(true).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertTrue(Files.exists(archivo)); // No se movió físicamente
    }

    @Test
    void ordenarCarpeta() throws Exception {
        Path archivo1 = descargas.resolve("a.png");
        Path archivo2 = descargas.resolve("b.pdf");
        Files.writeString(archivo1, "contenido");
        Files.writeString(archivo2, "contenido");
        
        List<Resultado> resultados = ordenador(false).ordenarCarpeta();
        
        assertEquals(2, resultados.size());
        assertEquals(Resultado.Estado.MOVIDO, resultados.get(0).estado()); // a.png
        assertEquals(Resultado.Estado.AVISADO, resultados.get(1).estado()); // b.pdf
    }
}
```

<!-- meta: {"segundos": 297.9, "finish_reason": "stop", "usage": {"prompt_tokens": 2730, "completion_tokens": 2311, "total_tokens": 5041, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
