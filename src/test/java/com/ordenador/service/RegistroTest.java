package com.ordenador.service;

import com.ordenador.model.Movimiento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistroTest {

    @Test
    void anotarYLeer(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m1 = new Movimiento(LocalDateTime.of(2026, 10, 25, 10, 0), Path.of("/origen1"), Path.of("/destino1"));
        Movimiento m2 = new Movimiento(LocalDateTime.of(2026, 10, 26, 11, 0), Path.of("/origen2"), Path.of("/destino2"));
        r.anotar(m1);
        r.anotar(m2);
        List<Movimiento> leido = r.leer();
        assertEquals(2, leido.size());
        assertEquals(m1, leido.get(0));
        assertEquals(m2, leido.get(1));
    }

    @Test
    void leerSinArchivoEsVacio(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("no.log"));
        List<Movimiento> movimientos = r.leer();
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void deshacerVuelveAlOrigen(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void deshacerNoPisa(@TempDir Path d) throws Exception {
        // TODO
    }

    @Test
    void deshacerSinRegistro(@TempDir Path d) throws Exception {
        // TODO
    }
}
