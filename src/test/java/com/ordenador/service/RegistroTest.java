package com.ordenador.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RegistroTest {

    @TempDir
    Path tempBase;

    @Test
    void leerArchivoNoExisteDevuelveVacio() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);
        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void anotarYLeerDevuelveMovimientosEnOrden() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        // Crear movimientos de prueba
        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now().minusDays(2), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        var m2 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                Path.of("/origen/dos.txt"), Path.of("/destino/dos.txt"));

        registro.anotar(m1);
        registro.anotar(m2);

        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertEquals(2, movimientos.size());
        // El más viejo primero
        assertEquals("/origen/uno.txt", movimientos.get(0).origen().toString());
        assertEquals("/destino/uno.txt", movimientos.get(0).destino().toString());
        assertEquals("/origen/dos.txt", movimientos.get(1).origen().toString());
    }

    @Test
    void deshacerUltimoRestauraArchivoYVaciaRegistro() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        // Crear un movimiento de prueba
        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now().minusDays(2), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        registro.anotar(m1);

        // Simular que el archivo destino existe (creamos un archivo temporal para probar)
        Path destino = m1.destino();
        Files.createFile(destino);

        // Deshacer
        var resultado = registro.deshacerUltimo();
        assertTrue(resultado.isPresent());

        // Verificar que el archivo origen ahora existe (o su variante si había conflicto)
        assertTrue(Files.exists(m1.origen()));
        // Verificar que el destino ya no existe
        assertFalse(Files.exists(destino));
        
        // Verificar que el registro está vacío
        List<com.ordenador.model.Movimiento> movimientos = registro.leer();
        assertTrue(movimientos.isEmpty());
    }

    @Test
    void deshacerUltimoSinRegistroDevuelveOptionalEmpty() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);
        Optional<com.ordenador.model.Movimiento> resultado = registro.deshacerUltimo();
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deshacerUltimoDestinoNoExisteLanzaIOException() throws IOException {
        Path archivo = tempBase.resolve("registro.log");
        Registro registro = new Registro(archivo);

        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                Path.of("/origen/uno.txt"), Path.of("/destino/uno.txt"));
        registro.anotar(m1);

        // No creamos el archivo destino, así que debería fallar
        assertThrows(IOException.class, () -> registro.deshacerUltimo());
    }

    @Test
    void deshacerUltimoConConflictoDeNombreUsaNombresNumerados() throws IOException {
        Path carpetaOrigen = tempBase.resolve("origen");
        Files.createDirectories(carpetaOrigen);
        
        Path archivoDestino = tempBase.resolve("/destino/uno.txt");
        Files.createFile(archivoDestino);

        var m1 = new com.ordenador.model.Movimiento(java.time.LocalDateTime.now(), 
                carpetaOrigen, archivoDestino);
        Registro registro = new Registro(tempBase.resolve("registro.log"));
        registro.anotar(m1);

        // Crear un archivo con el mismo nombre en la carpeta origen para simular conflicto
        Path archivoOrigenExistente = carpetaOrigen.resolve("uno.txt");
        Files.createFile(archivoOrigenExistente);

        var resultado = registro.deshacerUltimo();
        assertTrue(resultado.isPresent());

        // El archivo debería haberse movido como "uno (2).txt" o similar
        Path esperado = carpetaOrigen.resolve("uno (2).txt");
        assertTrue(Files.exists(esperado));
        assertFalse(Files.exists(archivoDestino));
    }
}
