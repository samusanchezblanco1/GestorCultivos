package com.example.plantaciones.ui;

import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.time.LocalDate;

/** Un selector de fecha con un campo extra para escribir solo la edad en años. */
public class SelectorFecha extends HBox {

    private final DatePicker fecha = new DatePicker();
    private final TextField edad = new TextField();

    public SelectorFecha(LocalDate inicial) {
        super(8);
        fecha.setValue(inicial);
        edad.setPromptText("o edad (años)");
        edad.setPrefWidth(110);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(fecha, edad);
    }

    /** La fecha elegida. Si solo se escribió la edad, calcula la fecha; si no hay nada, null. */
    public LocalDate valor() {
        if (fecha.getValue() != null) {
            return fecha.getValue();
        }
        try {
            int anios = Integer.parseInt(edad.getText().trim());
            if (anios >= 0 && anios <= 200) {
                return LocalDate.now().minusYears(anios);
            }
        } catch (NumberFormatException e) {
            // Edad no válida: se ignora y se devuelve null
        }
        return null;
    }
}