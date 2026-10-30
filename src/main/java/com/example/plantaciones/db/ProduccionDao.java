package com.example.plantaciones.db;

import com.example.plantaciones.model.Produccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class ProduccionDao {

    public record Totales(double estimados, double reales) { }

    public void guardarEstimado(long filaId, int anio, Double kilos) {
        guardar("kilos_estimados", filaId, anio, kilos);
    }

    public void guardarReal(long filaId, int anio, Double kilos) {
        guardar("kilos_reales", filaId, anio, kilos);
    }

    /** Estima cada fila de la plantación como kilosPorArbol x número de árboles de la fila. */
    public void aplicarMedia(long plantacionId, int anio, double kilosPorArbol) {
        String sql = """
                INSERT INTO produccion (fila_id, anio, kilos_estimados)
                SELECT id, ?, num_arboles * ? FROM fila WHERE plantacion_id = ?
                ON CONFLICT (fila_id, anio) DO UPDATE SET kilos_estimados = excluded.kilos_estimados""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setDouble(2, kilosPorArbol);
            ps.setLong(3, plantacionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo aplicar la media", e);
        }
    }

    /** Devuelve la producción de cada fila de la plantación en ese año (clave: id de la fila). */
    public Map<Long, Produccion> listarPorPlantacion(long plantacionId, int anio) {
        String sql = """
                SELECT p.id, p.fila_id, p.anio, p.kilos_estimados, p.kilos_reales
                FROM produccion p JOIN fila f ON p.fila_id = f.id
                WHERE f.plantacion_id = ? AND p.anio = ?""";
        Map<Long, Produccion> mapa = new HashMap<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            ps.setInt(2, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produccion p = new Produccion(
                            rs.getLong("id"),
                            rs.getLong("fila_id"),
                            rs.getInt("anio"),
                            leer(rs, "kilos_estimados"),
                            leer(rs, "kilos_reales"));
                    mapa.put(p.filaId(), p);
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer la producción", e);
        }
    }

    public Totales totales(long plantacionId, int anio) {
        String sql = """
                SELECT COALESCE(SUM(p.kilos_estimados), 0), COALESCE(SUM(p.kilos_reales), 0)
                FROM produccion p JOIN fila f ON p.fila_id = f.id
                WHERE f.plantacion_id = ? AND p.anio = ?""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            ps.setInt(2, anio);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Totales(rs.getDouble(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular los totales", e);
        }
    }

    private void guardar(String columna, long filaId, int anio, Double kilos) {
        // El nombre de la columna no puede ser un parámetro (?), por eso se concatena.
        // Es seguro porque solo lo pasan los dos métodos de arriba, con textos fijos.
        String sql = "INSERT INTO produccion (fila_id, anio, " + columna + ") VALUES (?, ?, ?) "
                + "ON CONFLICT (fila_id, anio) DO UPDATE SET " + columna + " = excluded." + columna;
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, filaId);
            ps.setInt(2, anio);
            if (kilos == null) {
                ps.setNull(3, Types.REAL);
            } else {
                ps.setDouble(3, kilos);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo guardar la producción", e);
        }
    }

    private static Double leer(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}