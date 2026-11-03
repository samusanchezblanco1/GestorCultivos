package com.example.plantaciones.model;

import java.time.LocalDate;
import java.time.MonthDay;

public record Variedad(long id, String nombre, String color, MonthDay inicio, MonthDay fin) {

    /** Dónde está hoy la variedad respecto a su época de maduración. */
    public EstadoMaduracion estado(LocalDate hoy) {
        if (inicio == null || fin == null) {
            return EstadoMaduracion.SIN_DATOS;
        }
        MonthDay dia = MonthDay.from(hoy);
        if (dia.isBefore(inicio)) {
            return EstadoMaduracion.AUN_NO;
        }
        if (dia.isAfter(fin)) {
            return EstadoMaduracion.FUERA_DE_PLAZO;
        }
        return EstadoMaduracion.EN_TEMPORADA;
    }
}