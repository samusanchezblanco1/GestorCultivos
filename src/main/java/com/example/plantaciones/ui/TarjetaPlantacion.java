package com.example.plantaciones.ui;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.model.Plantacion;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class TarjetaPlantacion extends VBox {

    private static final double ANCHO = 230;

    private Plantacion actual;
    private final StackPane zonaFoto = new StackPane();
    private final Consumer<Plantacion> alActualizar;

    public TarjetaPlantacion(Plantacion plantacion,
                             ArbolDao.Progreso progreso,
                             Consumer<Plantacion> alActualizar,
                             Consumer<Plantacion> alBorrar,
                             Consumer<Plantacion> alAbrir) {
        this.actual = plantacion;
        this.alActualizar = alActualizar;

        setPrefWidth(ANCHO);
        setMinWidth(ANCHO);
        setMaxWidth(ANCHO);
        setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #dcdcdc; -fx-border-radius: 12;");
        setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.15)));

        zonaFoto.setPrefSize(ANCHO, 140);
        zonaFoto.setMinHeight(140);
        zonaFoto.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 12 12 0 0;");
        mostrarFoto();

        TextoEditable nombre = new TextoEditable(plantacion.nombre(), "Sin nombre", false, false,
                texto -> guardar(new Plantacion(actual.id(), texto, actual.descripcion(),
                        actual.rutaFoto(), actual.hectareas())));
        nombre.estilo("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextoEditable descripcion = new TextoEditable(plantacion.descripcion(),
                "Doble clic para añadir una descripción", true, true,
                texto -> guardar(new Plantacion(actual.id(), actual.nombre(), texto,
                        actual.rutaFoto(), actual.hectareas())));
        descripcion.estilo("-fx-font-size: 12px;");

        Label hectareas = new Label(String.format("%.2f ha · %d manzanos", plantacion.hectareas(), progreso.total()));

        double fraccion = progreso.total() == 0 ? 0 : (double) progreso.recogidos() / progreso.total();
        ProgressBar barra = new ProgressBar(fraccion);
        barra.setMaxWidth(Double.MAX_VALUE);
        String textoProgreso = progreso.total() == 0
                ? "Sin árboles todavía"
                : String.format("%.0f %% recogido (%d de %d árboles)",
                fraccion * 100, progreso.recogidos(), progreso.total());
        Label etiquetaProgreso = new Label(textoProgreso);
        etiquetaProgreso.setStyle("-fx-font-size: 11px;");

        Button abrir = new Button("Abrir");
        abrir.setOnAction(e -> alAbrir.accept(actual));
        Button foto = new Button("Foto");
        foto.setOnAction(e -> elegirFoto());
        Button borrar = new Button("Borrar");
        borrar.setOnAction(e -> alBorrar.accept(actual));
        HBox botones = new HBox(8, abrir, foto, borrar);

        VBox contenido = new VBox(8, nombre, descripcion, hectareas, barra, etiquetaProgreso, botones);
        contenido.setPadding(new Insets(12));

        getChildren().addAll(zonaFoto, contenido);
    }

    private void guardar(Plantacion nueva) {
        actual = nueva;
        alActualizar.accept(nueva);
    }

    private void elegirFoto() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir foto de la plantación");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File archivo = selector.showOpenDialog(getScene().getWindow());
        if (archivo == null) {
            return;
        }
        try {
            String ruta = Fotos.guardar(archivo, actual.id());
            guardar(new Plantacion(actual.id(), actual.nombre(), actual.descripcion(),
                    ruta, actual.hectareas()));
            mostrarFoto();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "No se pudo copiar la foto: " + ex.getMessage()).showAndWait();
        }
    }

    private void mostrarFoto() {
        zonaFoto.getChildren().clear();
        Image imagen = Fotos.cargar(actual.rutaFoto());
        if (imagen != null) {
            zonaFoto.getChildren().add(new ImageView(imagen));
        } else {
            zonaFoto.getChildren().add(iconoManzana());
        }
    }

    private static Group iconoManzana() {
        Circle cuerpo = new Circle(40, 48, 30, Color.web("#d32f2f"));
        Rectangle tallo = new Rectangle(38, 8, 4, 14);
        tallo.setFill(Color.web("#5d4037"));
        Ellipse hoja = new Ellipse(52, 14, 11, 5);
        hoja.setFill(Color.web("#388e3c"));
        return new Group(cuerpo, tallo, hoja);
    }
}