package com.example.plantaciones.db;

import com.example.plantaciones.model.Produccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ProduccionDao {

    public record Totales(double estimados, double reales) { }

    public void guardarEstimado(long tramoId, int anio, Double kilos) {
        guardar("kilos_estimados", tramoId, anio, kilos);
    }

    public void guardarReal(long tramoId, int anio, Double kilos) {
        guardar("kilos_reales", tramoId, anio, kilos);
    }

    /** Estima cada tramo como kilosPorArbol x árboles vivos del tramo. */
    public void aplicarMedia(long plantacionId, int anio, double kilosPorArbol) {
        String sql = """
                INSERT INTO produccion (tramo_id, anio, kilos_estimados)
                SELECT t.id, ?,
                       ? * (SELECT COUNT(*) FROM arbol a WHERE a.tramo_id = t.id AND a.estado <> 'MUERTO')
                FROM tramo t JOIN fila f ON t.fila_id = f.id
                WHERE f.plantacion_id = ?
                ON CONFLICT (tramo_id, anio) DO UPDATE SET kilos_estimados = excluded.kilos_estimados""";
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

    /** Producción de cada tramo de la plantación en ese año (clave: id del tramo). */
    public Map<Long, Produccion> listarPorPlantacion(long plantacionId, int anio) {
        String sql = """
                SELECT p.id, p.tramo_id, p.anio, p.kilos_estimados, p.kilos_reales
                FROM produccion p
                JOIN tramo t ON p.tramo_id = t.id
                JOIN fila f ON t.fila_id = f.id
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
                            rs.getLong("tramo_id"),
                            rs.getInt("anio"),
                            Sql.leerDecimal(rs, "kilos_estimados"),
                            Sql.leerDecimal(rs, "kilos_reales"));
                    mapa.put(p.tramoId(), p);
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
                FROM produccion p
                JOIN tramo t ON p.tramo_id = t.id
                JOIN fila f ON t.fila_id = f.id
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

    private void guardar(String columna, long tramoId, int anio, Double kilos) {
        // El nombre de la columna no puede ser un parámetro (?); solo lo pasan métodos de esta clase.
        String sql = "INSERT INTO produccion (tramo_id, anio, " + columna + ") VALUES (?, ?, ?) "
                + "ON CONFLICT (tramo_id, anio) DO UPDATE SET " + columna + " = excluded." + columna;
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tramoId);
            ps.setInt(2, anio);
            Sql.ponerDecimal(ps, 3, kilos);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo guardar la producción", e);
        }
    }
}