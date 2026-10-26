// src/main/java/com/ordenador/service/ReglasPorDefecto.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;

import java.util.List;

public final class ReglasPorDefecto {
    private ReglasPorDefecto() { }

    /** Config("~/Descargas", 5, <las 5 reglas de la tabla, en ese orden>). */
    public static Config crear() {
        List<Regla> reglas = List.of(
            new Regla("instaladores", Accion.MOVER, List.of("deb", "AppImage", "exe"), null, null, "~/Descargas/Instaladores", null, 30),
            new Regla("rutinas", Accion.MOVER, List.of("json", "pdf"), List.of("rutina-*", "plan-*"), null, "~/Documentos/Carpeta personal/Gimnasio", null, 0),
            new Regla("facultad", Accion.AVISAR, List.of("pdf"), null, List.of("CALCULO NUMERIC", "INTRODUCCION A LA INGENIERIA", "MECANICA DE FLUIDOS", "TECNOLOGIA MECANICA"), null, "Subilo a Drive, carpeta {palabra}/teoria", 0),
            new Regla("finanzas", Accion.MOVER, List.of("pdf", "csv", "xls", "xlsx"), null, List.of("mercado pago", "mercadopago", "account statement", "resumen de cuenta", "estado de cuenta", "extracto"), "~/Documentos/Carpeta personal/Finanzas/entrada", null, 0),
            new Regla("imagenes", Accion.MOVER, List.of("png", "jpg", "jpeg", "webp", "gif"), null, null, "~/Imágenes", null, 0)
        );

        return new Config("~/Descargas", 5, reglas);
    }
}

// src/main/java/com/ordenador/service/ConfigRepo.java
package com.ordenador.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ordenador.model.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigRepo {
    private ConfigRepo() { }

    /** Si el archivo no existe: crea sus carpetas, guarda crear() y la devuelve. Si existe: leer(archivo). */
    public static Config cargarOCrear(Path archivo) throws IOException {
        if (!Files.exists(archivo)) {
            guardar(ReglasPorDefecto.crear(), archivo);
            return ReglasPorDefecto.crear();
        }
        return leer(archivo);
    }

    /** Lee y valida. Lanza IOException (con la ruta del archivo en el mensaje) si no es JSON válido o no pasa la validación. */
    public static Config leer(Path archivo) throws IOException {
        String contenido = new String(Files.readAllBytes(archivo), "UTF-8");
        
        if (contenido.trim().isEmpty()) {
            throw new IOException("Archivo vacío: " + archivo);
        }

        Gson gson = new Gson();
        Config config;
        try {
            config = gson.fromJson(contenido, Config.class);
        } catch (Exception e) {
            throw new IOException("JSON inválido en " + archivo + ": " + e.getMessage(), e);
        }

        if (config.getReglas() == null) {
            throw new IOException("Reglas null en " + archivo);
        }

        for (int i = 0; i < config.getReglas().size(); i++) {
            Regla regla = config.getReglas().get(i);
            
            if (regla.nombre() == null || regla.nombre().trim().isEmpty()) {
                throw new IOException("Regla " + i + " tiene nombre null o vacío en " + archivo);
            }

            if (regla.accion() == null) {
                throw new IOException("Regla " + i + " tiene accion null en " + archivo);
            }

            if (regla.accion() == Accion.MOVER && (regla.destino() == null || regla.destino().trim().isEmpty())) {
                throw new IOException("Regla " + i + " MOVER sin destino en " + archivo);
            }

            if (regla.accion() == Accion.AVISAR && regla.mensaje() == null) {
                throw new IOException("Regla " + i + " AVISAR sin mensaje en " + archivo);
            }
        }

        String carpeta = config.carpeta();
        int segundosQuieto = config.segundosQuieto();

        if (carpeta == null) {
            carpeta = "~/Descargas";
        }

        if (segundosQuieto <= 0) {
            segundosQuieto = 5;
        }

        return new Config(carpeta, segundosQuieto, config.getReglas());
    }

    /** Gson con setPrettyPrinting(), UTF-8. Atómico: escribir "<archivo>.tmp" y Files.move con REPLACE_EXISTING y ATOMIC_MOVE. */
    public static void guardar(Config config, Path archivo) throws IOException {
        String contenido = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(config);

        Path tmpArchivo = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.writeString(tmpArchivo, contenido, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
        
        Files.move(tmpArchivo, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}

// src/test/java/com/ordenador/service/ConfigRepoTest.java
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
        Config original = new Config("~/MiCarpeta", 10, null);
        Path archivo = d.resolve("reglas.json");

        ConfigRepo.guardar(original, archivo);
        Config leido = ConfigRepo.leer(archivo);

        assertEquals(original.carpeta(), leido.carpeta());
        assertEquals(original.segundosQuieto(), leido.segundosQuieto());
    }

    @Test
    void leer_JSONRotoLanzaIOException() throws IOException {
        Path archivo = d.resolve("reglas.json");
        Files.writeString(archivo, "{ reglas: [\""]);

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

<!-- meta: {"segundos": 308.2, "finish_reason": "stop", "usage": {"prompt_tokens": 2431, "completion_tokens": 2340, "total_tokens": 4771, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
