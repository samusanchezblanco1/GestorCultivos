package com.example.plantaciones;

import com.example.plantaciones.db.Database;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.ui.PlantacionesView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Database.inicializar();
        stage.setScene(new Scene(new PlantacionesView(this::mostrarDetalle), 1000, 650));
        stage.setTitle("Gestor de plantaciones");
        stage.show();
    }

    private void mostrarDetalle(Plantacion plantacion) {
        new Alert(Alert.AlertType.INFORMATION,
                "La pantalla de detalle se está rehaciendo con el nuevo modelo (siguiente paso).").showAndWait();
    }
}