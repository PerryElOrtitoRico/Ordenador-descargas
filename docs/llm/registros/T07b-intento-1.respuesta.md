```java
// src/main/java/com/ordenador/service/Vigilante.java
package com.ordenador.service;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Vigila una carpeta (sin subcarpetas) y avisa cuando un archivo lleva quietoMs sin cambiar. */
public final class Vigilante implements AutoCloseable {

    private final Path carpeta;
    private final Pendientes pendientes;
    private final Consumer<Path> alEstarListo;
    private final WatchService watchService;

    /** Abre un WatchService y registra la carpeta para ENTRY_CREATE y ENTRY_MODIFY. */
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException {
        this.carpeta = carpeta;
        this.pendientes = new Pendientes(quietoMs);
        this.alEstarListo = alEstarListo;
        this.watchService = FileSystems.getDefault().newWatchService();
        carpeta.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /** Bucle hasta close() o interrupción del hilo. */
    public void correr() {
        try {
            while (true) {
                WatchKey key = watchService.poll(200, TimeUnit.MILLISECONDS);
                if (key != null) {
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        if (ev.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        Path ruta = carpeta.resolve((Path) ev.context());
                        if (!Nombres.esTemporal(ruta.getFileName().toString())) pendientes.tocar(ruta, System.currentTimeMillis());
                    }
                    key.reset();
                }
                for (Path ruta : pendientes.listos(System.currentTimeMillis())) {
                    if (descargaEnCurso(ruta)) pendientes.tocar(ruta, System.currentTimeMillis());   // sigue esperando
                    else if (Files.isRegularFile(ruta)) alEstarListo.accept(ruta);
                }
            }
        } catch (ClosedWatchServiceException e) {
            // se cerró con close(): terminar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** true si existe al lado "<nombre>.part" o "<nombre>.crdownload" (Firefox crea el archivo final vacío mientras baja el .part). */
    public static boolean descargaEnCurso(Path archivo) {
        String n = archivo.getFileName().toString();
        return Files.exists(archivo.resolveSibling(n + ".part")) || Files.exists(archivo.resolveSibling(n + ".crdownload"));
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }
}
```

```java
// src/test/java/com/ordenador/service/VigilanteTest.java
package com.ordenador.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VigilanteTest {

    @Test
    void descargaEnCurso(@TempDir Path d) throws Exception {
        // TODO T07b
        Path x = Files.writeString(d.resolve("x.pdf"), "");
        assertFalse(Vigilante.descargaEnCurso(x));

        Path part = Files.createFile(d.resolve("x.pdf.part"));
        assertTrue(Vigilante.descargaEnCurso(x));
    }

    @Test
    @Timeout(15)
    void avisaUnaVezYNoPorTemporales(@TempDir Path d) throws Exception {
        List<Path> listos = Collections.synchronizedList(new ArrayList<>());
        Vigilante v = new Vigilante(d, 300, listos::add);
        Thread hilo = new Thread(v::correr);
        hilo.start();
        try {
            Thread.sleep(200);
            // TODO T07b: crear b.txt.part y a.txt; esperar hasta 5 s a que llegue a.txt; comprobar que llegó una sola vez y nada más
            Files.writeString(d.resolve("b.txt.part"), "x");
            Path a = Files.writeString(d.resolve("a.txt"), "hola");

            // Esperar hasta 5s a que a.txt aparezca en listos
            long inicio = System.currentTimeMillis();
            while (System.currentTimeMillis() - inicio < 5000) {
                if (!listos.isEmpty()) break;
                Thread.sleep(10);
            }

            // Verificar que solo llegó una vez y es a.txt
            assertEquals(List.of(a), new ArrayList<>(listos));
        } finally {
            v.close();
            hilo.join(3000);
        }
        assertFalse(hilo.isAlive());
    }
}
```

<!-- meta: {"segundos": 115.5, "finish_reason": "stop", "usage": {"prompt_tokens": 2046, "completion_tokens": 1174, "total_tokens": 3220, "completion_tokens_details": {"reasoning_tokens": 0}}, "razonamiento": "no"} -->
