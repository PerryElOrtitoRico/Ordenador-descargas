package com.ordenador.service;

import com.ordenador.model.Regla;

/** La regla elegida y la palabra de regla.palabras() que coincidió (null si la regla no tiene palabras). */
public record Coincidencia(Regla regla, String palabra) { }
