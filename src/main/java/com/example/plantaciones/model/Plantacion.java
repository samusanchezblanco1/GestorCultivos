package com.example.plantaciones.model;

import java.time.LocalDate;

public record Plantacion(long id, String nombre, String descripcion, String rutaFoto, double hectareas,
                         LocalDate fechaPlantacion, double distanciaArboles, double distanciaBorde) {

    /** Constructor corto: usa los valores por defecto (3 m entre árboles, sin fecha, sin borde). */
    public Plantacion(long id, String nombre, String descripcion, String rutaFoto, double hectareas) {
        this(id, nombre, descripcion, rutaFoto, hectareas, null, 3.0, 0.0);
    }

    public Plantacion conNombre(String nuevo) {
        return new Plantacion(id, nuevo, descripcion, rutaFoto, hectareas,
                fechaPlantacion, distanciaArboles, distanciaBorde);
    }

    public Plantacion conDescripcion(String nueva) {
        return new Plantacion(id, nombre, nueva, rutaFoto, hectareas,
                fechaPlantacion, distanciaArboles, distanciaBorde);
    }

    public Plantacion conFoto(String nueva) {
        return new Plantacion(id, nombre, descripcion, nueva, hectareas,
                fechaPlantacion, distanciaArboles, distanciaBorde);
    }
}