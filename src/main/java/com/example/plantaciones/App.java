package com.example.plantaciones;

import com.example.plantaciones.db.Database;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Database.inicializar();

        StackPane raiz = new StackPane(new Label("Gestor de plantaciones"));
        stage.setScene(new Scene(raiz, 800, 500));
        stage.setTitle("Gestor de plantaciones");
        stage.show();
    }
}