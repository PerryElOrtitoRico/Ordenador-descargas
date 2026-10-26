package com.ordenador.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PendientesTest {

    static final Path A = Path.of("/d/a.pdf");
    static final Path B = Path.of("/d/b.pdf");

    @Test
    void esperaElTiempoQuieto() {
        Pendientes p = new Pendientes(1000);
        p.tocar(A, 0);
        
        // 999ms no es suficiente
        assertTrue(p.listos(999).isEmpty());
        assertEquals(1, p.cantidad());

        // 1000ms es suficiente
        List<Path> listos1 = p.listos(1000);
        assertEquals(List.of(A), listos1);
        assertEquals(0, p.cantidad());
    }

    @Test
    void tocarDeNuevoReiniciaLaEspera() {
        Pendientes p = new Pendientes(1000);

        // 1. Tocar A a tiempo 0
        p.tocar(A, 0);
        
        // 2. Tocar A de nuevo a tiempo 800 (reinicia la espera)
        p.tocar(A, 800);
        
        // 3. Intentar listar a tiempo 1500. Esperamos que no esté listo (800 + 1000 = 1800)
        assertTrue(p.listos(1500).isEmpty());
        assertEquals(1, p.cantidad());

        // 4. Intentar listar a tiempo 1800. Ahora debería estar listo.
        List<Path> listos1 = p.listos(1800);
        assertEquals(List.of(A), listos1);
        assertEquals(0, p.cantidad());

        // Prueba con dos archivos diferentes
        Pendientes p2 = new Pendientes(1000);
        p2.tocar(B, 0);
        p2.tocar(A, 0);
        
        // Listar a tiempo 1000. Ambos deberían estar listos y ordenados.
        List<Path> listos2 = p2.listos(1000);
        assertEquals(List.of(A, B), listos2);
        assertEquals(0, p2.cantidad());
    }
}
