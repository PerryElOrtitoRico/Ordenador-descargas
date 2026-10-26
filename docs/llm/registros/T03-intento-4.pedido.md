<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 1243, max_tokens 6917 -->

## system

Sos un programador Java cuidadoso. Respondé SOLO con los archivos pedidos, completos (nada de '...'), cada uno en su propio bloque de código. La primera línea de cada bloque es un comentario con la ruta del archivo, por ejemplo `// src/main/java/com/ordenador/service/X.java` (o `# systemd/x.service` si no es Java). Sin explicaciones fuera de los bloques.

## user

Al correr `mvn -q test` hay este error:

```
COMPILATION ERROR:
ConfigRepoTest.java:[45,51] ')' or ',' expected
```

Archivo actual:

```
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

```

Corregí el error. Devolvé el archivo corregido completo (o los archivos, si hay que cambiar más de uno), con la ruta en la primera línea. No cambies las firmas públicas.
