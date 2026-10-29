package com.example.plantaciones.db;

import com.example.plantaciones.model.Arbol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ArbolDao {

    public record Progreso(int total, int recogidos) {
        public boolean completo() {
            return total > 0 && total == recogidos;
        }
    }

    public List<Arbol> listarPorFila(long filaId) {
        String sql = "SELECT id, fila_id, posicion, recogido FROM arbol WHERE fila_id = ? ORDER BY posicion";
        List<Arbol> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, filaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Arbol(
                            rs.getLong("id"),
                            rs.getLong("fila_id"),
                            rs.getInt("posicion"),
                            rs.getInt("recogido") == 1));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los árboles", e);
        }
    }

    public void marcar(long arbolId, boolean recogido) {
        ejecutar("UPDATE arbol SET recogido = ? WHERE id = ?", recogido, arbolId);
    }

    public void marcarFila(long filaId, boolean recogido) {
        ejecutar("UPDATE arbol SET recogido = ? WHERE fila_id = ?", recogido, filaId);
    }

    public Progreso progreso(long plantacionId) {
        String sql = """
                SELECT COUNT(*), COALESCE(SUM(a.recogido), 0)
                FROM arbol a JOIN fila f ON a.fila_id = f.id
                WHERE f.plantacion_id = ?""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Progreso(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular el progreso", e);
        }
    }

    private void ejecutar(String sql, boolean recogido, long id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, recogido ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar el estado", e);
        }
    }
}