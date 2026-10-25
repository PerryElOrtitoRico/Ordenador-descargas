package com.ordenador.service;

/** Quien muestra los avisos al usuario (en el programa, notify-send; en los tests, uno falso). */
public interface Notificador {
    void notificar(String titulo, String cuerpo);
}
