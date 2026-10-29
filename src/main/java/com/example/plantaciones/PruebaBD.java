package com.example.plantaciones;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.db.Database;
import com.example.plantaciones.db.FilaDao;
import com.example.plantaciones.db.PlantacionDao;
import com.example.plantaciones.db.ProduccionDao;
import com.example.plantaciones.model.Fila;
import com.example.plantaciones.model.Plantacion;

import java.time.LocalDate;

public class PruebaBD {
    public static void main(String[] args) {
        Database.inicializar();
        PlantacionDao plantaciones = new PlantacionDao();
        FilaDao filas = new FilaDao();
        ArbolDao arboles = new ArbolDao();
        ProduccionDao producciones = new ProduccionDao();

        long p = plantaciones.insertar(new Plantacion(0, "Plantación 2", "Prueba filas", null, 1.8));
        long f1 = filas.insertar(new Fila(0, p, 1, "Fuji", 5, LocalDate.of(2026, 10, 15)));
        long f2 = filas.insertar(new Fila(0, p, 2, "Gala", 3, LocalDate.of(2026, 9, 20)));

        System.out.println("Árboles fila 1: " + arboles.listarPorFila(f1).size());

        arboles.marcarFila(f1, true);
        System.out.println(arboles.progreso(p));
        arboles.marcarFila(f2, true);
        System.out.println("¿Completo? " + arboles.progreso(p).completo());

        producciones.guardar(f1, 2026, 1200.5);
        producciones.guardar(f2, 2026, 800);
        producciones.guardar(f2, 2026, 850);
        System.out.println("Total 2026: " + producciones.totalPlantacion(p, 2026));

        filas.actualizar(new Fila(f1, p, 1, "Fuji", 3, LocalDate.of(2026, 10, 15)));
        System.out.println("Árboles fila 1 tras reducir: " + arboles.listarPorFila(f1).size());
    }
}