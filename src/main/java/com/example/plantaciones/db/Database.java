package com.example.plantaciones.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final Path CARPETA = Path.of(System.getProperty("user.home"), ".plantaciones");
    private static final String URL =
            "jdbc:sqlite:" + CARPETA.resolve("plantaciones.db").toString().replace('\\', '/');

    private Database() { }

    public static Path carpetaDatos() {
        return CARPETA;
    }

    public static Connection conectar() throws SQLException {
        Connection conexion = DriverManager.getConnection(URL);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conexion;
    }

    public static void inicializar() {
        try {
            Files.createDirectories(CARPETA);
            try (Connection c = conectar(); Statement st = c.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS plantacion (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nombre TEXT NOT NULL,
                        descripcion TEXT,
                        ruta_foto TEXT,
                        hectareas REAL NOT NULL DEFAULT 0
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS fila (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        plantacion_id INTEGER NOT NULL REFERENCES plantacion(id) ON DELETE CASCADE,
                        numero INTEGER NOT NULL,
                        variedad TEXT NOT NULL,
                        num_arboles INTEGER NOT NULL,
                        fecha_recogida TEXT,
                        UNIQUE (plantacion_id, numero)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS arbol (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        fila_id INTEGER NOT NULL REFERENCES fila(id) ON DELETE CASCADE,
                        posicion INTEGER NOT NULL,
                        recogido INTEGER NOT NULL DEFAULT 0,
                        UNIQUE (fila_id, posicion)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS produccion (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        fila_id INTEGER NOT NULL REFERENCES fila(id) ON DELETE CASCADE,
                        anio INTEGER NOT NULL,
                        kilos_estimados REAL,
                        kilos_reales REAL,
                        UNIQUE (fila_id, anio)
                    )""");
                st.execute("""
                    CREATE TABLE IF NOT EXISTS variedad (
                        nombre TEXT PRIMARY KEY COLLATE NOCASE,
                        inicio TEXT,
                        fin TEXT
                    )""");
            }
        } catch (Exception e) {
            throw new DatosException("No se pudo inicializar la base de datos", e);
        }
    }
}