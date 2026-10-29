package com.example.plantaciones;

import com.example.plantaciones.db.Database;
import com.example.plantaciones.db.PlantacionDao;
import com.example.plantaciones.model.Plantacion;

public class PruebaBD {
    public static void main(String[] args) {
        Database.inicializar();
        PlantacionDao dao = new PlantacionDao();

        long id = dao.insertar(new Plantacion(0, "Plantación 1", "Prueba", null, 2.5));
        System.out.println("Insertada con id " + id);

        dao.listar().forEach(System.out::println);
    }
}