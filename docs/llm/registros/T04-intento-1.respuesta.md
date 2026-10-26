```java
// src/main/java/com/ordenador/model/Movimiento.java
package com.ordenador.model;

import java.time.LocalDateTime;
import java.nio.file.Path;

/**
 * Representa un movimiento realizado por el programa.
 */
public record Movimiento(LocalDateTime fecha, Path origen, Path destino) { }
```

```java
// src/main/java/com/ordenador/service/Rutas.java
package com.ordenador.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Utilidad para obtener las rutas de configuración y registro del programa.
 */
public final class Rutas {
    private Rutas() { }

    /**
     * Obtiene la ruta al archivo de reglas.
     * Prioriza XDG_CONFIG_HOME, si no existe o está vacío usa ~/.config.
     * Añade /ordenador/reglas.json.
     */
    public static Path config(Map<String, String> env, Path home) {
        String xdgConfig = env.get("XDG_CONFIG_HOME");
        if (xdgConfig == null || xdgConfig.isEmpty()) {
            return home.resolve(".config").resolve("ordenador").resolve("reglas.json");
        }
        return Path.of(xdgConfig).resolve("ordenador").resolve("reglas.json");
    }

    /**
     * Obtiene la ruta al archivo de registro de movimientos.
     * Prioriza XDG_DATA_HOME, si no existe o está vacío usa ~/.local/share.
     * Añade /ordenador/movimientos.log.
     */
    public static Path registro(Map<String, String> env, Path home) {
        String xdgData = env.get("XDG_DATA_HOME");
        if (xdgData == null || xdgData.isEmpty()) {
            return home.resolve(".local").resolve("share").resolve("ordenador").resolve("movimientos.log");
        }
        return Path.of(xdgData).resolve("ordenador").resolve("movimientos.log");
    }

    /**
     * Expande una ruta relativa o con '~' usando la carpeta home proporcionada.
     * Las rutas absolutas se devuelven tal cual.
     * Rutas relativas sin '~' (ej: "Descargas") lanzan IllegalArgumentException.
     */
    public static Path expandir(String ruta, Path home) {
        if (ruta.startsWith("~")) {
            // Reemplazar el primer ~ por la ruta home y limpiar barras dobles iniciales si las hubiera
            String expandida = ruta.replaceFirst("^~", home.toString());
            return Path.of(expandida).normalize();
        } else if (ruta.startsWith("/")) {
            // Ruta absoluta, devolver tal cual
            return Path.of(ruta);
        } else {
            // Ruta relativa sin ~ (ej: "Descargas")
            throw new IllegalArgumentException("Ruta relativa no permitida: " + ruta);
        }
    }
}
```

```java
// src/main/java/com/ordenador/service/Registro.java
package com.ordenador.service;

import com.ordenador.model.Movimiento;
import java.io.IOException;
import java.io.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona el registro de movimientos en un archivo de texto.
 */
public final class Registro {
    private final Path archivo;

    public Registro(Path archivo) {
        this.archivo = archivo;
    }

    /**
     * Crea las carpetas si hace falta y agrega una línea al final del archivo.
     * Formato: fecha ISO<TAB>origen<TAB>destino\n
     */
    public void anotar(Movimiento m) throws IOException {
        // Crear directorios padres si no existen
        Files.createDirectories(archivo.getParent());

        String linea = m.fecha().toString() + "\t" + m.origen() + "\t" + m.destino() + "\n";
        try (var writer = java.nio.file.Files.newBufferedWriter(archivo, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(linea);
        }
    }

    /**
     * Lee los movimientos guardados en orden cronológico (más viejo primero).
     * Ignora líneas vacías y malformadas.
     */
    public List<Movimiento> leer() throws IOException {
        if (!Files.exists(archivo)) {
            return new ArrayList<>();
        }

        List<Movimiento> movimientos = new ArrayList<>();
        try (var reader = java.nio.file.Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }

                String[] partes = linea.split("\t", -1); // -1 para mantener barras si las hubiera, aunque el formato usa tab
                if (partes.length != 3) {
                    continue;
                }

                try {
                    LocalDateTime fecha = LocalDateTime.parse(partes[0]);
                    Path origen = Paths.get(partes[1]);
                    Path destino = Paths.get(partes[2]);
                    movimientos.add(new Movimiento(fecha, origen, destino));
                } catch (Exception e) {
                    // Ignorar líneas con fecha inválida
                    continue;
                }
            }
        }
        return movimientos;
    }

    /**
     * Deshace el último movimiento registrado.
     * - Si no hay movimientos, devuelve Optional.empty().
     * - Si el destino ya no existe, lanza IOException y no cambia el registro.
     * - Si el destino existe:
     *   1. Mueve el archivo de destino a la carpeta origen (resolviendo conflictos de nombre).
     *   2. Reescribe el registro eliminando la última línea (atómicamente usando .tmp).
     *   3. Devuelve el nuevo movimiento con fecha actual y la nueva ruta final.
     */
    public Optional<Movimiento> deshacerUltimo() throws IOException {
        List<Movimiento> movimientos = leer();
        if (movimientos.isEmpty()) {
            return Optional.empty();
        }

        Movimiento ultimo = movimientos.get(movimientos.size() - 1);
        
        // Verificar si el destino ya no existe
        if (!Files.exists(ultimo.destino())) {
            throw new IOException("ya no está: " + ultimo.destino());
        }

        // Calcular la nueva ruta de destino (donde quedará tras deshacer)
        Path nuevaRuta = Rutas.expandir(ultimo.origen().toString(), ultimo.origen().getParent());
        
        // Resolver conflicto de nombre si es necesario
        String nombreFinal;
        if (!Files.exists(nuevaRuta)) {
            nombreFinal = ultimo.destino().getFileName().toString();
        } else {
            nombreFinal = Nombres.nombreLibre(ultimo.origen().getParent(), ultimo.destino().getFileName().toString());
        }

        // Construir el path final completo
        Path destinoFinal = ultimo.origen().getParent().resolve(nombreFinal);

        // Mover el archivo
        Files.move(ultimo.destino(), destinoFinal, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Reescribir el registro sin la última línea (atómicamente)
        List<String> lineas = new ArrayList<>();
        try (var reader = java.nio.file.Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.equals(ultimo.fecha().toString() + "\t" + ultimo.origen() + "\t" + ultimo.destino())) {
                    lineas.add(linea);
                }
            }
        }

        // Escribir el archivo temporalmente y luego moverlo para atomicidad
        Path archivoTmp = archivo.resolve(archivo.getFileName().toString() + ".tmp");
        try (var writer = java.nio.file.Files.newBufferedWriter(archivoTmp, StandardCharsets.UTF_8)) {
            for (String l : lineas) {
                writer.write(l);
                writer.newLine();
            }
        }

        Files.move(archivoTmp, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Crear el nuevo movimiento con la fecha actual y la nueva ruta
        Movimiento nuevoMovimiento = new Movimiento(LocalDateTime.now(), ultimo.origen(), destinoFinal);
        return Optional.of(nuevoMovimiento);
    }
}
```

```java
// src/test/java/com/ordenador/service/RutasTest.java
package com.ordenador.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RutasTest {

    @Test
    void configSinEnvUsaHomeConfig() {
        Path home = Path.of("/home/f");
        Path resultado = Rutas.config(Map.of(), home);
        assertEquals("/home/f/.config/ordenador/reglas.json", resultado.toString());
    }

    @Test
    void configConXdgConfigHome() {
        Path home = Path.of("/home/f");
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", "/x");
        Path resultado = Rutas.config(env, home);
        assertEquals("/x/ordenador/reglas.json", resultado.toString());
    }

    @Test
    void registroSinEnvUsaHomeLocalShare() {
        Path home = Path.of("/home/f");
        Map<String, String> env = Map.of("XDG_DATA_HOME", "");
        Path resultado = Rutas.registro(env, home);
        assertEquals("/home/f/.local/share/ordenador/movimientos.log", resultado.toString());
    }

    @Test
    void expandirTilde() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("~/Imágenes", home);
        assertEquals("/home/usuario/Imágenes", resultado.toString());
    }

    @Test
    void expandirTildeSolo() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("~", home);
        assertEquals("/home/usuario", resultado.toString());
    }

    @Test
    void expandirAbsoluta() {
        Path home = Path.of("/home/usuario");
        Path resultado = Rutas.expandir("/tmp/a", home);
        assertEquals("/tmp/a", resultado.toString());
    }

    @Test
    void expandirRelativaLanzaExcepcion() {
        Path home = Path.of("/home/usuario");
        assertThrows(IllegalArgumentException.class, () -> Rutas.expandir("Descargas", home));
    }
}
```

```java
// src/test/java/com/ordenador/service/RegistroTest.java
package com.ordenador.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegistroTest {

    @TempDir
    Path tempBase;

    @Test
    void leerArchivoNoExisteDevuelveVacio() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);
        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void anotarYLeerDevuelveMovimientosEnOrden() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        // Crear movimientos de prueba
        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now().minusDays(2), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        var m2 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                Path.of("/origen/dos.txt"), Path.of("/destino/dos.txt"));

        registro.anotar(m1);
        registro.anotar(m2);

        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertEquals(2, movimientos.size());
        // El más viejo primero
        assertEquals("/origen/uno.txt", movimientos.get(0).origen().toString());
        assertEquals("/destino/uno.txt", movimientos.get(0).destino().toString());
        assertEquals("/origen/dos.txt", movimientos.get(1).origen().toString());
    }

    @Test
    void deshacerUltimoRestauraArchivoYVaciaRegistro() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        // Crear un movimiento de prueba
        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now().minusDays(2), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        registro.anotar(m1);

        // Simular que el archivo destino existe (creamos un archivo temporal para probar)
        Path destino = m1.destino();
        Files.createFile(destino);

        // Deshacer
        var resultado = registro.deshacerUltimo();
        assertTrue(resultado.isPresent());

        // Verificar que el archivo origen ahora existe (o su variante si había conflicto)
        assertTrue(Files.exists(m1.origen()));
        // Verificar que el destino ya no existe
        assertFalse(Files.exists(destino));
        
        // Verificar que el registro está vacío
        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void deshacerUltimoSinRegistroDevuelveOptionalEmpty() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);
        Optional<com.ordenador.model.Movimiento> resultado = registro.deshacerUltimo();
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deshacerUltimoDestinoNoExisteLanzaIOException() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        registro.anotar(m1);

        // No creamos el archivo destino, así que debería fallar
        assertThrows(IOException.class, () -> registro.deshacerUltimo());
    }

    @Test
    void deshacerUltimoConConflictoDeNombreUsaNombresNumerados() throws IOException {
        Path carpetaOrigen = tempBase.resolve("origen");
        Files.createDirectories(carpetaOrigen);
        
        Path archivoDestino = tempBase.resolve("/destino/uno.txt");
        Files.createFile(archivoDestino);

        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                carpetaOrigen, archivoDestino);
        Registro registro = new Registro(tempBase.resolve("registro.log"));
        registro.anotar(m1);

        // Crear un archivo con el mismo nombre en la carpeta origen para simular conflicto
        Path archivoOrigenExistente = carpetaOrigen.resolve("uno.txt");
        Files.createFile(archivoOrigenExistente);

        var resultado = registro.deshacerUltimo();
        assertTrue(resultado.isPresent());

        // El archivo debería haberse movido como "uno (2).txt" o similar
        Path esperado = carpetaOrigen.resolve("uno (2).txt");
        assertTrue(Files.exists(esperado));
        assertFalse(Files.exists(archivoDestino));
    }
}
```

<!-- meta: {"segundos": 548.1, "finish_reason": "stop", "usage": {"prompt_tokens": 2321, "completion_tokens": 3576, "total_tokens": 5897, "completion_tokens_details": {"reasoning_tokens": 0}}} -->
