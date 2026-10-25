package com.ordenador.model;

import java.util.List;

/** Configuración global del sistema de ordenamiento. */
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
