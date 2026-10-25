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
        Path origen = d.resolve("a");
        Path destino = d.resolve("b");
        Files.createDirectories(origen);
        Files.createDirectories(destino);
        Path archivoX = destino.resolve("x.pdf");
        Files.writeString(archivoX, "contenido");
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m = new Movimiento(LocalDateTime.now(), origen.resolve("x.pdf"), destino.resolve("x.pdf"));
        r.anotar(m);
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isPresent());
        Path adonde = deshacer.get().destino();
        assertTrue(Files.exists(adonde));
        assertFalse(Files.exists(destino.resolve("x.pdf")));
        List<Movimiento> leido = r.leer();
        assertTrue(leido.isEmpty());
    }

    @Test
    void deshacerNoPisa(@TempDir Path d) throws Exception {
        Path carpeta = d.resolve("a");
        Files.createDirectories(carpeta);
        Path archivoX1 = carpeta.resolve("x.pdf");
        Files.writeString(archivoX1, "contenido1");
        Path destino = d.resolve("b");
        Files.createDirectories(destino);
        Path archivoX2 = destino.resolve("x.pdf");
        Files.writeString(archivoX2, "contenido2");
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Movimiento m = new Movimiento(LocalDateTime.now(), carpeta.resolve("x.pdf"), destino.resolve("x.pdf"));
        r.anotar(m);
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isPresent());
        Path adonde = deshacer.get().destino();
        assertEquals(carpeta.resolve("x (2).pdf"), adonde);
        assertEquals("contenido1", Files.readString(carpeta.resolve("x.pdf")));
        assertEquals("contenido2", Files.readString(adonde));
        List<Movimiento> leido = r.leer();
        assertTrue(leido.isEmpty());
    }

    @Test
    void deshacerSinRegistro(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("datos/movimientos.log"));
        Optional<Movimiento> deshacer = r.deshacerUltimo();
        assertTrue(deshacer.isEmpty());
    }
}
