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

/** Test oculto de T07a: no se le muestra al LLM. */
class T07aPendientesOcultoTest {


    private static final Path A = Path.of("/d/a.pdf");
    private static final Path B = Path.of("/d/b.pdf");

    @Test
    void pendientesEsperanElTiempoQuieto() {
        Pendientes p = new Pendientes(1000);
        p.tocar(B, 100);
        p.tocar(A, 0);
        assertEquals(2, p.cantidad());
        assertEquals(List.of(), p.listos(999));
        assertEquals(List.of(A), p.listos(1000));
        assertEquals(1, p.cantidad());
        assertEquals(List.of(), p.listos(1000));
        assertEquals(List.of(B), p.listos(5000));
        assertEquals(0, p.cantidad());
    }

    @Test
    void tocarReiniciaYOrdenaPorRuta() {
        Pendientes p = new Pendientes(1000);
        p.tocar(B, 0);
        p.tocar(A, 0);
        p.tocar(A, 800);
        p.tocar(A, 800);
        assertEquals(2, p.cantidad());
        assertEquals(List.of(B), p.listos(1500));
        assertEquals(List.of(A), p.listos(1800));
        p.tocar(B, 0);
        p.tocar(A, 0);
        assertEquals(List.of(A, B), p.listos(1000));
    }
}
