<!-- modelo qwen/qwen3.5-9b, temperatura 0.2, tokens de entrada 2730, max_tokens 5430, razonamiento no -->

## system

Sos un programador Java 21 cuidadoso. Te paso archivos con métodos marcados `// TODO`: completá solo los cuerpos que nombra la tarea; los `// TODO` que la tarea no nombra quedan exactamente como están.
Reglas:
- No cambies package, imports que ya están, nombres de clases, firmas ni métodos que ya están hechos. Podés agregar imports y métodos private.
- Los records se leen con sus métodos de acceso: m.origen(), regla.extensiones(). No existen getOrigen() ni getExtensiones().
- No crees ni modifiques otros archivos.
- Devolvé cada archivo completo (nada de '...') en su propio bloque de código, con la ruta en la primera línea. Ejemplo:
```java
// src/main/java/com/ordenador/service/Ejemplo.java
package com.ordenador.service;

public final class Ejemplo { }
```
- Sin explicaciones fuera de los bloques.

## user

# Contrato del proyecto (extracto)

# ORDENADOR — Especificación (contrato)

## 1. Reglas de código
- Java 21, Maven. Paquete base `com.ordenador`: `model` (datos), `service` (lógica), `cli` (comandos).
- Única dependencia: Gson 2.10.1. Tests con JUnit 5 (`org.junit.jupiter.api.*`). No toques `pom.xml`.
- Nombres y comentarios en español.
- Nada de rutas relativas ni `user.dir`: cada clase recibe las rutas por parámetro (así se testea con `@TempDir`).
- Nunca `Files.delete` sobre archivos del usuario.

## 2. Modelo (`com.ordenador.model`)
```java
public enum Accion { MOVER, AVISAR }

public record Regla(String nombre, Accion accion, List<String> extensiones, List<String> patrones,
                    List<String> palabras, String destino, String mensaje, int diasPapelera) { }

public record Config(String carpeta, int segundosQuieto, List<Regla> reglas) { }

public record Movimiento(java.time.LocalDateTime fecha, java.nio.file.Path origen, java.nio.file.Path destino) { }
```
En `Regla`, una lista `null` se toma como lista vacía.

## 4. Acciones
- `MOVER`: `destino` es una carpeta (`~` = carpeta personal); se crea si no existe. Si ya hay un archivo con ese nombre, no se pisa: `nombre (2).ext`, `nombre (3).ext`… Se anota en el registro y se notifica `"<nombre> → <ruta destino>"`. Si `diasPapelera > 0`, al llegar se le pone fecha de modificación = ahora; cuando pasan esos días ahí, va a la papelera.
- `AVISAR`: no mueve; notifica `mensaje` con `{palabra}` reemplazado por la palabra de la lista que coincidió (tal como está escrita en la lista).
- Sin regla: el archivo queda donde está.
- **Simular**: no se mueve, no se anota ni se notifica nada; solo se informa qué se haría.

---

# T05a — Ordenador: decidir y avisar

## Objetivo
Completar **solo** `procesar` y `avisar` de `Ordenador`, y los tests `avisarNoMueve` y `temporalIgnoradoYSinRegla`. `mover` y `ordenarCarpeta` quedan con su `// TODO`.

## Código que ya existe (no lo cambies)
```java
package com.ordenador.service;
public final class Nombres {
    public static boolean esTemporal(String nombre);              // .part, .crdownload, .tmp o empieza con "."
    public static Path nombreLibre(Path carpeta, String nombre);  // carpeta/nombre, o "x (2).pdf"… si ya existe; devuelve Path
}
public record Coincidencia(Regla regla, String palabra) { }       // palabra: la que coincidió, o null
public final class Clasificador {
    public static Optional<Coincidencia> clasificar(String nombre, List<Regla> reglas);
}
public final class Rutas {
    public static Path expandir(String ruta, Path home);           // "~/x" → home/x
}
public final class Registro {
    public void anotar(Movimiento m) throws IOException;
    public List<Movimiento> leer() throws IOException;
}
```
Los records se leen así: `c.regla()`, `c.palabra()`, `regla.accion()`, `regla.destino()`, `regla.mensaje()`, `regla.diasPapelera()`, `config.carpeta()`, `config.reglas()`.

## `procesar(archivo)`, en este orden
1. `String nombre = archivo.getFileName().toString();`
2. `if (!Files.isRegularFile(archivo) || Nombres.esTemporal(nombre)) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);`
3. `Optional<Coincidencia> c = Clasificador.clasificar(nombre, config.reglas());`
4. `if (c.isEmpty()) return new Resultado(archivo, Resultado.Estado.SIN_REGLA, null, null);`
5. `if (c.get().regla().accion() == Accion.AVISAR) return avisar(archivo, c.get());`
6. `return mover(archivo, c.get());`

## `avisar(archivo, c)`
1. `String palabra = c.palabra() == null ? "" : c.palabra();`
2. `String mensaje = c.regla().mensaje().replace("{palabra}", palabra);`
3. `if (!simular) notificador.notificar("Ordenador", archivo.getFileName() + ": " + mensaje);`
4. `return new Resultado(archivo, Resultado.Estado.AVISADO, null, mensaje);`

## Tests (usá `preparar()`, `ordenador(false)` y la lista `avisos` que ya están en el test)
- `avisarNoMueve`: crear `descargas/Fisica_TP1.pdf` con `Files.writeString`; `procesar` → estado `AVISADO`, mensaje `"Subilo a FISICA/teoria"`, `avisos` = `List.of("Fisica_TP1.pdf: Subilo a FISICA/teoria")` y el archivo sigue existiendo.
- `temporalIgnoradoYSinRegla`: `video.mp4.part` → `IGNORADO`; `nada.xyz` → `SIN_REGLA`; los dos siguen en `descargas` y `avisos` queda vacía.

---

# Archivos actuales

```java
// src/main/java/com/ordenador/service/Ordenador.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Movimiento;
import com.ordenador.model.Regla;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Aplica a cada archivo la acción de su regla (sección 4 de la especificación). */
public final class Ordenador {

    private final Config config;
    private final Path home;
    private final Registro registro;
    private final Notificador notificador;
    private final boolean simular;

    public Ordenador(Config config, Path home, Registro registro, Notificador notificador, boolean simular) {
        this.config = config;
        this.home = home;
        this.registro = registro;
        this.notificador = notificador;
        this.simular = simular;
    }

    /**
     * Decide qué hacer con el archivo:
     * no es archivo regular o es temporal → IGNORADO; sin regla → SIN_REGLA;
     * regla AVISAR → avisar(...); regla MOVER → mover(...).
     */
    public Resultado procesar(Path archivo) throws IOException {
        // TODO T05a
        throw new UnsupportedOperationException();
    }

    /** Regla AVISAR: no mueve; notifica (si no se simula) y devuelve AVISADO con el mensaje. */
    private Resultado avisar(Path archivo, Coincidencia c) {
        // TODO T05a
        throw new UnsupportedOperationException();
    }

    /** Regla MOVER: mueve sin pisar, anota y notifica; simulando, solo devuelve lo que haría. */
    private Resultado mover(Path archivo, Coincidencia c) throws IOException {
        // TODO T05b
        throw new UnsupportedOperationException();
    }

    /**
     * procesar() de cada archivo regular de primer nivel de Rutas.expandir(config.carpeta(), home),
     * ordenados por nombre; las subcarpetas no se tocan. Lista vacía si la carpeta no existe.
     */
    public List<Resultado> ordenarCarpeta() throws IOException {
        // TODO T05c
        throw new UnsupportedOperationException();
    }
}
```

```java
// src/test/java/com/ordenador/service/OrdenadorTest.java
package com.ordenador.service;

import com.ordenador.model.Accion;
import com.ordenador.model.Config;
import com.ordenador.model.Regla;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrdenadorTest {

    @TempDir Path home;
    Path descargas;
    Registro registro;
    List<String> avisos;

    /** Imágenes → ~/Imágenes; PDF con "FISICA" → aviso. */
    static final List<Regla> REGLAS = List.of(
            new Regla("img", Accion.MOVER, List.of("png", "jpg"), null, null, "~/Imágenes", null, 0),
            new Regla("fac", Accion.AVISAR, List.of("pdf"), null, List.of("FISICA"), null, "Subilo a {palabra}/teoria", 0));

    @BeforeEach
    void preparar() throws Exception {
        descargas = Files.createDirectories(home.resolve("Descargas"));
        registro = new Registro(home.resolve("log.txt"));
        avisos = new ArrayList<>();
    }

    /** Un Ordenador sobre home/Descargas con REGLAS y un notificador falso que guarda los cuerpos en avisos. */
    Ordenador ordenador(boolean simular) {
        return new Ordenador(new Config("~/Descargas", 5, REGLAS), home, registro, (t, c) -> avisos.add(c), simular);
    }

    @Test
    void avisarNoMueve() throws Exception {
        // TODO T05a
    }

    @Test
    void temporalIgnoradoYSinRegla() throws Exception {
        // TODO T05a
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        // TODO T05b
    }

    @Test
    void noPisa() throws Exception {
        // TODO T05b
    }

    @Test
    void simularNoTocaNada() throws Exception {
        // TODO T05b
    }

    @Test
    void ordenarCarpeta() throws Exception {
        // TODO T05c
    }
}
```

Completá los `// TODO` que pide la tarea T05a. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Ordenador.java, src/test/java/com/ordenador/service/OrdenadorTest.java.
