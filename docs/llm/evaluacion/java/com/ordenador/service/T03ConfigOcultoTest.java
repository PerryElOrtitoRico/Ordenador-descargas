package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T03: no se le muestra al LLM. */
class T03ConfigOcultoTest {

    @Test
    void reglasPorDefecto() {
        Config c = ReglasPorDefecto.crear();
        assertEquals("~/Descargas", c.carpeta());
        assertEquals(5, c.segundosQuieto());
        assertEquals(List.of("instaladores", "rutinas", "facultad", "finanzas", "imagenes"),
                c.reglas().stream().map(Regla::nombre).toList());
        Regla inst = c.reglas().get(0);
        assertEquals(Accion.MOVER, inst.accion());
        assertEquals("~/Descargas/Instaladores", inst.destino());
        assertEquals(30, inst.diasPapelera());
        assertEquals(List.of("deb", "AppImage", "exe"), inst.extensiones());
        Regla rut = c.reglas().get(1);
        assertEquals(List.of("rutina-*", "plan-*"), rut.patrones());
        assertEquals("~/Documentos/Carpeta personal/Gimnasio", rut.destino());
        Regla fac = c.reglas().get(2);
        assertEquals(Accion.AVISAR, fac.accion());
        assertEquals(List.of("CALCULO NUMERIC", "INTRODUCCION A LA INGENIERIA", "MECANICA DE FLUIDOS", "TECNOLOGIA MECANICA"),
                fac.palabras());
        assertEquals("Subilo a Drive, carpeta {palabra}/teoria", fac.mensaje());
        assertEquals("~/Documentos/Carpeta personal/Finanzas/entrada", c.reglas().get(3).destino());
        assertTrue(c.reglas().get(3).palabras().contains("mercado pago"));
        assertEquals("~/Imágenes", c.reglas().get(4).destino());
        assertEquals(List.of("png", "jpg", "jpeg", "webp", "gif"), c.reglas().get(4).extensiones());
        assertEquals(0, c.reglas().get(4).diasPapelera());
    }

    @Test
    void cargarOCrearEscribeLosDefectos(@TempDir Path d) throws Exception {
        Path archivo = d.resolve("a/b/reglas.json");
        assertEquals(ReglasPorDefecto.crear(), ConfigRepo.cargarOCrear(archivo));
        assertTrue(Files.exists(archivo));
        String json = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(json.contains("Imágenes"), "tiene que guardar en UTF-8");
        assertTrue(json.contains("\n"), "con setPrettyPrinting");
        assertEquals(ReglasPorDefecto.crear(), ConfigRepo.cargarOCrear(archivo));
    }

    @Test
    void cargarOCrearNoPisaLoEditado(@TempDir Path d) throws Exception {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"/tmp/x\",\"segundosQuieto\":2,\"reglas\":[{\"nombre\":\"todo\","
                + "\"accion\":\"AVISAR\",\"mensaje\":\"hola\"}]}");
        Config c = ConfigRepo.cargarOCrear(archivo);
        assertEquals("/tmp/x", c.carpeta());
        assertEquals(2, c.segundosQuieto());
        assertEquals(1, c.reglas().size());
        assertEquals(List.of(), c.reglas().get(0).extensiones());
        assertTrue(Files.readString(archivo).contains("/tmp/x"));
    }

    @Test
    void guardarYLeerSinTemporal(@TempDir Path d) throws Exception {
        Path archivo = d.resolve("reglas.json");
        Config c = new Config("~/Bajadas", 9, List.of(
                new Regla("r", Accion.MOVER, List.of("zip"), List.of("a*"), List.of("x"), "~/z", null, 3)));
        ConfigRepo.guardar(c, archivo);
        assertEquals(c, ConfigRepo.leer(archivo));
        ConfigRepo.guardar(ReglasPorDefecto.crear(), archivo);
        assertEquals(ReglasPorDefecto.crear(), ConfigRepo.leer(archivo));
        assertFalse(Files.exists(d.resolve("reglas.json.tmp")));
    }

    @Test
    void invalidosLanzanIOExceptionConRuta(@TempDir Path d) throws Exception {
        String[] malos = {
                "",
                "{ reglas: [",
                "{\"carpeta\":\"~/D\"}",
                "{\"reglas\":[{\"nombre\":\"x\",\"accion\":\"COPIAR\",\"destino\":\"~/x\"}]}",
                "{\"reglas\":[{\"accion\":\"MOVER\",\"destino\":\"~/x\"}]}",
                "{\"reglas\":[{\"nombre\":\" \",\"accion\":\"MOVER\",\"destino\":\"~/x\"}]}",
                "{\"reglas\":[{\"nombre\":\"x\",\"accion\":\"MOVER\"}]}",
                "{\"reglas\":[{\"nombre\":\"x\",\"accion\":\"AVISAR\"}]}",
        };
        for (int i = 0; i < malos.length; i++) {
            Path archivo = d.resolve("malo" + i + ".json");
            Files.writeString(archivo, malos[i]);
            IOException e = assertThrows(IOException.class, () -> ConfigRepo.leer(archivo), "caso " + i + ": " + malos[i]);
            assertNotNull(e.getMessage(), "caso " + i);
            assertTrue(e.getMessage().contains(archivo.toString()), "caso " + i + " sin la ruta: " + e.getMessage());
        }
    }

    @Test
    void valoresQueFaltan(@TempDir Path d) throws Exception {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"reglas\":[]}");
        Config c = ConfigRepo.leer(archivo);
        assertEquals("~/Descargas", c.carpeta());
        assertEquals(5, c.segundosQuieto());
        assertEquals(List.of(), c.reglas());
        Files.writeString(archivo, "{\"segundosQuieto\":-3,\"reglas\":[]}");
        assertEquals(5, ConfigRepo.leer(archivo).segundosQuieto());
    }
}
