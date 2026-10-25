package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Movimiento;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Aplica a cada archivo la acción de su regla (sección 4 de la especificación). */
public final class Ordenador {

    private final Config config;
    private final Path home;
    private final Registro registro;
    private final Notificador notificador;
    private final boolean simular;

    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular) {
        this.config = config;
        this.home = home;
        this.registro = registro;
        this.notificador = notificador;
        this.simular = simular;
    }

    /**
     * Decide qué hacer con el archivo:
     * no es archivo regular o es temporal → IGNORADO; sin regla → SIN_REGLA;
     * regla AVISAR → avisar(...); regla MOVER → mover(...).
     */
    public Resultado procesar(Path archivo) throws IOException {
        String nombre = archivo.getFileName().toString();
        if (!Files.isRegularFile(archivo) || Nombres.esTemporal(nombre)) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Optional<Coincidencia> c = Clasificador.clasificar(nombre, config.reglas());
        if (c.isEmpty()) return new Resultado(archivo, Resultado.Estado.SIN_REGLA, null, null);
        if (c.get().regla().accion() == Accion.AVISAR) return avisar(archivo, c.get());
        return mover(archivo, c.get());
    }

    /** Regla AVISAR: no mueve; notifica (si no se simula) y devuelve AVISADO con el mensaje. */
    private Resultado avisar(Path archivo, Coincidencia c) {
        String palabra = c.palabra() == null ? "" : c.palabra();
        String mensaje = c.regla().mensaje().replace("{palabra}", palabra);
        if (!simular) notificador.notificar("Ordenador", archivo.getFileName() + ": " + mensaje);
        return new Resultado(archivo, Resultado.Estado.AVISADO, null, mensaje);
    }

    /** Regla MOVER: mueve sin pisar, anota y notifica; simulando, solo devuelve lo que haría. */
    private Resultado mover(Path archivo, Coincidencia c) throws IOException {
        String nombre = archivo.getFileName().toString();
        Path carpeta = Rutas.expandir(c.regla().destino(), home);
        if (carpeta.equals(archivo.getParent())) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Path destino = Nombres.nombreLibre(carpeta, nombre);
        String mensaje = nombre + " → " + destino;
        if (simular) return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
        Files.createDirectories(carpeta);
        Files.move(archivo, destino);
        if (c.regla().diasPapelera() > 0) Files.setLastModifiedTime(destino, FileTime.from(Instant.now()));
        registro.anotar(new Movimiento(LocalDateTime.now(), archivo, destino));
        notificador.notificar("Ordenador", mensaje);
        return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
    }

    /**
     * procesar() de cada archivo regular de primer nivel de Rutas.expandir(config.carpeta(), home),
     * ordenados por nombre; las subcarpetas no se tocan. Lista vacía si la carpeta no existe.
     */
    public List<Resultado> ordenarCarpeta() throws IOException {
        // lo completa otra subtarea (T05c): no lo cambies
        throw new UnsupportedOperationException("T05c");
    }
}

