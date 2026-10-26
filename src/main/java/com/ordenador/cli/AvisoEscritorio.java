package com.ordenador.cli;

import com.ordenador.service.Notificador;

import java.io.IOException;

/**
 * Notificación de escritorio: kdialog (viene con KDE) y, si no está, notify-send.
 * Si ninguno anda, lo escribe en System.err y sigue.
 */
public final class AvisoEscritorio implements Notificador {

    @Override
    public void notificar(String titulo, String cuerpo) {
        try {
            new ProcessBuilder("kdialog", "--title", titulo, "--icon", "folder-downloads", "--passivepopup", cuerpo, "15").start();
        } catch (IOException sinKdialog) {
            try {
                new ProcessBuilder("notify-send", "-a", "Ordenador", "-i", "folder-downloads", titulo, cuerpo).start();
            } catch (IOException e) {
                System.err.println("No se pudo notificar: " + e.getMessage());
            }
        }
    }
}
