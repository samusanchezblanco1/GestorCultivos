package com.example.plantaciones.ui;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.db.PlantacionDao;
import com.example.plantaciones.model.Plantacion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class PlantacionesView extends BorderPane {

    private final PlantacionDao plantaciones = new PlantacionDao();
    private final ArbolDao arboles = new ArbolDao();
    private final FlowPane tarjetas = new FlowPane(16, 16);

    public PlantacionesView() {
        Label titulo = new Label("Mis plantaciones");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Button nueva = new Button("+ Nueva plantación");
        nueva.setOnAction(e -> nuevaPlantacion());

        HBox cabecera = new HBox(16, titulo, nueva);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.setPadding(new Insets(16));

        tarjetas.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(tarjetas);
        scroll.setFitToWidth(true);

        setTop(cabecera);
        setCenter(scroll);
        recargar();
    }

    public void recargar() {
        tarjetas.getChildren().clear();
        List<Plantacion> lista = plantaciones.listar();
        if (lista.isEmpty()) {
            tarjetas.getChildren().add(new Label("Todavía no hay plantaciones. Pulsa «+ Nueva plantación»."));
            return;
        }
        for (Plantacion p : lista) {
            tarjetas.getChildren().add(new TarjetaPlantacion(
                    p, arboles.progreso(p.id()), this::actualizar, this::borrar, this::abrir));
        }
    }

    private void actualizar(Plantacion p) {
        plantaciones.actualizar(p);
    }

    private void abrir(Plantacion p) {
        alerta("La pantalla de detalle de «" + p.nombre() + "» la construimos en el siguiente paso.");
    }

    private void borrar(Plantacion p) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Borrar «" + p.nombre() + "» con todas sus filas, árboles y producción?",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            plantaciones.borrar(p.id());
            recargar();
        }
    }

    private void nuevaPlantacion() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Nueva plantación");

        TextField nombre = new TextField();
        nombre.setPromptText("Ej.: Plantación 1");
        TextField hectareas = new TextField();
        hectareas.setPromptText("Ej.: 2,5");

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(16));
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("Hectáreas"), hectareas);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            String texto = nombre.getText().trim();
            Double ha = parsear(hectareas.getText());
            if (texto.isEmpty() || ha == null) {
                alerta("Introduce un nombre y unas hectáreas válidas (un número mayor que 0).");
                return;
            }
            plantaciones.insertar(new Plantacion(0, texto, "", null, ha));
            recargar();
        }
    }

    private static Double parsear(String texto) {
        try {
            double valor = Double.parseDouble(texto.trim().replace(',', '.'));
            return valor > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void alerta(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }
}
