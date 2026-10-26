package com.ordenador.cli;

import com.ordenador.service.Notificador;
import com.ordenador.service.Papelera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Test oculto de T08a: no se le muestra al LLM. */
class T08aArgumentosOcultoTest {

    @Test
    void parsearValidos() {
        assertEquals(new Argumentos("ordenar", true), Argumentos.parsear(new String[]{"ordenar", "--simular"}));
        assertEquals(new Argumentos("ordenar", true), Argumentos.parsear(new String[]{"--simular", "ordenar"}));
        assertEquals(new Argumentos("vigilar", false), Argumentos.parsear(new String[]{"vigilar"}));
        assertEquals(new Argumentos("limpiar", true), Argumentos.parsear(new String[]{"limpiar", "--simular"}));
        assertEquals(new Argumentos("deshacer", false), Argumentos.parsear(new String[]{"deshacer"}));
        assertEquals(new Argumentos("resumen", false), Argumentos.parsear(new String[]{"resumen"}));
        assertEquals("ayuda", Argumentos.parsear(new String[]{"-h"}).comando());
        assertEquals("ayuda", Argumentos.parsear(new String[]{"--help"}).comando());
        assertEquals("ayuda", Argumentos.parsear(new String[]{"ayuda"}).comando());
    }

    @Test
    void parsearInvalidos() {
        String[][] malos = {{}, {"borrar"}, {"ordenar", "resumen"}, {"deshacer", "--simular"}, {"resumen", "--simular"},
                {"ordenar", "--rapido"}, {"--simular"}};
        for (String[] m : malos) {
            assertThrows(IllegalArgumentException.class, () -> Argumentos.parsear(m), String.join(" ", m));
        }
    }
}
