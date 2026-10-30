package com.example.plantaciones.ui;

import com.example.plantaciones.db.Database;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Fotos {

    private Fotos() { }

    /** Copia la foto a la carpeta de la app y devuelve su ruta relativa. */
    public static String guardar(File origen, long plantacionId) throws IOException {
        Path carpeta = Database.carpetaDatos().resolve("fotos");
        Files.createDirectories(carpeta);

        String nombre = origen.getName();
        String extension = nombre.contains(".") ? nombre.substring(nombre.lastIndexOf('.')) : "";
        String destino = "plantacion_" + plantacionId + "_" + System.currentTimeMillis() + extension;

        Files.copy(origen.toPath(), carpeta.resolve(destino), StandardCopyOption.REPLACE_EXISTING);
        return "fotos/" + destino;
    }

    /** Carga la imagen, o devuelve null si no hay foto o el archivo ya no existe. */
    public static Image cargar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return null;
        }
        Path ruta = Database.carpetaDatos().resolve(rutaRelativa);
        if (!Files.exists(ruta)) {
            return null;
        }
        return new Image(ruta.toUri().toString(), 230, 140, true, true);
    }
}