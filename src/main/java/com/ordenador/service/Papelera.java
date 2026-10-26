package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Manda a la papelera lo que lleva diasPapelera días en la carpeta de su regla. Nunca borra. */
public final class Papelera {

    private Papelera() { }

    /** Quien manda un archivo a la papelera. En los tests se usa uno falso que solo anota. */
    public interface Tacho {
        void tirar(Path archivo) throws IOException;
    }

    /** Ejecuta: gio trash <ruta absoluta>. Si el código de salida no es 0 → IOException con la ruta. */
    public static final Tacho GIO = archivo -> {
        // TODO T06a
        throw new UnsupportedOperationException();
    };

    /**
     * Archivos regulares de primer nivel de la carpeta cuya fecha de modificación es anterior o igual
     * a ahora − dias. Ordenados por nombre. Lista vacía si dias <= 0 o la carpeta no existe.
     */
    public static List<Path> vencidos(Path carpeta, int dias, Instant ahora) throws IOException {
        // TODO T06a
        throw new UnsupportedOperationException();
    }

    /**
     * Para cada regla MOVER con diasPapelera > 0: vencidos(Rutas.expandir(destino, home), diasPapelera, ahora).
     * Si no se simula, tacho.tirar(...) de cada uno. Devuelve todos los vencidos encontrados, en orden de regla.
     */
    public static List<Path> limpiar(Config config, Path home, Instant ahora, Tacho tacho, boolean simular) throws IOException {
        // TODO T06a
        throw new UnsupportedOperationException();
    }
}
