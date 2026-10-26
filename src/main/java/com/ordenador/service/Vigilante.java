package com.ordenador.service;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Vigila una carpeta (sin subcarpetas) y avisa cuando un archivo lleva quietoMs sin cambiar. */
public final class Vigilante implements AutoCloseable {

    private final Path carpeta;
    private final Pendientes pendientes;
    private final Consumer<Path> alEstarListo;
    private final WatchService watchService;

    /** Abre un WatchService y registra la carpeta para ENTRY_CREATE y ENTRY_MODIFY. */
    public Vigilante(Path carpeta, long quietoMs, Consumer<Path> alEstarListo) throws IOException {
        this.carpeta = carpeta;
        this.pendientes = new Pendientes(quietoMs);
        this.alEstarListo = alEstarListo;
        this.watchService = FileSystems.getDefault().newWatchService();
        carpeta.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /** Bucle hasta close() o interrupción del hilo. */
    public void correr() {
        // TODO T07b
        throw new UnsupportedOperationException();
    }

    /** true si existe al lado "<nombre>.part" o "<nombre>.crdownload" (Firefox crea el archivo final vacío mientras baja el .part). */
    public static boolean descargaEnCurso(Path archivo) {
        // TODO T07b
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }
}
