package com.ordenador.cli;

import com.ordenador.service.Notificador;

import java.io.IOException;

/** Notificación de escritorio (KDE) con notify-send. Si falla, lo escribe en System.err y sigue. */
public final class NotifySend implements Notificador {

    @Override
    public void notificar(String titulo, String cuerpo) {
        try { new ProcessBuilder("notify-send", "-a", "Ordenador", "-i", "folder-downloads", titulo, cuerpo).start(); } catch (IOException e) { System.err.println("No se pudo notificar: " + e.getMessage()); }
    }
}

