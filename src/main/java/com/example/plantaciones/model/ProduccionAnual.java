package com.example.plantaciones.model;

public record ProduccionAnual(long id, int anio, long variedadId, Long plantacionId,
                              Double kilosEstimados, Double kilosReales) { }