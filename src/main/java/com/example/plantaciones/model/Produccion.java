package com.example.plantaciones.model;

public record Produccion(long id, long tramoId, int anio, Double kilosEstimados, Double kilosReales) { }