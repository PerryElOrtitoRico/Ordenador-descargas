package com.ordenador.service;

import com.google.gson.JsonParseException;
import com.ordenador.model.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigRepoTest {

    @TempDir
    Path d;

    @Test
    void cargarOCrear_creaArchivoYDevuelveConfigPorDefecto() throws IOException {
        Path archivo = d.resolve("sub/reglas.json");
        Config config = ConfigRepo.cargarOCrear(archivo);

        assertNotNull(config);
        assertEquals("~/Descargas", config.carpeta());
        assertEquals(5, config.segundosQuieto());
        assertFalse(Files.exists(archivo.resolveSibling(archivo.getFileName() + ".tmp")));
    }

    @Test
    void guardarYLeerDevuelveConfigIgual() throws IOException {
        Config original = new Config("~/MiCarpeta", 10, java.util.List.of());
        Path archivo = d.resolve("reglas.json");

        ConfigRepo.guardar(original, archivo);
        Config leido = ConfigRepo.leer(archivo);

        assertEquals(original.carpeta(), leido.carpeta());
        assertEquals(original, leido);
    }

    @Test
    void leer_JSONRotoLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{ reglas: [\"");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_ReglasNulasLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":5,\"reglas\":null}");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_ReglaSinNombreLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":5,\"reglas\":[{\"accion\":\"MOVER\",\"destino\":\"~/y\"}]}");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_ReglaSinAccionLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":5,\"reglas\":[{\"nombre\":\"test\"}]}");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_ReglaMoverSinDestinoLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":5,\"reglas\":[{\"nombre\":\"test\",\"accion\":\"MOVER\"}]}");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_ReglaAvisarSinMensajeLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":5,\"reglas\":[{\"nombre\":\"test\",\"accion\":\"AVISAR\"}]}");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }

    @Test
    void leer_CarpetaNulaSeUsaPorDefecto() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"segundosQuieto\":5,\"reglas\":[]}");

        Config config = ConfigRepo.leer(archivo);
        assertEquals("~/Descargas", config.carpeta());
    }

    @Test
    void leer_SegundosQuietoNegativoSeUsaPorDefecto() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{\"carpeta\":\"~/x\",\"segundosQuieto\":-1,\"reglas\":[]}");

        Config config = ConfigRepo.leer(archivo);
        assertEquals(5, config.segundosQuieto());
    }

    @Test
    void leer_JSONVacioLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "");

        assertThrows(IOException.class, () -> ConfigRepo.leer(archivo));
    }
}
