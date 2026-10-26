package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Tests para la clase Clasificador. */
public class ClasificadorTest {

    @Test
    void testCoincideGlobEjemplos() {
        // rutina-* coincide con Rutina-Octubre.pdf (ignora mayúsculas)
        assertTrue(Clasificador.coincideGlob("rutina-*", "Rutina-Octubre.pdf"));
        assertFalse(Clasificador.coincideGlob("rutina-*", "foto.jpg"));

        // plan-* debe empezar con plan-, mi-plan-2.json no coincide porque empieza con 'mi'
        assertFalse(Clasificador.coincideGlob("plan-*", "mi-plan-2.json"));
        assertTrue(Clasificador.coincideGlob("plan-*", "plan-2024.json"));

        // a?c.txt: ? es un carácter, abc.txt sí coincide
        assertTrue(Clasificador.coincideGlob("a?c.txt", "abc.txt"));
        assertTrue(Clasificador.coincideGlob("a?c.txt", "axc.txt"));
        assertFalse(Clasificador.coincideGlob("a?c.txt", "ac.txt")); // falta un carácter
        assertFalse(Clasificador.coincideGlob("a?c.txt", "abxc.txt")); // más de un carácter

        // a.c: el punto es literal, no coincide con abc (no tiene punto)
        assertFalse(Clasificador.coincideGlob("a.c", "abc"));
        assertTrue(Clasificador.coincideGlob("a.c", "a.c"));
    }

    @Test
    void testReglaExtensiones() {
        // Regla con extensiones = ["deb", "AppImage"]
        List<String> exts = List.of("deb", "AppImage");
        Regla regla = new Regla("paquete", Accion.MOVER, exts, null, null, "/ruta/paquetes", null, 0);

        // x.DEB coincide (ignora mayúsculas)
        assertTrue(Clasificador.clasificar("x.DEB", List.of(regla)).isPresent());
        // y.appimage coincide (ignora mayúsculas)
        assertTrue(Clasificador.clasificar("y.appimage", List.of(regla)).isPresent());
        // z.zip no coincide
        assertFalse(Clasificador.clasificar("z.zip", List.of(regla)).isPresent());
    }

    @Test
    void testReglaPalabras() {
        // Regla con extensiones = ["pdf"] y palabras = ["CALCULO NUMERIC", "MECANICA DE FLUIDOS"]
        List<String> exts = List.of("pdf");
        List<String> palabras = List.of("CALCULO NUMERIC", "MECANICA DE FLUIDOS");
        Regla regla = new Regla("estudio", Accion.AVISAR, exts, null, palabras, null, "Estudiando: {palabra}", 0);

        // Mecánica_de_Fluidos-TP1.pdf coincide con palabra "MECANICA DE FLUIDOS"
        String nombre1 = "Mecánica_de_Fluidos-TP1.pdf";
        Optional<Coincidencia> resultado1 = Clasificador.clasificar(nombre1, List.of(regla));
        assertTrue(resultado1.isPresent());
        assertEquals("MECANICA DE FLUIDOS", resultado1.get().palabra());

        // Mecánica_de_Fluidos.docx no coincide (extensión distinta)
        String nombre2 = "Mecánica_de_Fluidos.docx";
        assertFalse(Clasificador.clasificar(nombre2, List.of(regla)).isPresent());

        // Calculo_Numerico.pdf coincide con palabra "CALCULO NUMERIC"
        String nombre3 = "Calculo_Numerico.pdf";
        Optional<Coincidencia> resultado3 = Clasificador.clasificar(nombre3, List.of(regla));
        assertTrue(resultado3.isPresent());
        assertEquals("CALCULO NUMERIC", resultado3.get().palabra());
    }

    @Test
    void testPrimeraReglaGana() {
        // Dos reglas que coinciden, gana la primera.
        Regla regla1 = new Regla("primero", Accion.MOVER, List.of("txt"), null, null, "/ruta/primero", "msg1", 0);
        Regla regla2 = new Regla("segundo", Accion.AVISAR, List.of("txt"), null, null, "/ruta/segundo", "msg2", 0);

        String nombre = "documento.txt";
        Optional<Coincidencia> resultado = Clasificador.clasificar(nombre, List.of(regla1, regla2));
        assertTrue(resultado.isPresent());
        assertEquals("primero", resultado.get().regla().nombre());
    }

    @Test
    void testReglaConListasNulas() {
        // new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0).extensiones() es una lista vacía.
        Regla regla = new Regla("x", Accion.MOVER, null, null, null, "~/x", null, 0);
        assertNotNull(regla.extensiones());
        assertTrue(regla.extensiones().isEmpty());

        // Una regla con todas las listas vacías coincide con todo (si no hay otras reglas que coincidan antes)
        Regla reglaVacia = new Regla("todo", Accion.AVISAR, null, null, null, null, "Mensaje genérico", 0);
        Optional<Coincidencia> resultado = Clasificador.clasificar("cualquier_archivo.pdf", List.of(reglaVacia));
        assertTrue(resultado.isPresent());
    }

    @Test
    void testSinReglas() {
        // Si no hay reglas, devuelve Optional.empty()
        assertFalse(Clasificador.clasificar("archivo.txt", List.of()).isPresent());
    }
}
