The user wants me to complete the `deshacerUltimo` method in `Registro.java` and implement the three test methods (`deshacerVuelveAlOrigen`, `deshacerNoPisa`, `deshacerSinRegistro`) in `RegistroTest.java`.

I need to follow the contract:
1. Java 21, Maven, package `com.ordenador`
2. Only complete what's marked with `// TODO` or specified in the task
3. Keep existing code structure intact
4. Use the provided steps for `deshacerUltimo` implementation
5. Implement tests according to the specifications

Let me implement:

1. **Registro.java - deshacerUltimo()**: Complete the method body following the 8 steps provided
2. **RegistroTest.java**: Implement the three test methods with their specific requirements

For `Nombres.nombreLibre`, I need to check if it exists in the codebase. Looking at the contract, there's a reference to `Nombres` class but I don't see its full implementation. Since the task says "No crees ni modifiques otros archivos", I should assume this class already exists or is provided elsewhere. However, looking at the current code, I see `Nombres.nombreLibre` is referenced in step 5 of deshacerUltimo.

Wait, looking more carefully at the contract section:
```java
public final class Nombres {
    /** carpeta.resolve(nombre) si está libre; si no, "base (2).ext", "base (3).ext"… Devuelve un Path. */
    public static Path nombreLibre(Path carpeta, String nombre);
}
```

This is marked as existing code that I shouldn't modify. So I should use it as-is.

Let me implement the deshacerUltimo method following the 8 steps:

1. `List<Movimiento> todos = leer();` - already done in current code
2. `if (todos.isEmpty()) return Optional.empty();` - already handled


3. `Movimiento m = todos.get(todos.size() - 1);` - get last movement
4. `if (!Files.exists(m.destino())) throw new IOException("ya no está: " + m.destino());` - check if destination exists
5. `Path carpeta = m.origen().getParent(); Files.createDirectories(carpeta);` - ensure origin folder exists
6. `Path adonde = Files.exists(m.origen()) ? Nombres.nombreLibre(carpeta, m.origen().getFileName().toString()) : m.origen();` - determine where to move the file
7. `Files.move(m.destino(), adonde);` - move the file
8. Reescribir el registro con todos menos el último usando archivo.tmp y Files.move

I'll complete the method by following these steps precisely, ensuring each step is implemented correctly according to the contract specifications.

For the tests, I need to:
- `deshacerVuelveAlOrigen`: Create a file at destination, annotate movement, then undo - verify file returns to origin location and log is empty
- `deshacerNoPisa`: Same setup but origin already has file - should create numbered version like "x (2).pdf"
- `deshacerSinRegistro`: Call deshacerUltimo without any prior movements - should return Optional.empty()

I'll implement each test to verify the undo functionality works correctly under different scenarios.
