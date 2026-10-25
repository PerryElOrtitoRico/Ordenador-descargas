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

/** Test oculto de T04c: no se le muestra al LLM. */
class T04cDeshacerOcultoTest {


    private static final Path HOME = Path.of("/home/f");

    @Test
    void deshacerVuelveYSacaLaLinea(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("m.log"));
        Path a = Files.createDirectories(d.resolve("a"));
        Path b = Files.createDirectories(d.resolve("b"));
        Path x1 = Files.writeString(a.resolve("uno.pdf"), "1");
        Path x2 = Files.writeString(a.resolve("dos.pdf"), "2");
        Files.move(x1, b.resolve("uno.pdf"));
        r.anotar(new Movimiento(LocalDateTime.now(), x1, b.resolve("uno.pdf")));
        Files.move(x2, b.resolve("dos.pdf"));
        r.anotar(new Movimiento(LocalDateTime.now(), x2, b.resolve("dos.pdf")));

        Optional<Movimiento> hecho = r.deshacerUltimo();
        assertTrue(hecho.isPresent());
        assertEquals(b.resolve("dos.pdf"), hecho.get().origen());
        assertEquals(x2, hecho.get().destino());
        assertEquals("2", Files.readString(x2));
        assertFalse(Files.exists(b.resolve("dos.pdf")));
        assertEquals(1, r.leer().size());

        r.deshacerUltimo();
        assertEquals("1", Files.readString(x1));
        assertEquals(List.of(), r.leer());
        assertEquals(Optional.empty(), r.deshacerUltimo());
        assertFalse(Files.exists(d.resolve("m.log.tmp")));
    }

    @Test
    void deshacerNoPisaYCreaCarpeta(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("m.log"));
        Path b = Files.createDirectories(d.resolve("b"));
        Files.writeString(b.resolve("x.pdf"), "movido");
        Path origen = d.resolve("a/x.pdf");
        r.anotar(new Movimiento(LocalDateTime.now(), origen, b.resolve("x.pdf")));
        // la carpeta de origen no existe: se crea
        assertEquals(origen, r.deshacerUltimo().get().destino());
        assertEquals("movido", Files.readString(origen));

        Files.move(origen, b.resolve("x.pdf"));
        r.anotar(new Movimiento(LocalDateTime.now(), origen, b.resolve("x.pdf")));
        Files.writeString(origen, "otro");
        Movimiento m = r.deshacerUltimo().get();
        assertEquals(d.resolve("a/x (2).pdf"), m.destino());
        assertEquals("otro", Files.readString(origen));
        assertEquals("movido", Files.readString(d.resolve("a/x (2).pdf")));
    }

    @Test
    void deshacerSiYaNoEstaNoTocaElRegistro(@TempDir Path d) throws Exception {
        Registro r = new Registro(d.resolve("m.log"));
        r.anotar(new Movimiento(LocalDateTime.now(), d.resolve("a/x.pdf"), d.resolve("b/x.pdf")));
        assertThrows(IOException.class, r::deshacerUltimo);
        assertEquals(1, r.leer().size());
    }
}
