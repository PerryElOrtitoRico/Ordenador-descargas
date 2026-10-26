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
        } finally {
            v.close();
            hilo.join(3000);
        }
        assertFalse(hilo.isAlive());
    }
}
