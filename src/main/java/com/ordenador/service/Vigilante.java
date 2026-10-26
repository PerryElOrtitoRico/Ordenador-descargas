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
        try {
            while (true) {
                WatchKey key = watchService.poll(200, TimeUnit.MILLISECONDS);
                if (key != null) {
                    for (WatchEvent<?> ev : key.pollEvents()) {
                        if (ev.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        Path ruta = carpeta.resolve((Path) ev.context());
                        if (!Nombres.esTemporal(ruta.getFileName().toString())) pendientes.tocar(ruta, System.currentTimeMillis());
                    }
                    key.reset();
                }
                for (Path ruta : pendientes.listos(System.currentTimeMillis())) {
                    if (descargaEnCurso(ruta)) pendientes.tocar(ruta, System.currentTimeMillis());   // sigue esperando
                    else if (Files.isRegularFile(ruta)) alEstarListo.accept(ruta);
                }
            }
        } catch (ClosedWatchServiceException e) {
            // se cerró con close(): terminar
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** true si existe al lado "<nombre>.part" o "<nombre>.crdownload" (Firefox crea el archivo final vacío mientras baja el .part). */
    public static boolean descargaEnCurso(Path archivo) {
        String n = archivo.getFileName().toString();
        return Files.exists(archivo.resolveSibling(n + ".part")) || Files.exists(archivo.resolveSibling(n + ".crdownload"));
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }
}
