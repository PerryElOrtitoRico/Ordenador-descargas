<!-- contrato: 1,3 | archivos: src/main/java/com/ordenador/service/Vigilante.java, src/test/java/com/ordenador/service/VigilanteTest.java -->
# T07b — Vigilante: WatchService

## Objetivo
Completar `correr` y `descargaEnCurso` de `Vigilante` y los `// TODO` de sus dos tests. El constructor y `close()` ya están.

## Código que ya existe (no lo cambies)
```java
public final class Nombres {
    public static boolean esTemporal(String nombre);   // .part, .crdownload, .tmp o empieza con "."
}
public final class Pendientes {
    public void tocar(Path archivo, long ahoraMs);
    public List<Path> listos(long ahoraMs);   // los que no cambiaron hace quietoMs o más; los saca
}
```

## `descargaEnCurso(archivo)`
`String n = archivo.getFileName().toString(); return Files.exists(archivo.resolveSibling(n + ".part")) || Files.exists(archivo.resolveSibling(n + ".crdownload"));`

## `correr()`
```text
try {
    while (true) {
        WatchKey key = watchService.poll(200, TimeUnit.MILLISECONDS);
        if (key != null) {
            for (WatchEvent<?> ev : key.pollEvents()) {
                if (ev.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                Path ruta = carpeta.resolve((Path) ev.context());
                if (!Nombres.esTemporal(ruta.getFileName().toString())) pendientes.tocar(ruta, System.currentTimeMillis());
            }
            key.reset();
        }
        for (Path ruta : pendientes.listos(System.currentTimeMillis())) {
            if (descargaEnCurso(ruta)) pendientes.tocar(ruta, System.currentTimeMillis());   // sigue esperando
            else if (Files.isRegularFile(ruta)) alEstarListo.accept(ruta);
        }
    }
} catch (ClosedWatchServiceException e) {
    // se cerró con close(): terminar
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```
Escribilo en Java tal cual (es el cuerpo completo del método).

## Tests
- `descargaEnCurso`: con `x.pdf` solo → `false`; con `x.pdf` y `x.pdf.part` → `true`.
- `avisaUnaVezYNoPorTemporales`: completá el `// TODO` del test: `Files.writeString(d.resolve("b.txt.part"), "x")`, `Path a = Files.writeString(d.resolve("a.txt"), "hola")`; esperar en un bucle (`Thread.sleep(50)`, hasta 5 s) a que `listos` tenga 1 elemento; después `Thread.sleep(800)` y `assertEquals(List.of(a), new ArrayList<>(listos))`.
