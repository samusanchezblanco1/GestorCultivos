package com.example.plantaciones.ui;

import com.example.plantaciones.db.ArbolDao;
import com.example.plantaciones.db.DatosException;
import com.example.plantaciones.db.FilaDao;
import com.example.plantaciones.db.PlantacionDao;
import com.example.plantaciones.db.ProduccionDao;
import com.example.plantaciones.db.TramoDao;
import com.example.plantaciones.db.VariedadDao;
import com.example.plantaciones.model.Arbol;
import com.example.plantaciones.model.EstadoArbol;
import com.example.plantaciones.model.EstadoMaduracion;
import com.example.plantaciones.model.Fila;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.model.Produccion;
import com.example.plantaciones.model.Tramo;
import com.example.plantaciones.model.Variedad;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class DetalleView extends BorderPane {

    private static final double RADIO = 10;
    private static final Color COLOR_RECOGIDO = Color.web("#66bb6a");
    private static final Color COLOR_PENDIENTE = Color.web("#e0e0e0");
    private static final Color COLOR_MUERTO = Color.web("#424242");

    private Plantacion plantacion;
    private final PlantacionDao plantaciones = new PlantacionDao();
    private final FilaDao filas = new FilaDao();
    private final TramoDao tramos = new TramoDao();
    private final ArbolDao arboles = new ArbolDao();
    private final ProduccionDao producciones = new ProduccionDao();
    private final VariedadDao variedades = new VariedadDao();
    private final Map<Long, Variedad> variedadesPorId = new HashMap<>();

    private final VBox listaFilas = new VBox(12);
    private final Label tituloPlantacion = new Label();
    private final Label datosPlantacion = new Label();
    private final ProgressBar barra = new ProgressBar(0);
    private final Label resumen = new Label();
    private final Label totales = new Label();
    private final Spinner<Integer> selectorAnio = new Spinner<>(2000, 2100, LocalDate.now().getYear());

    public DetalleView(Plantacion inicial, Runnable alVolver) {
        this.plantacion = inicial;

        Button volver = new Button("← Volver");
        volver.setOnAction(e -> alVolver.run());

        tituloPlantacion.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Button editarPlantacion = new Button("Editar plantación");
        editarPlantacion.setOnAction(e -> editarPlantacion());
        Button nuevaFila = new Button("+ Nueva fila");
        nuevaFila.setOnAction(e -> editarFila(null));
        Button gestionVariedades = new Button("Variedades");
        gestionVariedades.setOnAction(e -> {
            new VariedadesDialog().mostrar();
            recargar();
        });

        HBox cabecera = new HBox(16, volver, tituloPlantacion, editarPlantacion, nuevaFila, gestionVariedades);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        datosPlantacion.setStyle("-fx-font-size: 12px; -fx-opacity: 0.8;");

        barra.setPrefWidth(300);
        HBox progreso = new HBox(12, barra, resumen);
        progreso.setAlignment(Pos.CENTER_LEFT);

        selectorAnio.valueProperty().addListener((obs, antes, ahora) -> recargar());
        Button aplicarMedia = new Button("Aplicar media kg/árbol a toda la plantación");
        aplicarMedia.setOnAction(e -> pedirMedia(
                "Se calculará el estimado de cada tramo (media × árboles vivos) y sustituirá "
                        + "los estimados actuales del año " + anio() + ".",
                kg -> {
                    producciones.aplicarMedia(plantacion.id(), anio(), kg);
                    recargar();
                }));
        FlowPane produccion = new FlowPane(12, 6, new Label("Año:"), selectorAnio, totales, aplicarMedia);
        produccion.setRowValignment(VPos.CENTER);

        Label leyenda = new Label("Clic en un árbol: recogido / pendiente · clic derecho: muerto, fecha propia… "
                + "· el borde indica la variedad · gris = pendiente, verde = recogido, oscuro = muerto.");
        leyenda.setStyle("-fx-font-size: 11px; -fx-opacity: 0.7;");
        leyenda.setWrapText(true);

        VBox arriba = new VBox(8, cabecera, datosPlantacion, progreso, produccion, leyenda);
        arriba.setPadding(new Insets(16));

        listaFilas.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(listaFilas);
        scroll.setFitToWidth(true);

        setTop(arriba);
        setCenter(scroll);
        actualizarCabecera();
        recargar();
    }

    private int anio() {
        return selectorAnio.getValue();
    }

    private void actualizarCabecera() {
        tituloPlantacion.setText(plantacion.nombre());
        String texto = String.format("%.2f ha · árboles cada %.1f m · distancia al borde %.1f m",
                plantacion.hectareas(), plantacion.distanciaArboles(), plantacion.distanciaBorde());
        if (plantacion.fechaPlantacion() != null) {
            texto += " · plantación de " + Fechas.texto(plantacion.fechaPlantacion())
                    + " (" + Fechas.edad(plantacion.fechaPlantacion()) + ")";
        }
        datosPlantacion.setText(texto);
    }

    private void recargar() {
        variedadesPorId.clear();
        for (Variedad v : variedades.listar()) {
            variedadesPorId.put(v.id(), v);
        }

        Map<Long, List<Tramo>> tramosPorFila = new HashMap<>();
        for (Tramo t : tramos.listarPorPlantacion(plantacion.id())) {
            tramosPorFila.computeIfAbsent(t.filaId(), k -> new ArrayList<>()).add(t);
        }
        Map<Long, Produccion> datos = producciones.listarPorPlantacion(plantacion.id(), anio());

        listaFilas.getChildren().clear();
        List<Fila> lista = filas.listarPorPlantacion(plantacion.id());
        if (lista.isEmpty()) {
            listaFilas.getChildren().add(new Label("Esta plantación no tiene filas. Pulsa «+ Nueva fila»."));
        }
        for (Fila f : lista) {
            listaFilas.getChildren().add(crearFila(f, tramosPorFila.getOrDefault(f.id(), List.of()), datos));
        }
        actualizarResumen();
        actualizarTotales();
    }

    private VBox crearFila(Fila f, List<Tramo> losTramos, Map<Long, Produccion> datos) {
        Label titulo = new Label("Fila " + f.numero());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        String detalle = String.format("ancho %.1f m", f.ancho());
        if (f.fechaPlantacion() != null) {
            detalle += " · plantada " + Fechas.texto(f.fechaPlantacion())
                    + " (" + Fechas.edad(f.fechaPlantacion()) + ")";
        }

        Button editar = new Button("Editar fila");
        editar.setOnAction(e -> editarFila(f));
        Button nuevoTramo = new Button("+ Tramo");
        nuevoTramo.setOnAction(e -> editarTramo(f, null));
        Button borrar = new Button("Borrar fila");
        borrar.setOnAction(e -> borrarFila(f));

        FlowPane cabecera = new FlowPane(12, 6, titulo, new Label(detalle), editar, nuevoTramo, borrar);
        cabecera.setRowValignment(VPos.CENTER);

        VBox caja = new VBox(10, cabecera);
        if (losTramos.isEmpty()) {
            caja.getChildren().add(new Label("Esta fila no tiene tramos. Pulsa «+ Tramo»."));
        }
        for (Tramo t : losTramos) {
            caja.getChildren().add(crearTramo(f, t, datos.get(t.id())));
        }

        caja.setPadding(new Insets(12));
        caja.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #dcdcdc; -fx-border-radius: 10;");
        return caja;
    }

    private VBox crearTramo(Fila f, Tramo t, Produccion pr) {
        Variedad v = variedadesPorId.get(t.variedadId());
        String nombreVariedad = v == null ? "?" : v.nombre();
        Color color = v == null ? Color.GRAY : Color.web(v.color());

        List<Arbol> losArboles = arboles.listarPorTramo(t.id());
        int muertos = (int) losArboles.stream().filter(a -> a.estado() == EstadoArbol.MUERTO).count();

        Circle chip = new Circle(7, color);
        Label nombre = new Label(nombreVariedad);
        nombre.setStyle("-fx-font-weight: bold;");

        Label maduracion = new Label();
        if (v != null) {
            EstadoMaduracion estado = v.estado(LocalDate.now());
            maduracion.setText(textoMaduracion(estado));
            if (estado == EstadoMaduracion.EN_TEMPORADA) {
                maduracion.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            }
        }

        LocalDate plantado = fechaEfectiva(f, t);
        String info = t.numArboles() + " árboles"
                + (muertos > 0 ? " (" + muertos + " muertos)" : "")
                + " · "
                + (plantado == null
                ? "sin fecha de plantación"
                : "plantado " + Fechas.texto(plantado) + " (" + Fechas.edad(plantado) + ")")
                + " · recogida: " + Fechas.texto(t.fechaRecogida());

        Button editar = new Button("Editar");
        editar.setOnAction(e -> editarTramo(f, t));
        Button marcar = new Button("Marcar todo");
        marcar.setOnAction(e -> {
            arboles.marcarTramo(t.id(), EstadoArbol.RECOGIDO);
            recargar();
        });
        Button desmarcar = new Button("Desmarcar");
        desmarcar.setOnAction(e -> {
            arboles.marcarTramo(t.id(), EstadoArbol.PENDIENTE);
            recargar();
        });
        Button borrar = new Button("Borrar tramo");
        borrar.setOnAction(e -> borrarTramo(t, losArboles.size()));

        FlowPane cabecera = new FlowPane(10, 6, chip, nombre, maduracion, new Label(info),
                editar, marcar, desmarcar, borrar);
        cabecera.setRowValignment(VPos.CENTER);

        Double estimadoInicial = pr == null ? null : pr.kilosEstimados();
        Double realInicial = pr == null ? null : pr.kilosReales();
        TextField estimado = campoKilos(estimadoInicial,
                kg -> producciones.guardarEstimado(t.id(), anio(), kg));
        TextField real = campoKilos(realInicial,
                kg -> producciones.guardarReal(t.id(), anio(), kg));
        Button media = new Button("Media kg/árbol…");
        media.setOnAction(e -> pedirMedia(
                nombreVariedad + " · " + (t.numArboles() - muertos) + " árboles vivos",
                kg -> {
                    producciones.guardarEstimado(t.id(), anio(), kg * (t.numArboles() - muertos));
                    recargar();
                }));
        FlowPane filaProduccion = new FlowPane(8, 6,
                new Label("Estimado (kg):"), estimado, new Label("Real (kg):"), real, media);
        filaProduccion.setRowValignment(VPos.CENTER);

        FlowPane circulos = new FlowPane(6, 6);
        for (Arbol a : losArboles) {
            circulos.getChildren().add(crearCirculo(a, color));
        }

        VBox caja = new VBox(8, cabecera, filaProduccion, circulos);
        caja.setPadding(new Insets(10));
        caja.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 8; "
                + "-fx-border-color: #eeeeee; -fx-border-radius: 8;");
        return caja;
    }

    private LocalDate fechaEfectiva(Fila f, Tramo t) {
        if (t.fechaPlantacion() != null) {
            return t.fechaPlantacion();
        }
        if (f.fechaPlantacion() != null) {
            return f.fechaPlantacion();
        }
        return plantacion.fechaPlantacion();
    }

    private Circle crearCirculo(Arbol arbol, Color borde) {
        EstadoArbol[] estado = { arbol.estado() };

        Circle circulo = new Circle(RADIO);
        circulo.setStroke(borde);
        circulo.setStrokeWidth(3);
        circulo.setCursor(Cursor.HAND);

        Tooltip ayuda = new Tooltip();
        Tooltip.install(circulo, ayuda);
        Runnable refrescar = () -> {
            pintar(circulo, estado[0]);
            ayuda.setText("Árbol " + arbol.posicion() + " · " + textoEstado(estado[0]));
        };
        refrescar.run();

        circulo.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || estado[0] == EstadoArbol.MUERTO) {
                return;
            }
            estado[0] = estado[0] == EstadoArbol.RECOGIDO ? EstadoArbol.PENDIENTE : EstadoArbol.RECOGIDO;
            arboles.cambiarEstado(arbol.id(), estado[0]);
            refrescar.run();
            actualizarResumen();
        });

        MenuItem muerto = new MenuItem("Marcar como muerto");
        muerto.setOnAction(e -> cambiarEstado(arbol, EstadoArbol.MUERTO));
        MenuItem pendiente = new MenuItem("Marcar como pendiente");
        pendiente.setOnAction(e -> cambiarEstado(arbol, EstadoArbol.PENDIENTE));
        MenuItem recogido = new MenuItem("Marcar como recogido");
        recogido.setOnAction(e -> cambiarEstado(arbol, EstadoArbol.RECOGIDO));
        MenuItem fecha = new MenuItem("Fecha de plantación propia…");
        fecha.setOnAction(e -> fechaDelArbol(arbol));
        ContextMenu menu = new ContextMenu(muerto, pendiente, recogido, new SeparatorMenuItem(), fecha);
        circulo.setOnContextMenuRequested(e -> menu.show(circulo, e.getScreenX(), e.getScreenY()));

        return circulo;
    }

    private void cambiarEstado(Arbol arbol, EstadoArbol nuevo) {
        arboles.cambiarEstado(arbol.id(), nuevo);
        recargar();
    }

    private void fechaDelArbol(Arbol arbol) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Fecha de plantación del árbol " + arbol.posicion());
        dialogo.setHeaderText("Si la dejas vacía, hereda la del tramo, la fila o la plantación.");

        SelectorFecha selector = new SelectorFecha(arbol.fechaPlantacion());
        VBox contenido = new VBox(10, selector);
        contenido.setPadding(new Insets(16));
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            arboles.ponerFecha(arbol.id(), selector.valor());
            recargar();
        }
    }

    private static void pintar(Circle circulo, EstadoArbol estado) {
        switch (estado) {
            case RECOGIDO -> circulo.setFill(COLOR_RECOGIDO);
            case PENDIENTE -> circulo.setFill(COLOR_PENDIENTE);
            case MUERTO -> circulo.setFill(COLOR_MUERTO);
        }
        circulo.setOpacity(estado == EstadoArbol.MUERTO ? 0.6 : 1.0);
    }

    private static String textoEstado(EstadoArbol estado) {
        return switch (estado) {
            case PENDIENTE -> "pendiente";
            case RECOGIDO -> "recogido";
            case MUERTO -> "muerto";
        };
    }

    private static String textoMaduracion(EstadoMaduracion estado) {
        return switch (estado) {
            case SIN_DATOS -> "";
            case AUN_NO -> "aún no maduro";
            case EN_TEMPORADA -> "en temporada";
            case FUERA_DE_PLAZO -> "fuera de plazo";
        };
    }

    private void actualizarResumen() {
        ArbolDao.Progreso p = arboles.progreso(plantacion.id());
        double fraccion = p.total() == 0 ? 0 : (double) p.recogidos() / p.total();
        barra.setProgress(fraccion);

        String texto = String.format("%.0f %% recogido · %d de %d manzanos vivos",
                fraccion * 100, p.recogidos(), p.total());
        if (p.muertos() > 0) {
            texto += " · " + p.muertos() + " muertos";
        }
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

    private void editarPlantacion() {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar plantación");

        TextField hectareas = new TextField(numero(plantacion.hectareas()));
        SelectorFecha fecha = new SelectorFecha(plantacion.fechaPlantacion());
        TextField distanciaArboles = new TextField(numero(plantacion.distanciaArboles()));
        TextField distanciaBorde = new TextField(numero(plantacion.distanciaBorde()));

        GridPane rejilla = nuevaRejilla();
        rejilla.addRow(0, new Label("Hectáreas"), hectareas);
        rejilla.addRow(1, new Label("Plantación (fecha o edad)"), fecha);
        rejilla.addRow(2, new Label("Distancia entre árboles (m)"), distanciaArboles);
        rejilla.addRow(3, new Label("Distancia al borde (m)"), distanciaBorde);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        Double ha = decimal(hectareas.getText());
        Double entreArboles = decimal(distanciaArboles.getText());
        Double alBorde = decimal(distanciaBorde.getText());
        if (ha == null || ha <= 0 || entreArboles == null || entreArboles <= 0 || alBorde == null) {
            alerta("Revisa los números: hectáreas y distancia entre árboles deben ser mayores que 0, "
                    + "y la distancia al borde no puede ser negativa.");
            return;
        }

        Plantacion nueva = new Plantacion(plantacion.id(), plantacion.nombre(), plantacion.descripcion(),
                plantacion.rutaFoto(), ha, fecha.valor(), entreArboles, alBorde);
        plantaciones.actualizar(nueva);
        plantacion = nueva;
        actualizarCabecera();
        recargar();
    }

    private void editarFila(Fila existente) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Nueva fila" : "Editar fila");

        TextField numero = new TextField(String.valueOf(
                existente == null ? siguienteNumero() : existente.numero()));
        TextField ancho = new TextField(numero(existente == null ? 4.0 : existente.ancho()));
        SelectorFecha fecha = new SelectorFecha(existente == null ? null : existente.fechaPlantacion());

        GridPane rejilla = nuevaRejilla();
        rejilla.addRow(0, new Label("Nº de fila"), numero);
        rejilla.addRow(1, new Label("Ancho de la fila (m)"), ancho);
        rejilla.addRow(2, new Label("Plantada (fecha o edad)"), fecha);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        Integer n = entero(numero.getText(), 1000);
        Double a = decimal(ancho.getText());
        if (n == null || a == null || a <= 0) {
            alerta("Introduce un número de fila (1 a 1000) y un ancho mayor que 0.");
            return;
        }

        try {
            if (existente == null) {
                long id = filas.insertar(new Fila(0, plantacion.id(), n, a, fecha.valor()));
                recargar();
                editarTramo(new Fila(id, plantacion.id(), n, a, fecha.valor()), null);
            } else {
                filas.actualizar(new Fila(existente.id(), plantacion.id(), n, a, fecha.valor()));
                recargar();
            }
        } catch (DatosException ex) {
            alerta("No se pudo guardar la fila. ¿Ya existe otra fila con el número " + n + "?");
        }
    }

    private void editarTramo(Fila fila, Tramo existente) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(existente == null ? "Nuevo tramo en la fila " + fila.numero() : "Editar tramo");

        ComboBox<String> variedad = new ComboBox<>();
        variedad.setEditable(true);
        variedad.setPromptText("Ej.: Fuji");
        for (Variedad v : variedades.listar()) {
            variedad.getItems().add(v.nombre());
        }
        if (existente != null && variedadesPorId.containsKey(existente.variedadId())) {
            variedad.setValue(variedadesPorId.get(existente.variedadId()).nombre());
        }

        TextField numArboles = new TextField(existente == null ? "" : String.valueOf(existente.numArboles()));
        numArboles.setPromptText("Ej.: 35");
        SelectorFecha plantado = new SelectorFecha(existente == null ? null : existente.fechaPlantacion());
        DatePicker recogida = new DatePicker(existente == null ? null : existente.fechaRecogida());

        GridPane rejilla = nuevaRejilla();
        rejilla.addRow(0, new Label("Variedad"), variedad);
        rejilla.addRow(1, new Label("Nº de árboles"), numArboles);
        rejilla.addRow(2, new Label("Plantado (fecha o edad)"), plantado);
        rejilla.addRow(3, new Label("Fecha de recogida"), recogida);

        dialogo.getDialogPane().setContent(rejilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> respuesta = dialogo.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        String nombreVariedad = variedad.getEditor().getText().trim();
        Integer cantidad = entero(numArboles.getText(), 1000);
        if (nombreVariedad.isEmpty() || cantidad == null) {
            alerta("Escribe o elige una variedad e indica el número de árboles (1 a 1000).");
            return;
        }

        try {
            long variedadId = variedades.obtenerOCrear(nombreVariedad);
            if (existente == null) {
                tramos.insertar(new Tramo(0, fila.id(), siguienteOrden(fila), variedadId, cantidad,
                        plantado.valor(), recogida.getValue()));
            } else {
                tramos.actualizar(new Tramo(existente.id(), fila.id(), existente.orden(), variedadId, cantidad,
                        plantado.valor(), recogida.getValue()));
            }
            recargar();
        } catch (DatosException ex) {
            alerta("No se pudo guardar el tramo: " + ex.getMessage());
        }
    }

    private void borrarFila(Fila f) {
        confirmar("¿Borrar la fila " + f.numero() + " con todos sus tramos y árboles?", () -> {
            filas.borrar(f.id());
            recargar();
        });
    }

    private void borrarTramo(Tramo t, int numeroDeArboles) {
        confirmar("¿Borrar este tramo con sus " + numeroDeArboles + " árboles?", () -> {
            tramos.borrar(t.id());
            recargar();
        });
    }

    private void confirmar(String mensaje, Runnable alAceptar) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            alAceptar.run();
        }
    }

    private int siguienteNumero() {
        return filas.listarPorPlantacion(plantacion.id()).stream()
                .mapToInt(Fila::numero)
                .max()
                .orElse(0) + 1;
    }

    private int siguienteOrden(Fila fila) {
        return tramos.listarPorFila(fila.id()).stream()
                .mapToInt(Tramo::orden)
                .max()
                .orElse(0) + 1;
    }

    private static GridPane nuevaRejilla() {
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(16));
        return rejilla;
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

    private static String numero(double valor) {
        return String.format("%.2f", valor);
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