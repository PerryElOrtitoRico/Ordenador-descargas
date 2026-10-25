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

/** Test oculto de T04: no se le muestra al LLM. */
class T04RegistroOcultoTest {

    private static final Path HOME = Path.of("/home/f");

    @Test
    void rutasXdg() {
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of(), HOME));
        assertEquals(Path.of("/x/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", "/x"), HOME));
        assertEquals(Path.of("/home/f/.config/ordenador/reglas.json"), Rutas.config(Map.of("XDG_CONFIG_HOME", ""), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of(), HOME));
        assertEquals(Path.of("/home/f/.local/share/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", ""), HOME));
        assertEquals(Path.of("/d/ordenador/movimientos.log"), Rutas.registro(Map.of("XDG_DATA_HOME", "/d"), HOME));
    }

    @Test
    void expandir() {
        assertEquals(Path.of("/home/f/Imágenes"), Rutas.expandir("~/Imágenes", HOME));
        assertEquals(HOME, Rutas.expandir("~", HOME));
        assertEquals(Path.of("/home/f/Documentos/Carpeta personal/Gimnasio"),
                Rutas.expandir("~/Documentos/Carpeta personal/Gimnasio", HOME));
        assertEquals(Path.of("/tmp/a"), Rutas.expandir("/tmp/a", HOME));
        var e = assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", HOME));
        assertTrue(e.getMessage().contains("Descargas"));
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("~otro/x", HOME));
    }

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
