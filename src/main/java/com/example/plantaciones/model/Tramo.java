package com.example.plantaciones.model;

import java.time.LocalDate;

public record Tramo(long id, long filaId, int orden, long variedadId, int numArboles,
                    LocalDate fechaPlantacion, LocalDate fechaRecogida) { }