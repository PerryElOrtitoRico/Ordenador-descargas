<!-- modelo google/gemma-4-e4b-qat, temperatura 0.2, tokens de entrada 3513, max_tokens 4647, razonamiento no -->

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

# T05c — Ordenador: ordenar toda la carpeta

## Objetivo
Completar **solo** `ordenarCarpeta` de `Ordenador` (lo demás ya está: no lo cambies) y el test `ordenarCarpeta`.

## Código que ya existe (no lo cambies)
```java
public final class Rutas {
    public static Path expandir(String ruta, Path home);   // "~/x" → home/x
}
```
Los records se leen así: `config.carpeta()`.

## `ordenarCarpeta()`, en este orden
1. `Path carpeta = Rutas.expandir(config.carpeta(), home);`
2. `if (!Files.isDirectory(carpeta)) return new ArrayList<>();`
3. Juntar **primero** la lista de archivos (antes de mover nada):
   `try (Stream<Path> s = Files.list(carpeta)) { archivos = s.filter(Files::isRegularFile).sorted().toList(); }`
4. Por cada uno, en ese orden: `resultados.add(procesar(p));`
5. `return resultados;`

## Test (usá `preparar()`, `ordenador(false)` y `descargas`)
- `ordenarCarpeta`: crear `descargas/b.png`, `descargas/a.xyz` y la subcarpeta `descargas/sub` con `sub/c.png` adentro.
  `ordenarCarpeta()` devuelve 2 resultados: primero `a.xyz` (`SIN_REGLA`), después `b.png` (`MOVIDO`).
  `sub/c.png` sigue donde estaba.

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
        String nombre = archivo.getFileName().toString();
        if (!Files.isRegularFile(archivo) || Nombres.esTemporal(nombre)) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Optional<Coincidencia> c = Clasificador.clasificar(nombre, config.reglas());
        if (c.isEmpty()) return new Resultado(archivo, Resultado.Estado.SIN_REGLA, null, null);
        if (c.get().regla().accion() == Accion.AVISAR) return avisar(archivo, c.get());
        return mover(archivo, c.get());
    }

    /** Regla AVISAR: no mueve; notifica (si no se simula) y devuelve AVISADO con el mensaje. */
    private Resultado avisar(Path archivo, Coincidencia c) {
        String palabra = c.palabra() == null ? "" : c.palabra();
        String mensaje = c.regla().mensaje().replace("{palabra}", palabra);
        if (!simular) notificador.notificar("Ordenador", archivo.getFileName() + ": " + mensaje);
        return new Resultado(archivo, Resultado.Estado.AVISADO, null, mensaje);
    }

    /** Regla MOVER: mueve sin pisar, anota y notifica; simulando, solo devuelve lo que haría. */
    private Resultado mover(Path archivo, Coincidencia c) throws IOException {
        String nombre = archivo.getFileName().toString();
        Path carpeta = Rutas.expandir(c.regla().destino(), home);
        if (carpeta.equals(archivo.getParent())) return new Resultado(archivo, Resultado.Estado.IGNORADO, null, null);
        Path destino = Nombres.nombreLibre(carpeta, nombre);
        String mensaje = nombre + " → " + destino;
        if (simular) return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
        Files.createDirectories(carpeta);
        Files.move(archivo, destino);
        if (c.regla().diasPapelera() > 0) Files.setLastModifiedTime(destino, FileTime.from(Instant.now()));
        registro.anotar(new Movimiento(LocalDateTime.now(), archivo, destino));
        notificador.notificar("Ordenador", mensaje);
        return new Resultado(archivo, Resultado.Estado.MOVIDO, destino, mensaje);
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
        Path archivo = descargas.resolve("Fisica_TP1.pdf");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.AVISADO, r.estado());
        assertEquals("Subilo a FISICA/teoria", r.mensaje());
        assertTrue(Files.exists(archivo));
        assertEquals(List.of("Fisica_TP1.pdf: Subilo a FISICA/teoria"), avisos);
    }

    @Test
    void temporalIgnoradoYSinRegla() throws Exception {
        Path part = Files.writeString(descargas.resolve("video.mp4.part"), "x");
        Path nada = Files.writeString(descargas.resolve("nada.xyz"), "x");
        assertEquals(Resultado.Estado.IGNORADO, ordenador(false).procesar(part).estado());
        assertEquals(Resultado.Estado.SIN_REGLA, ordenador(false).procesar(nada).estado());
        assertTrue(Files.exists(part));
        assertTrue(Files.exists(nada));
        assertEquals(List.of(), avisos);
    }

    @Test
    void mueveAnotaYNotifica() throws Exception {
        Path archivo = descargas.resolve("foto.PNG");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.PNG → " + home.resolve("Imágenes/foto.PNG"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.PNG")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto.PNG")));
        assertEquals(1, avisos.size());
        assertEquals(1, registro.leer().size());
    }

    @Test
    void noPisa() throws Exception {
        Path destino = home.resolve("Imágenes");
        Files.createDirectories(destino);
        Path archivoViejo = destino.resolve("foto.png");
        Files.writeString(archivoViejo, "viejo");
        
        Path archivo = descargas.resolve("foto.png");
        Files.writeString(archivo, "nuevo");
        
        Resultado r = ordenador(false).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("foto.png → " + home.resolve("Imágenes/foto (2).png"), r.mensaje());
        assertFalse(Files.exists(descargas.resolve("foto.png")));
        assertTrue(Files.exists(home.resolve("Imágenes").resolve("foto (2).png")));
        assertEquals("viejo", Files.readString(archivoViejo));
    }

    @Test
    void simularNoTocaNada() throws Exception {
        Path archivo = descargas.resolve("a.jpg");
        Files.writeString(archivo, "contenido");
        
        Resultado r = ordenador(true).procesar(archivo);
        
        assertEquals(Resultado.Estado.MOVIDO, r.estado());
        assertEquals("a.jpg → " + home.resolve("Imágenes/a.jpg"), r.mensaje());
        assertTrue(Files.exists(descargas.resolve("a.jpg")));
        assertFalse(Files.exists(home.resolve("Imágenes")));
        assertEquals(0, avisos.size());
        assertEquals(0, registro.leer().size());
    }

    @Test
    void ordenarCarpeta() throws Exception {
        // TODO T05c
    }
}
```

Completá los `// TODO` que pide la tarea T05c. Devolvé estos archivos completos, cada uno en su bloque: src/main/java/com/ordenador/service/Ordenador.java, src/test/java/com/ordenador/service/OrdenadorTest.java.
