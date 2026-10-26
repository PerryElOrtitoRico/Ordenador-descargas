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

/** Test oculto de T07b: no se le muestra al LLM. */
class T07bVigilanteOcultoTest {


    private static final Path A = Path.of("/d/a.pdf");
    private static final Path B = Path.of("/d/b.pdf");

    @Test
    void descargaEnCurso(@TempDir Path d) throws Exception {
        Path x = Files.createFile(d.resolve("x.pdf"));
        assertFalse(Vigilante.descargaEnCurso(x));
        Files.createFile(d.resolve("x.pdf.part"));
        assertTrue(Vigilante.descargaEnCurso(x));
        Path y = Files.createFile(d.resolve("y.zip"));
        Files.createFile(d.resolve("y.zip.crdownload"));
        assertTrue(Vigilante.descargaEnCurso(y));
    }

    private static void esperarHasta(List<Path> lista, int cantidad, long ms) throws InterruptedException {
        long fin = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < fin) {
            synchronized (lista) {
                if (lista.size() >= cantidad) return;
            }
            Thread.sleep(50);
        }
    }

    @Test
    @Timeout(20)
    void vigilaYEsperaQueTermine(@TempDir Path d) throws Exception {
        List<Path> listos = Collections.synchronizedList(new ArrayList<>());
        Vigilante v = new Vigilante(d, 300, listos::add);
        Thread hilo = new Thread(v::correr);
        hilo.start();
        try {
            Thread.sleep(200);
            Files.writeString(d.resolve("b.txt.part"), "bajando");
            Files.writeString(d.resolve(".oculto"), "x");
            Path a = Files.writeString(d.resolve("a.txt"), "hola");
            esperarHasta(listos, 1, 5000);
            Thread.sleep(800);
            assertEquals(List.of(a), new ArrayList<>(listos), "solo a.txt y una sola vez");

            // un archivo que sigue cambiando no se avisa hasta que se queda quieto
            Path c = d.resolve("c.bin");
            for (int i = 0; i < 8; i++) {
                Files.writeString(c, "parte " + i + "\n", java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
                Thread.sleep(100);
                assertFalse(listos.contains(c), "avisó antes de que dejara de cambiar");
            }
            esperarHasta(listos, 2, 5000);
            assertEquals(List.of(a, c), new ArrayList<>(listos));

            // Firefox: el final vacío + .part; recién cuando desaparece el .part se avisa
            Path f = Files.createFile(d.resolve("f.pdf"));
            Path part = Files.writeString(d.resolve("f.pdf.part"), "x");
            Thread.sleep(1200);
            assertFalse(listos.contains(f), "avisó con el .part todavía al lado");
            Files.move(part, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            esperarHasta(listos, 3, 5000);
            assertEquals(List.of(a, c, f), new ArrayList<>(listos));
        } finally {
            v.close();
            hilo.join(3000);
        }
        assertFalse(hilo.isAlive(), "correr() tiene que terminar después de close()");
    }

    @Test
    @Timeout(10)
    void terminaAlInterrumpir(@TempDir Path d) throws Exception {
        try (Vigilante v = new Vigilante(d, 300, p -> { })) {
            Thread hilo = new Thread(v::correr);
            hilo.start();
            Thread.sleep(300);
            hilo.interrupt();
            hilo.join(3000);
            assertFalse(hilo.isAlive());
        }
    }
}
