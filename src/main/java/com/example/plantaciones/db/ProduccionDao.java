package com.example.plantaciones.db;

import com.example.plantaciones.model.Produccion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProduccionDao {

    public void guardar(long filaId, int anio, double kilos) {
        String sql = """
                INSERT INTO produccion (fila_id, anio, kilos) VALUES (?, ?, ?)
                ON CONFLICT (fila_id, anio) DO UPDATE SET kilos = excluded.kilos""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, filaId);
            ps.setInt(2, anio);
            ps.setDouble(3, kilos);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo guardar la producción", e);
        }
    }

    public List<Produccion> listarPorFila(long filaId) {
        String sql = "SELECT id, fila_id, anio, kilos FROM produccion WHERE fila_id = ? ORDER BY anio DESC";
        List<Produccion> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, filaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Produccion(
                            rs.getLong("id"),
                            rs.getLong("fila_id"),
                            rs.getInt("anio"),
                            rs.getDouble("kilos")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer la producción", e);
        }
    }

    public double totalPlantacion(long plantacionId, int anio) {
        String sql = """
                SELECT COALESCE(SUM(p.kilos), 0)
                FROM produccion p JOIN fila f ON p.fila_id = f.id
                WHERE f.plantacion_id = ? AND p.anio = ?""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            ps.setInt(2, anio);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular el total", e);
        }
    }
}