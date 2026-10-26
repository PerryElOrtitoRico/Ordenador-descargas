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
