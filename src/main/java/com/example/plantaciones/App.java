package com.example.plantaciones;

import com.example.plantaciones.db.Database;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.ui.DetalleView;
import com.example.plantaciones.ui.PlantacionesView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private Scene escena;

    @Override
    public void start(Stage stage) {
        Database.inicializar();
        escena = new Scene(crearLista(), 1000, 650);
        stage.setScene(escena);
        stage.setTitle("Gestor de plantaciones");
        stage.show();
    }

    private PlantacionesView crearLista() {
        return new PlantacionesView(this::mostrarDetalle);
    }

    private void mostrarDetalle(Plantacion plantacion) {
        escena.setRoot(new DetalleView(plantacion, this::mostrarLista));
    }

    private void mostrarLista() {
        escena.setRoot(crearLista());
    }
}