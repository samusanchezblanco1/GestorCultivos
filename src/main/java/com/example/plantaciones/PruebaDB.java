package com.example.plantaciones;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.db.Database;
import com.example.plantaciones.db.FilaDao;
import com.example.plantaciones.db.PlantacionDao;
import com.example.plantaciones.db.ProduccionAnualDao;
import com.example.plantaciones.db.ProduccionDao;
import com.example.plantaciones.db.TramoDao;
import com.example.plantaciones.db.VariedadDao;
import com.example.plantaciones.model.Arbol;
import com.example.plantaciones.model.EstadoArbol;
import com.example.plantaciones.model.Fila;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.model.Tramo;

import java.time.LocalDate;
import java.util.List;

public class PruebaDB {
    public static void main(String[] args) {
        Database.inicializar();
        VariedadDao variedades = new VariedadDao();
        PlantacionDao plantaciones = new PlantacionDao();
        FilaDao filas = new FilaDao();
        TramoDao tramos = new TramoDao();
        ArbolDao arboles = new ArbolDao();
        ProduccionDao producciones = new ProduccionDao();
        ProduccionAnualDao anuales = new ProduccionAnualDao();

        long fuji = variedades.obtenerOCrear("Fuji");
        long gala = variedades.obtenerOCrear("Gala");
        System.out.println("¿Misma variedad con otras mayúsculas? " + (variedades.obtenerOCrear("fuji") == fuji));

        long p = plantaciones.insertar(new Plantacion(0, "Plantación 1", "Prueba", null, 2.0));
        long f1 = filas.insertar(new Fila(0, p, 1, 4.0, null));
        long t1 = tramos.insertar(new Tramo(0, f1, 1, fuji, 5,
                LocalDate.of(2018, 3, 15), LocalDate.of(2026, 10, 15)));
        long t2 = tramos.insertar(new Tramo(0, f1, 2, gala, 3,
                LocalDate.of(2022, 3, 10), LocalDate.of(2026, 9, 20)));

        List<Arbol> delTramo1 = arboles.listarPorTramo(t1);
        System.out.println("Árboles tramo 1: " + delTramo1.size());

        arboles.cambiarEstado(delTramo1.get(0).id(), EstadoArbol.MUERTO);
        arboles.marcarTramo(t1, EstadoArbol.RECOGIDO);
        System.out.println(arboles.progreso(p));
        arboles.marcarTramo(t2, EstadoArbol.RECOGIDO);
        System.out.println("¿Completo? " + arboles.progreso(p).completo());

        producciones.guardarEstimado(t1, 2026, 1000.0);
        producciones.guardarReal(t1, 2026, 950.5);
        producciones.guardarReal(t2, 2026, 400.0);
        System.out.println(producciones.totales(p, 2026));
        producciones.aplicarMedia(p, 2026, 40);
        System.out.println(producciones.totales(p, 2026));

        tramos.actualizar(new Tramo(t1, f1, 1, fuji, 3,
                LocalDate.of(2018, 3, 15), LocalDate.of(2026, 10, 15)));
        System.out.println("Árboles tramo 1 tras reducir: " + arboles.listarPorTramo(t1).size());
        System.out.println(arboles.progreso(p));

        anuales.guardarReal(2025, fuji, null, 20000.0);
        anuales.guardarReal(2025, fuji, null, 21000.0);
        anuales.guardarReal(2025, fuji, p, 8000.0);
        anuales.listar().forEach(System.out::println);
    }
}