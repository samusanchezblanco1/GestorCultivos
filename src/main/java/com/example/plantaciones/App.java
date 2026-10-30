package com.example.plantaciones;

import com.example.plantaciones.db.Database;
import com.example.plantaciones.ui.PlantacionesView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Database.inicializar();
        stage.setScene(new Scene(new PlantacionesView(), 1000, 650));
        stage.setTitle("Gestor de plantaciones");
        stage.show();
    }
}