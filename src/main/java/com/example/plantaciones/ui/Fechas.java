package com.example.plantaciones.ui;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

public final class Fechas {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fechas() { }

    public static String texto(LocalDate fecha) {
        return fecha == null ? "sin fecha" : fecha.format(FORMATO);
    }

    /** Edad legible: "8 años", "1 año" o "5 meses". */
    public static String edad(LocalDate fecha) {
        if (fecha == null) {
            return "sin fecha";
        }
        Period periodo = Period.between(fecha, LocalDate.now());
        if (periodo.isNegative()) {
            return "fecha futura";
        }
        int anios = periodo.getYears();
        if (anios >= 1) {
            return anios == 1 ? "1 año" : anios + " años";
        }
        int meses = periodo.getMonths();
        return meses == 1 ? "1 mes" : meses + " meses";
    }
}