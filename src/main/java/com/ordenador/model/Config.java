package com.ordenador.model;

import java.util.List;

/** Contenido de reglas.json: carpeta vigilada, segundos de espera y reglas en orden. */
public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }
