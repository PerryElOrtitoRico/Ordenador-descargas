package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T02: no se le muestra al LLM. */
class T02ClasificadorOcultoTest {

    private static Regla regla(String nombre, List<String> ext, List<String> pat, List<String> pal) {
        return new Regla(nombre, Accion.MOVER, ext, pat, pal, "~/" + nombre, null, 0);
    }

    @Test
    void reglaConListasNullYCopia() {
        Regla r = new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0);
        assertEquals(List.of(), r.extensiones());
        assertEquals(List.of(), r.patrones());
        assertEquals(List.of(), r.palabras());
        List<String> ext = new ArrayList<>(List.of("pdf"));
        Regla r2 = new Regla("y", Accion.AVISAR, ext, null, null, null, "m", 0);
        ext.add("zip");
        assertEquals(List.of("pdf"), r2.extensiones(), "la regla tiene que copiar la lista");
        Config c = new Config("~/Descargas", 5, List.of(r));
        assertEquals(5, c.segundosQuieto());
        assertEquals(2, Accion.values().length);
    }

    @Test
    void glob() {
        assertTrue(Clasificador.coincideGlob("rutina-*", "Rutina-Octubre.pdf"));
        assertFalse(Clasificador.coincideGlob("plan-*", "mi-plan-2.json"));
        assertTrue(Clasificador.coincideGlob("a?c.txt", "abc.txt"));
        assertFalse(Clasificador.coincideGlob("a?c.txt", "ac.txt"));
        assertFalse(Clasificador.coincideGlob("a.c", "abc"));
        assertTrue(Clasificador.coincideGlob("informe (1)*", "informe (1) final.pdf"));
        assertTrue(Clasificador.coincideGlob("*", ""));
        assertTrue(Clasificador.coincideGlob("[x]+$*", "[X]+$y"));
        assertFalse(Clasificador.coincideGlob("plan", "plan-1"));
    }

    @Test
    void extensionesSinDistinguirMayusculas() {
        List<Regla> rs = List.of(regla("inst", List.of("deb", "AppImage"), null, null));
        assertTrue(Clasificador.clasificar("x.DEB", rs).isPresent());
        assertTrue(Clasificador.clasificar("y.appimage", rs).isPresent());
        assertTrue(Clasificador.clasificar("z.zip", rs).isEmpty());
        assertTrue(Clasificador.clasificar("deb", rs).isEmpty());
    }

    @Test
    void palabrasDevuelveLaQueCoincidioTalCual() {
        Regla fac = new Regla("facultad", Accion.AVISAR, List.of("pdf"), null,
                List.of("CALCULO NUMERIC", "MECANICA DE FLUIDOS"), null, "Subilo a {palabra}", 0);
        Optional<Coincidencia> c = Clasificador.clasificar("Mecánica_de_Fluidos-TP1.pdf", List.of(fac));
        assertTrue(c.isPresent());
        assertSame(fac, c.get().regla());
        assertEquals("MECANICA DE FLUIDOS", c.get().palabra());
        assertEquals("CALCULO NUMERIC", Clasificador.clasificar("calculo.numerico.parcial.PDF", List.of(fac)).get().palabra());
        assertTrue(Clasificador.clasificar("Mecánica_de_Fluidos.docx", List.of(fac)).isEmpty());
        assertTrue(Clasificador.clasificar("fluidos.pdf", List.of(fac)).isEmpty());
    }

    @Test
    void todasLasCondicionesTienenQueCumplirse() {
        List<Regla> rs = List.of(regla("rutinas", List.of("json", "pdf"), List.of("rutina-*", "plan-*"), null));
        assertTrue(Clasificador.clasificar("plan-franco.json", rs).isPresent());
        assertTrue(Clasificador.clasificar("RUTINA-1.PDF", rs).isPresent());
        assertTrue(Clasificador.clasificar("plan-franco.txt", rs).isEmpty());
        assertTrue(Clasificador.clasificar("otro.json", rs).isEmpty());
    }

    @Test
    void ganaLaPrimeraYSinPalabraEsNull() {
        Regla a = regla("a", List.of("pdf"), null, null);
        Regla b = regla("b", null, List.of("*.pdf"), null);
        Optional<Coincidencia> c = Clasificador.clasificar("x.pdf", List.of(a, b));
        assertSame(a, c.get().regla());
        assertNull(c.get().palabra());
        assertSame(b, Clasificador.clasificar("x.pdf", List.of(b, a)).get().regla());
        assertTrue(Clasificador.clasificar("x.pdf", List.of()).isEmpty());
    }

    @Test
    void reglaVaciaCoincideConTodo() {
        Regla todo = regla("todo", null, null, null);
        assertSame(todo, Clasificador.clasificar("cualquier cosa", List.of(todo)).get().regla());
    }
}
