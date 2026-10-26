package com.ordenador.cli;

import com.ordenador.service.Notificador;

import java.io.IOException;

/** Notificación de escritorio (KDE) con notify-send. Si falla, lo escribe en System.err y sigue. */
public final class NotifySend implements Notificador {

    @Override
    public void notificar(String titulo, String cuerpo) {
        // TODO T08c
        throw new UnsupportedOperationException();
    }
}
