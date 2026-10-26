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
        String nombre = archivo.getFileName().toString();
        Path carpeta = Rutas.expandir(c.regla().destino(), home);
        if (carpeta.equals(archivo.getParent())) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Path destino = Nombres.nombreLibre(carpeta, nombre);
        String mensaje = nombre + " → " + destino;
        if (simular) return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
        Files.createDirectories(carpeta);
        Files.move(archivo, destino);
        if (c.regla().diasPapelera() > 0) Files.setLastModifiedTime(destino, FileTime.from(Instant.now()));
        registro.anotar(new Movimiento(LocalDateTime.now(), archivo, destino));
        notificador.notificar("Ordenador", mensaje);
        return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
    }

    /**
     * procesar() de cada archivo regular de primer nivel de Rutas.expandir(config.carpeta(), home),
     * ordenados por nombre; las subcarpetas no se tocan. Lista vacía si la carpeta no existe.
     */
    public List<Resultado> ordenarCarpeta() throws IOException {
        Path carpeta = Rutas.expandir(config.carpeta(), home);
        if (!Files.isDirectory(carpeta)) return new ArrayList<>();
        
        try (Stream<Path> s = Files.list(carpeta)) {
            List<Path> archivos = s.filter(Files::isRegularFile).sorted().toList();
            List<Resultado> resultados = new ArrayList<>();
            for (Path p : archivos) {
                resultados.add(procesar(p));
            }
            return resultados;
        }
    }
}

```

```java
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
        Path part = Files.writeString(descargas.resolve("video.mp4.part"), "x");
        Path nada = Files.writeString(descargas.resolve("nada.xyz"), "x");
        assertEquals(Resultado.Estado.IGNORADO, ordenador(false).procesar(part).estado());
        assertEquals(Resultado.Estado.SIN_REGLA, ordenador(false).procesar(nada).estado());
        assertTrue(Files.exists(part));
        assertTrue(Files.exists(nada));
        assertEquals(List.of(), avisos);
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        Path archivo = descargas.resolve("foto.PNG");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.PNG → " + home.resolve("Imágenes/foto.PNG"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.PNG")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto.PNG")));
        assertEquals(1, avisos.size());
        assertEquals(1, registro.leer().size());
    }

    @Test
    void noPisa() throws Exception {
        Path destino = home.resolve("Imágenes");
        Files.createDirectories(destino);
        Path archivoViejo = destino.resolve("foto.png");
        Files.writeString(archivoViejo, "viejo");
        
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "nuevo");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.png → " + home.resolve("Imágenes/foto (2).png"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.png")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto (2).png")));
        assertEquals("viejo", Files.readString(archivoViejo));
    }

    @Test
    void simularNoTocaNada() throws Exception {
        Path archivo = descargas.resolve("a.jpg");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(true).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("a.jpg → " + home.resolve("Imágenes/a.jpg"), r.mensaje());
        assertTrue(Files.exists(descargas.resolve("a.jpg")));
        assertFalse(Files.exists(home.resolve("Imágenes")));
        assertEquals(0, avisos.size());
        assertEquals(0, registro.leer().size());
    }

    @Test
    void ordenarCarpeta() throws Exception {
        Path carpeta = home.resolve("Descargas");
        Files.createDirectories(carpeta);
        
        // Crear archivos de primer nivel
        Path bPng = carpeta.resolve("b.png");
        Files.writeString(bPng, "contenido_b");
        
        Path aXyz = carpeta.resolve("a.xyz");
        Files.writeString(aXyz, "contenido_a");
        
        // Crear subcarpeta con archivo interno (no debe ser tocado)
        Path subCarpeta = carpeta.resolve("sub");
        Files.createDirectories(subCarpeta);
        Path cPng = subCarpeta.resolve("c.png");
        Files.writeString(cPng, "contenido_c");
        
        // Ejecutar ordenarCarpeta
        List<Resultado> resultados = ordenador(false).ordenarCarpeta();
        
        // Debería haber 2 resultados: primero 'a.xyz' (SIN_REGLA), luego 'b.png' (MOVIDO)
        assertEquals(2, resultados.size());
        
        // Verificar primer resultado: a.xyz
        assertEquals(Resultado.Estado.SIN_REGLA, resultados.get(0).estado());
        assertEquals(aXyz, resultados.get(0).archivo());
        assertNull(resultados.get(0).destino());
        assertNull(resultados.get(0).mensaje());
        
        // Verificar segundo resultado: b.png movido a ~/Imágenes/b.png
        assertEquals(Resultado.Estado.MOVIDO, resultados.get(1).estado());
        assertEquals(bPng, resultados.get(1).archivo());
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("b.png")));
        assertFalse(Files.exists(carpeta.resolve("b.png")));
        
        // Verificar que sub/c.png sigue donde estaba
        assertTrue(Files.exists(cPng));
    }
}
```

<!-- meta: {"segundos": 265.8, "finish_reason": "stop", "usage": {"prompt_tokens": 3289, "completion_tokens": 2628, "total_tokens": 5917, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
