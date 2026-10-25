The user is pointing out that my previous implementation failed because of a `NoSuchFileException` when trying to write to the file. The issue is in the `anotar` method - it's trying to use `StandardOpenOption.CREATE` and `APPEND`, but there's a logic error in how I'm handling the parent directory creation.

Looking at the error:
```
java.nio.file.NoSuchFileException: /tmp/junit6178859439857945002/datos/movimientos.log
    at com.ordenador.service.Registro.anotar(Registro.java:34)   ← línea: Files.writeString(archivo, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
```

The problem is that I'm checking `if (m.origen().getParent() != null)` but the error shows it's failing on the file write itself. Looking at the contract rules again:

> `anotar`: si `archivo.getParent()` no es null, `Files.createDirectories(archivo.getParent())`. Después `Files.writeString(archivo, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)`

So I need to create directories for the **archivo** (the log file path), not for `m.origen()`. The contract says:
- `anotar`: si `archivo.getParent()` no es null, `Files.createDirectories(archivo.getParent())`

But in my current code, I'm doing:
```java
if (m.origen().getParent() != null) {
    Files.createDirectories(m.origen().getParent());
}
```

This is wrong! I should be checking and creating directories for the `archivo` parameter, not `m.origen()`.

Let me fix this according to the contract specification.

Also, looking at the test:
```java
Registro r = new Registro(d.resolve("datos/movimientos.log"));


```

The path is `d.resolve("datos/movimientos.log")`, so I need to ensure the parent directory (`datos/`) exists before writing. The fix is straightforward - check and create directories for the archivo's parent, not the movimiento's origen.
