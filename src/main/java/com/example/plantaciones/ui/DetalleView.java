package com.example.plantaciones.ui;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.db.DatosException;
import com.example.plantaciones.db.FilaDao;
import com.example.plantaciones.db.ProduccionDao;
import com.example.plantaciones.model.Arbol;
import com.example.plantaciones.model.Fila;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.model.Produccion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class DetalleView extends BorderPane {

    private static final double RADIO = 10;
    private static final Color COLOR_RECOGIDO = Color.web("#66bb6a");
    private static final Color COLOR_PENDIENTE = Color.web("#e0e0e0");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Plantacion plantacion;
    private final FilaDao filas = new FilaDao();
    private final ArbolDao arboles = new ArbolDao();
    private final ProduccionDao producciones = new ProduccionDao();

    private final VBox listaFilas = new VBox(12);
    private final ProgressBar barra = new ProgressBar(0);
    private final Label resumen = new Label();
    private final Label totales = new Label();
    private final Spinner<Integer> selectorAnio = new Spinner<>(2000, 2100, LocalDate.now().getYear());

    public DetalleView(Plantacion plantacion, Runnable alVolver) {
        this.plantacion = plantacion;

        Button volver = new Button("← Volver");
        volver.setOnAction(e -> alVolver.run());

        Label titulo = new Label(plantacion.nombre());
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label hectareas = new Label(String.format("%.2f ha", plantacion.hectareas()));

        Button nuevaFila = new Button("+ Nueva fila");
        nuevaFila.setOnAction(e -> editarFila(null));

        HBox cabecera = new HBox(16, volver, titulo, hectareas, nuevaFila);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        barra.setPrefWidth(300);
        HBox progreso = new HBox(12, barra, resumen);
        progreso.setAlignment(Pos.CENTER_LEFT);

        selectorAnio.valueProperty().addListener((obs, antes, ahora) -> recargar());
        Button aplicarMedia = new Button("Aplicar media kg/árbol a toda la plantación");
        aplicarMedia.setOnAction(e -> pedirMedia(
                "Se calculará el estimado de cada fila (media × nº de árboles) y sustituirá "
                        + "los estimados actuales del año " + anio() + ".",
                kg -> {
                    producciones.aplicarMedia(plantacion.id(), anio(), kg);
                    recargar();
                }));
        HBox produccion = new HBox(12, new Label("Año:"), selectorAnio, totales, aplicarMedia);
        produccion.setAlignment(Pos.CENTER_LEFT);

        Label leyenda = new Label("Círculo verde = recogido · gris = pendiente · el borde indica la variedad. "
                + "Haz clic en un árbol para marcarlo.");
        leyenda.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");

        VBox arriba = new VBox(8, cabecera, progreso, produccion, leyenda);
        arriba.setPadding(new Insets(16));

        listaFilas.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(listaFilas);
        scroll.setFitToWidth(true);

        setTop(arriba);
        setCenter(scroll);
        recargar();
    }

    private int anio() {
        return selectorAnio.getValue();
    }

    private void recargar() {
        listaFilas.getChildren().clear();
        List<Fila> lista = filas.listarPorPlantacion(plantacion.id());
        Map<Long, Produccion> datos = producciones.listarPorPlantacion(plantacion.id(), anio());
        if (lista.isEmpty()) {
            listaFilas.getChildren().add(new Label("Esta plantación no tiene filas. Pulsa «+ Nueva fila»."));
        }
        for (Fila f : lista) {
            listaFilas.getChildren().add(crearFila(f, datos.get(f.id())));
        }
        actualizarResumen();
        actualizarTotales();
    }

    private VBox crearFila(Fila f, Produccion prod) {
        Label titulo = new Label(String.format("Fila %d · %s", f.numero(), f.variedad()));
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        String fecha = f.fechaRecogida() == null ? "sin fecha" : f.fechaRecogida().format(FORMATO);
        Label info = new Label(f.numArboles() + " árboles · recogida: " + fecha);

        Button editar = new Button("Editar");
        editar.setOnAction(e -> editarFila(f));
        Button marcar = new Button("Marcar toda");
        marcar.setOnAction(e -> {
            arboles.marcarFila(f.id(), true);
            recargar();
        });
        Button desmarcar = new Button("Desmarcar");
        desmarcar.setOnAction(e -> {
            arboles.marcarFila(f.id(), false);
            recargar();
        });
        Button borrar = new Button("Borrar");
        borrar.setOnAction(e -> borrarFila(f));

        HBox cabecera = new HBox(12, titulo, info, editar, marcar, desmarcar, borrar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Double estimadoInicial = prod == null ? null : prod.kilosEstimados();
        Double realInicial = prod == null ? null : prod.kilosReales();
        TextField estimado = campoKilos(estimadoInicial,
                kg -> producciones.guardarEstimado(f.id(), anio(), kg));
        TextField real = campoKilos(realInicial,
                kg -> producciones.guardarReal(f.id(), anio(), kg));
        Button media = new Button("Media kg/árbol…");
        media.setOnAction(e -> pedirMedia(
                "Fila " + f.numero() + " · " + f.numArboles() + " árboles",
                kg -> {
                    producciones.guardarEstimado(f.id(), anio(), kg * f.numArboles());
                    recargar();
                }));
        HBox filaProduccion = new HBox(8,
                new Label("Estimado (kg):"), estimado, new Label("Real (kg):"), real, media);
        filaProduccion.setAlignment(Pos.CENTER_LEFT);

        Color colorVariedad = Color.hsb(Math.abs(f.variedad().hashCode() % 360), 0.65, 0.8);
        FlowPane circulos = new FlowPane(6, 6);
        for (Arbol a : arboles.listarPorFila(f.id())) {
            circulos.getChildren().add(crearCirculo(a, colorVariedad));
        }

        VBox caja = new VBox(8, cabecera, filaProduccion, circulos);
        caja.setPadding(new Insets(12));
        caja.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #dcdcdc; -fx-border-radius: 10;");
        return caja;
    }

    private TextField campoKilos(Double inicial, Consumer<Double> alGuardar) {
        String[] mostrado = { inicial == null ? "" : kilosCampo(inicial) };
        TextField campo = new TextField(mostrado[0]);
        campo.setPrefWidth(100);
        campo.setPromptText("kg");

        Runnable guardar = () -> {
            String texto = campo.getText().trim();
            if (texto.equals(mostrado[0])) {
                return;
            }
            Double valor = texto.isEmpty() ? null : decimal(texto);
            if (!texto.isEmpty() && valor == null) {
                campo.setText(mostrado[0]);
                return;
            }
            alGuardar.accept(valor);
            mostrado[0] = valor == null ? "" : kilosCampo(valor);
            campo.setText(mostrado[0]);
            actualizarTotales();
        };

        campo.setOnAction(e -> guardar.run());
        campo.focusedProperty().addListener((obs, antes, ahora) -> {
            if (!ahora) {
                guardar.run();
            }
        });
        return campo;
    }

    private void pedirMedia(String mensaje, Consumer<Double> alAceptar) {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Media por árbol");
        dialogo.setHeaderText(mensaje);
        dialogo.setContentText("kg por árbol:");
        Optional<String> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty()) {
            return;
        }
        Double kg = decimal(respuesta.get());
        if (kg == null || kg <= 0) {
            alerta("Introduce un número mayor que 0 (por ejemplo, 45,5).");
            return;
        }
        alAceptar.accept(kg);
    }

    private Circle crearCirculo(Arbol arbol, Color borde) {
        boolean[] recogido = { arbol.recogido() };

        Circle circulo = new Circle(RADIO);
        circulo.setStroke(borde);
        circulo.setStrokeWidth(3);
        circulo.setCursor(Cursor.HAND);
        pintar(circulo, recogido[0]);
        Tooltip.install(circulo, new Tooltip("Árbol " + arbol.posicion()));

        circulo.setOnMouseClicked(e -> {
            recogido[0] = !recogido[0];
            arboles.marcar(arbol.id(), recogido[0]);
            pintar(circulo, recogido[0]);
            actualizarResumen();
        });
        return circulo;
    }

    private static void pintar(Circle circulo, boolean recogido) {
        circulo.setFill(recogido ? COLOR_RECOGIDO : COLOR_PENDIENTE);
    }

    private void actualizarResumen() {
        ArbolDao.Progreso p = arboles.progreso(plantacion.id());
        double fraccion = p.total() == 0 ? 0 : (double) p.recogidos() / p.total();
        barra.setProgress(fraccion);

        String texto = String.format("%.0f %% recogido · %d de %d manzanos",
                fraccion * 100, p.recogidos(), p.total());
        if (p.completo()) {
            texto += " · ¡Cosecha completa!";
        }
        resumen.setText(texto);
    }

    private void actualizarTotales() {
        ProduccionDao.Totales t = producciones.totales(plantacion.id(), anio());
        totales.setText(String.format("Estimado: %s kg · Real: %s kg",
                kilosTotal(t.estimados()), kilosTotal(t.reales())));
    }

    private void editarFila(Fila existente) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Nueva fila" : "Editar fila");

        TextField numero = new TextField(String.valueOf(
                existente == null ? siguienteNumero() : existente.numero()));
        TextField variedad = new TextField(existente == null ? "" : existente.variedad());
        variedad.setPromptText("Ej.: Fuji");
        TextField numArboles = new TextField(existente == null ? "" : String.valueOf(existente.numArboles()));
        numArboles.setPromptText("Ej.: 35");
        DatePicker fecha = new DatePicker(existente == null ? null : existente.fechaRecogida());

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(16));
        rejilla.addRow(0, new Label("Nº de fila"), numero);
        rejilla.addRow(1, new Label("Variedad"), variedad);
        rejilla.addRow(2, new Label("Nº de árboles"), numArboles);
        rejilla.addRow(3, new Label("Fecha de recogida"), fecha);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        Integer n = entero(numero.getText(), 1000);
        Integer cantidad = entero(numArboles.getText(), 1000);
        String nombreVariedad = variedad.getText().trim();
        if (n == null || cantidad == null || nombreVariedad.isEmpty()) {
            alerta("Rellena el número de fila, la variedad y el número de árboles (números entre 1 y 1000).");
            return;
        }

        try {
            if (existente == null) {
                filas.insertar(new Fila(0, plantacion.id(), n, nombreVariedad, cantidad, fecha.getValue()));
            } else {
                filas.actualizar(new Fila(existente.id(), plantacion.id(), n, nombreVariedad,
                        cantidad, fecha.getValue()));
            }
            recargar();
        } catch (DatosException ex) {
            alerta("No se pudo guardar la fila. ¿Ya existe otra fila con el número " + n + "?");
        }
    }

    private void borrarFila(Fila f) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Borrar la fila " + f.numero() + " con sus " + f.numArboles() + " árboles?",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            filas.borrar(f.id());
            recargar();
        }
    }

    private int siguienteNumero() {
        return filas.listarPorPlantacion(plantacion.id()).stream()
                .mapToInt(Fila::numero)
                .max()
                .orElse(0) + 1;
    }

    private static Integer entero(String texto, int maximo) {
        try {
            int valor = Integer.parseInt(texto.trim());
            return (valor > 0 && valor <= maximo) ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double decimal(String texto) {
        try {
            double valor = Double.parseDouble(texto.trim().replace(',', '.'));
            return (valor >= 0 && Double.isFinite(valor)) ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String kilosCampo(double valor) {
        return String.format("%.1f", valor);
    }

    private static String kilosTotal(double valor) {
        return String.format("%,.1f", valor);
    }

    private static void alerta(String mensaje) {
        new Alert(Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }
}