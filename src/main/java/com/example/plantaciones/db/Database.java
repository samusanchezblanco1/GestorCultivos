package com.example.plantaciones.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static Path carpeta = Path.of(System.getProperty("user.home"), ".plantaciones");

    private Database() { }

    /** Cambia la carpeta de datos (lo usan los tests para trabajar con una base de datos temporal). */
    public static void usarCarpeta(Path nueva) {
        carpeta = nueva;
    }

    public static Path carpetaDatos() {
        return carpeta;
    }

    public static Connection conectar() throws SQLException {
        String url = "jdbc:sqlite:" + carpeta.resolve("plantaciones.db").toString().replace('\\', '/');
        Connection conexion = DriverManager.getConnection(url);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexion;
    }

    public static void inicializar() {
        try {
            Files.createDirectories(carpeta);
            try (Connection c = conectar(); Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS plantacion (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        descripcion TEXT,
                        ruta_foto TEXT,
                        hectareas REAL NOT NULL DEFAULT 0,
                        fecha_plantacion TEXT,
                        distancia_arboles REAL NOT NULL DEFAULT 3.0,
                        distancia_borde REAL NOT NULL DEFAULT 0
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS variedad (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
                        color TEXT NOT NULL DEFAULT '#e53935',
                        inicio TEXT,
                        fin TEXT
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS fila (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plantacion_id INTEGER NOT NULL REFERENCES plantacion(id) ON DELETE CASCADE,
                        numero INTEGER NOT NULL,
                        ancho REAL NOT NULL DEFAULT 4.0,
                        fecha_plantacion TEXT,
                        UNIQUE (plantacion_id, numero)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS tramo (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        fila_id INTEGER NOT NULL REFERENCES fila(id) ON DELETE CASCADE,
                        orden INTEGER NOT NULL,
                        variedad_id INTEGER NOT NULL REFERENCES variedad(id),
                        num_arboles INTEGER NOT NULL,
                        fecha_plantacion TEXT,
                        fecha_recogida TEXT,
                        UNIQUE (fila_id, orden)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS arbol (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        tramo_id INTEGER NOT NULL REFERENCES tramo(id) ON DELETE CASCADE,
                        posicion INTEGER NOT NULL,
                        estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                        fecha_plantacion TEXT,
                        UNIQUE (tramo_id, posicion)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS produccion (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        tramo_id INTEGER NOT NULL REFERENCES tramo(id) ON DELETE CASCADE,
                        anio INTEGER NOT NULL,
                        kilos_estimados REAL,
                        kilos_reales REAL,
                        UNIQUE (tramo_id, anio)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS produccion_anual (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        anio INTEGER NOT NULL,
                        variedad_id INTEGER NOT NULL REFERENCES variedad(id) ON DELETE CASCADE,
                        plantacion_id INTEGER REFERENCES plantacion(id) ON DELETE CASCADE,
                        kilos_estimados REAL,
                        kilos_reales REAL
                    )""");
                st.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ux_produccion_anual
                    ON produccion_anual (anio, variedad_id, COALESCE(plantacion_id, 0))""");
            }
        } catch (Exception e) {
            throw new DatosException("No se pudo inicializar la base de datos", e);
        }
    }
}