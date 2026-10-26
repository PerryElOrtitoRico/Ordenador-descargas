package com.ordenador.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import com.ordenador.model.Accion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigRepo {
    private ConfigRepo() { }

    /** Si el archivo no existe: crea sus carpetas, guarda crear() y la devuelve. Si existe: leer(archivo). */
    public static Config cargarOCrear(Path archivo) throws IOException {
        if (!Files.exists(archivo)) {
            Config porDefecto = ReglasPorDefecto.crear();
            guardar(porDefecto, archivo);
            return porDefecto;
        }
        return leer(archivo);
    }

    /** Lee y valida. Lanza IOException (con la ruta del archivo en el mensaje) si no es JSON válido o no pasa la validación. */
    public static Config leer(Path archivo) throws IOException {
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        
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

        if (config == null || config.reglas() == null) {
            throw new IOException("Reglas null en " + archivo);
        }

        for (int i = 0; i < config.reglas().size(); i++) {
            Regla regla = config.reglas().get(i);
            
            if (regla.nombre() == null || regla.nombre().trim().isEmpty()) {
                throw new IOException("Regla " + i + " tiene nombre null o vacío en " + archivo);
            }

            if (regla.accion() == null) {
                throw new IOException("Regla " + i + " (" + regla.nombre() + ") tiene una acción inválida (MOVER o AVISAR) en " + archivo);
            }

            if (regla.accion() == Accion.MOVER && (regla.destino() == null || regla.destino().trim().isEmpty())) {
                throw new IOException("Regla " + i + " (" + regla.nombre() + ") es MOVER y no tiene destino en " + archivo);
            }

            if (regla.accion() == Accion.AVISAR && regla.mensaje() == null) {
                throw new IOException("Regla " + i + " (" + regla.nombre() + ") es AVISAR y no tiene mensaje en " + archivo);
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

        return new Config(carpeta, segundosQuieto, config.reglas());
    }

    /** Gson con setPrettyPrinting(), UTF-8. Atómico: escribir "<archivo>.tmp" y Files.move con REPLACE_EXISTING y ATOMIC_MOVE. */
    public static void guardar(Config config, Path archivo) throws IOException {
        String contenido = new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(config);

        if (archivo.getParent() != null) {
            Files.createDirectories(archivo.getParent());
        }
        Path tmpArchivo = archivo.resolveSibling(archivo.getFileName() + ".tmp");
        Files.writeString(tmpArchivo, contenido, StandardCharsets.UTF_8);
        
        Files.move(tmpArchivo, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }
}
