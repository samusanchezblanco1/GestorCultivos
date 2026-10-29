package com.example.plantaciones.db;

public class DatosException extends RuntimeException {
    public DatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}