package com.ordenador.model;

import java.util.ArrayList;
import java.util.List;

public final class Config {
    private String carpeta;
    private int segundosQuieto;
    private List<Regla> reglas;

    public Config() {
        this.carpeta = null;
        this.segundosQuieto = 0;
        this.reglas = new ArrayList<>();
    }

    public Config(String carpeta, int segundosQuieto, List<Regla> reglas) {
        this.carpeta = carpeta;
        this.segundosQuieto = segundosQuieto;
        this.reglas = reglas;
    }

    public String carpeta() {
        return carpeta;
    }

    public int segundosQuieto() {
        return segundosQuieto;
    }

    public List<Regla> getReglas() {
        return reglas;
    }

    // Método estático requerido por ConfigRepo para crear una instancia vacía inicial
    public static Config crear() {
        return new Config();
    }
}
