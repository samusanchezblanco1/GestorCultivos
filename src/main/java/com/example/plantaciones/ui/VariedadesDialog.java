package com.example.plantaciones.ui;

import com.example.plantaciones.db.DatosException;
import com.example.plantaciones.db.VariedadDao;
import com.example.plantaciones.model.EstadoMaduracion;
import com.example.plantaciones.model.Variedad;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class VariedadesDialog {

    private static final DateTimeFormatter MOSTRAR = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LEER = DateTimeFormatter.ofPattern("d/M");

    private final VariedadDao dao = new VariedadDao();
    private final TableView<Variedad> tabla = new TableView<>();

    public void mostrar() {
        configurarColumnas();
        recargar();

        Button nueva = new Button("Nueva");
        nueva.setOnAction(e -> editar(null));
        Button editar = new Button("Editar");
        editar.setOnAction(e -> editarSeleccionada());
        Button borrar = new Button("Borrar");
        borrar.setOnAction(e -> borrarSeleccionada());
        HBox botones = new HBox(8, nueva, editar, borrar);

        tabla.setPlaceholder(new Label("Aún no hay variedades. Pulsa «Nueva» o créalas al añadir un tramo."));
        tabla.setPrefSize(640, 320);
        tabla.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                editarSeleccionada();
            }
        });

        Label ayuda = new Label("La época de maduración es orientativa (día/mes). "
                + "Doble clic en una variedad para editarla.");
        ayuda.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");

        VBox contenido = new VBox(10, tabla, botones, ayuda);
        contenido.setPadding(new Insets(12));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Variedades y épocas de maduración");
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.showAndWait();
    }

    private void configurarColumnas() {
        TableColumn<Variedad, Variedad> colColor = new TableColumn<>("Color");
        colColor.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colColor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Variedad v, boolean vacio) {
                super.updateItem(v, vacio);
                setGraphic(vacio || v == null ? null : new Circle(8, Color.web(v.color())));
            }
        });
        colColor.setPrefWidth(60);

        TableColumn<Variedad, String> colNombre = new TableColumn<>("Variedad");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        colNombre.setPrefWidth(200);

        TableColumn<Variedad, String> colInicio = new TableColumn<>("Maduración desde");
        colInicio.setCellValueFactory(c -> new SimpleStringProperty(diaTexto(c.getValue().inicio())));
        colInicio.setPrefWidth(130);

        TableColumn<Variedad, String> colFin = new TableColumn<>("hasta");
        colFin.setCellValueFactory(c -> new SimpleStringProperty(diaTexto(c.getValue().fin())));
        colFin.setPrefWidth(90);

        TableColumn<Variedad, String> colEstado = new TableColumn<>("Hoy");
        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(textoEstado(c.getValue().estado(LocalDate.now()))));
        colEstado.setPrefWidth(130);

        tabla.getColumns().add(colColor);
        tabla.getColumns().add(colNombre);
        tabla.getColumns().add(colInicio);
        tabla.getColumns().add(colFin);
        tabla.getColumns().add(colEstado);
    }

    private void recargar() {
        tabla.setItems(FXCollections.observableArrayList(dao.listar()));
    }

    private void editarSeleccionada() {
        Variedad v = tabla.getSelectionModel().getSelectedItem();
        if (v == null) {
            alerta("Selecciona una variedad de la tabla.");
            return;
        }
        editar(v);
    }

    private void borrarSeleccionada() {
        Variedad v = tabla.getSelectionModel().getSelectedItem();
        if (v == null) {
            alerta("Selecciona una variedad de la tabla.");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Borrar la variedad «" + v.nombre() + "»?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                dao.borrar(v.id());
                recargar();
            } catch (DatosException ex) {
                alerta("No se puede borrar «" + v.nombre() + "»: está en uso en algún tramo.");
            }
        }
    }

    private void editar(Variedad existente) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Nueva variedad" : "Editar variedad");

        TextField nombre = new TextField(existente == null ? "" : existente.nombre());
        nombre.setPromptText("Ej.: Fuji");
        ColorPicker color = new ColorPicker(
                existente == null ? Color.web("#e53935") : Color.web(existente.color()));
        TextField inicio = new TextField(existente == null ? "" : diaEditable(existente.inicio()));
        inicio.setPromptText("día/mes, ej. 05/10");
        TextField fin = new TextField(existente == null ? "" : diaEditable(existente.fin()));
        fin.setPromptText("día/mes, ej. 30/11");

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(16));
        rejilla.addRow(0, new Label("Nombre"), nombre);
        rejilla.addRow(1, new Label("Color"), color);
        rejilla.addRow(2, new Label("Maduración desde"), inicio);
        rejilla.addRow(3, new Label("hasta"), fin);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        String nombreTexto = nombre.getText().trim();
        if (nombreTexto.isEmpty()) {
            alerta("La variedad necesita un nombre.");
            return;
        }

        String textoInicio = inicio.getText().trim();
        String textoFin = fin.getText().trim();
        MonthDay desde = null;
        MonthDay hasta = null;
        if (!textoInicio.isEmpty() || !textoFin.isEmpty()) {
            if (textoInicio.isEmpty() || textoFin.isEmpty()) {
                alerta("Rellena las dos fechas de maduración, o déjalas las dos vacías.");
                return;
            }
            try {
                desde = MonthDay.parse(textoInicio, LEER);
                hasta = MonthDay.parse(textoFin, LEER);
            } catch (DateTimeParseException ex) {
                alerta("Escribe las fechas como día/mes, por ejemplo 05/10.");
                return;
            }
            if (hasta.isBefore(desde)) {
                alerta("El final no puede ser anterior al inicio (la época no puede cruzar el fin de año).");
                return;
            }
        }

        String hex = aHex(color.getValue());
        try {
            if (existente == null) {
                dao.insertar(new Variedad(0, nombreTexto, hex, desde, hasta));
            } else {
                dao.actualizar(new Variedad(existente.id(), nombreTexto, hex, desde, hasta));
            }
            recargar();
        } catch (DatosException ex) {
            alerta("No se pudo guardar. ¿Ya existe otra variedad con el nombre «" + nombreTexto + "»?");
        }
    }

    private static String aHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255),
                Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }

    private static String diaTexto(MonthDay dia) {
        return dia == null ? "—" : dia.format(MOSTRAR);
    }

    private static String diaEditable(MonthDay dia) {
        return dia == null ? "" : dia.format(MOSTRAR);
    }

    private static String textoEstado(EstadoMaduracion estado) {
        return switch (estado) {
            case SIN_DATOS -> "sin datos";
            case AUN_NO -> "aún no maduro";
            case EN_TEMPORADA -> "en temporada";
            case FUERA_DE_PLAZO -> "fuera de plazo";
        };
    }

    private static void alerta(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }
}