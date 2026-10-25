package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T05c: no se le muestra al LLM. */
class T05cOrdenarCarpetaOcultoTest {


    @TempDir Path home;
    Path descargas;
    Registro registro;
    List<String> avisos;
    Notificador falso;

    static final List<Regla> REGLAS = List.of(
            new Regla("inst", Accion.MOVER, List.of("deb"), null, null, "~/Descargas/Instaladores", null, 30),
            new Regla("fac", Accion.AVISAR, List.of("pdf"), null, List.of("FISICA", "QUIMICA"), null,
                    "Subilo a Drive, carpeta {palabra}/teoria", 0),
            new Regla("img", Accion.MOVER, List.of("png", "jpg"), null, null, "~/Imágenes", null, 0),
            new Regla("todo-pdf", Accion.AVISAR, List.of("pdf"), null, null, null, "Sin {palabra}materia", 0));

    @BeforeEach
    void preparar() throws Exception {
        descargas = Files.createDirectories(home.resolve("Descargas"));
        registro = new Registro(home.resolve("datos/movimientos.log"));
        avisos = new ArrayList<>();
        falso = (titulo, cuerpo) -> avisos.add(titulo + "|" + cuerpo);
    }

    private Ordenador ordenador(boolean simular) {
        return new Ordenador(new Config("~/Descargas", 5, REGLAS), home, registro, falso, simular);
    }

    @Test
    void ordenarCarpetaEnOrdenSinSubcarpetas() throws Exception {
        Files.writeString(descargas.resolve("b.png"), "x");
        Files.writeString(descargas.resolve("a.xyz"), "x");
        Files.writeString(descargas.resolve("c.deb"), "x");
        Files.createDirectories(descargas.resolve("sub"));
        Files.writeString(descargas.resolve("sub/d.png"), "x");
        List<Resultado> rs = ordenador(false).ordenarCarpeta();
        assertEquals(List.of("a.xyz", "b.png", "c.deb"), rs.stream().map(r -> r.archivo().getFileName().toString()).toList());
        assertEquals(List.of(Resultado.Estado.SIN_REGLA, Resultado.Estado.MOVIDO, Resultado.Estado.MOVIDO),
                rs.stream().map(Resultado::estado).toList());
        assertTrue(Files.exists(descargas.resolve("sub/d.png")));
        assertTrue(Files.exists(home.resolve("Descargas/Instaladores/c.deb")));
        Ordenador sinCarpeta = new Ordenador(new Config("~/NoExiste", 5, REGLAS), home, registro, falso, false);
        assertEquals(List.of(), sinCarpeta.ordenarCarpeta());
    }
}
