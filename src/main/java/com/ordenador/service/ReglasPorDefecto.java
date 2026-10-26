package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;

import java.util.List;

public final class ReglasPorDefecto {
    private ReglasPorDefecto() { }

    /** Config("~/Descargas", 5, <las 5 reglas de la tabla, en ese orden>). */
    public static Config crear() {
        List<Regla> reglas = List.of(
            new Regla("instaladores", Accion.MOVER, List.of("deb", "AppImage", "exe"), null, null, "~/Descargas/Instaladores", null, 30),
            new Regla("rutinas", Accion.MOVER, List.of("json", "pdf"), List.of("rutina-*", "plan-*"), null, "~/Documentos/Carpeta personal/Gimnasio", null, 0),
            new Regla("facultad", Accion.AVISAR, List.of("pdf"), null, List.of("CALCULO NUMERIC", "INTRODUCCION A LA INGENIERIA", "MECANICA DE FLUIDOS", "TECNOLOGIA MECANICA"), null, "Subilo a Drive, carpeta {palabra}/teoria", 0),
            new Regla("finanzas", Accion.MOVER, List.of("pdf", "csv", "xls", "xlsx"), null, List.of("mercado pago", "mercadopago", "account statement", "resumen de cuenta", "estado de cuenta", "extracto"), "~/Documentos/Carpeta personal/Finanzas/entrada", null, 0),
            new Regla("imagenes", Accion.MOVER, List.of("png", "jpg", "jpeg", "webp", "gif"), null, null, "~/Imágenes", null, 0)
        );

        return new Config("~/Descargas", 5, reglas);
    }
}
