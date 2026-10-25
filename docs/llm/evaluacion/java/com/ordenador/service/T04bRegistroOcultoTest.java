package com.ordenador.service;

import com.ordenador.model.Movimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T04b: no se le muestra al LLM. */
class T04bRegistroOcultoTest {


    private static final Path HOME = Path.of("/home/f");

    @Test
    void anotarYLeerEnOrden(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("datos/ordenador/movimientos.log"));
        assertEquals(List.of(), r.leer());
        Movimiento m1 = new Movimiento(LocalDateTime.of(2026, 10, 25, 10, 0), d.resolve("Descargas/a b.pdf"), d.resolve("X/a b.pdf"));
        Movimiento m2 = new Movimiento(LocalDateTime.of(2026, 10, 25, 10, 5, 30, 123_000_000), d.resolve("Descargas/c.png"), d.resolve("Imágenes/c (2).png"));
        r.anotar(m1);
        r.anotar(m2);
        assertEquals(List.of(m1, m2), r.leer());
        assertEquals(List.of(m1, m2), new Registro(d.resolve("datos/ordenador/movimientos.log")).leer());
        String linea = Files.readAllLines(d.resolve("datos/ordenador/movimientos.log")).get(0);
        assertEquals(m1.fecha() + "\t" + m1.origen() + "\t" + m1.destino(), linea);
    }

    @Test
    void leerIgnoraLineasMalas(@TempDir Path d) throws Exception {
        Path log = d.resolve("m.log");
        Files.writeString(log, "\nbasura\n2026-13-45T10:00\t/a\t/b\n2026-10-25T10:00\t/a/x\t/b/x\n   \n");
        List<Movimiento> ms = new Registro(log).leer();
        assertEquals(1, ms.size());
        assertEquals(Path.of("/a/x"), ms.get(0).origen());
    }
}
