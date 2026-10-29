package com.example.plantaciones.model;

import java.time.LocalDate;

public record Fila(long id, long plantacionId, int numero, String variedad, int numArboles, LocalDate fechaRecogida) { }