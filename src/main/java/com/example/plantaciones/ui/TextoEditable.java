package com.example.plantaciones.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public class TextoEditable extends StackPane {

    private final Label etiqueta = new Label();
    private final TextInputControl campo;
    private final String textoVacio;
    private final boolean permitirVacio;
    private final Consumer<String> alCambiar;
    private String valor;

    public TextoEditable(String valor, String textoVacio, boolean multilinea,
                         boolean permitirVacio, Consumer<String> alCambiar) {
        this.valor = valor == null ? "" : valor;
        this.textoVacio = textoVacio;
        this.permitirVacio = permitirVacio;
        this.alCambiar = alCambiar;
        this.campo = multilinea ? new TextArea() : new TextField();

        etiqueta.setWrapText(true);
        campo.setVisible(false);
        if (campo instanceof TextArea area) {
            area.setPrefRowCount(3);
            area.setWrapText(true);
        }
        getChildren().addAll(etiqueta, campo);
        pintarEtiqueta();

        etiqueta.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                empezarEdicion();
            }
        });
        campo.focusedProperty().addListener((obs, antes, ahora) -> {
            if (!ahora) {
                terminarEdicion(true);
            }
        });
        campo.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                terminarEdicion(false);
            } else if (e.getCode() == KeyCode.ENTER && !(campo instanceof TextArea)) {
                terminarEdicion(true);
            }
        });
    }

    public void estilo(String css) {
        etiqueta.setStyle(css);
    }

    private void empezarEdicion() {
        campo.setText(valor);
        etiqueta.setVisible(false);
        campo.setVisible(true);
        campo.requestFocus();
        campo.selectAll();
    }

    private void terminarEdicion(boolean guardar) {
        if (!campo.isVisible()) {
            return;
        }
        campo.setVisible(false);
        etiqueta.setVisible(true);
        if (!guardar) {
            return;
        }
        String nuevo = campo.getText().trim();
        if ((nuevo.isEmpty() && !permitirVacio) || nuevo.equals(valor)) {
            return;
        }
        valor = nuevo;
        pintarEtiqueta();
        alCambiar.accept(valor);
    }

    private void pintarEtiqueta() {
        boolean vacio = valor.isBlank();
        etiqueta.setText(vacio ? textoVacio : valor);
        etiqueta.setOpacity(vacio ? 0.5 : 1.0);
    }
}