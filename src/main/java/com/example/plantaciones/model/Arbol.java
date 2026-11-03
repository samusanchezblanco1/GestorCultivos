package com.example.plantaciones.model;

import java.time.LocalDate;

public record Arbol(long id, long tramoId, int posicion, EstadoArbol estado, LocalDate fechaPlantacion) { }