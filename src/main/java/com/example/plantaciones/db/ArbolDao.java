package com.example.plantaciones.db;

import com.example.plantaciones.model.Arbol;
import com.example.plantaciones.model.EstadoArbol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ArbolDao {

    /** total = árboles vivos (los muertos no cuentan en el porcentaje). */
    public record Progreso(int total, int recogidos, int muertos) {
        public boolean completo() {
            return total > 0 && total == recogidos;
        }
    }

    public List<Arbol> listarPorTramo(long tramoId) {
        String sql = "SELECT id, tramo_id, posicion, estado, fecha_plantacion "
                + "FROM arbol WHERE tramo_id = ? ORDER BY posicion";
        List<Arbol> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tramoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Arbol(
                            rs.getLong("id"),
                            rs.getLong("tramo_id"),
                            rs.getInt("posicion"),
                            EstadoArbol.valueOf(rs.getString("estado")),
                            Sql.leerFecha(rs, "fecha_plantacion")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los árboles", e);
        }
    }

    public void cambiarEstado(long arbolId, EstadoArbol estado) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE arbol SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado.name());
            ps.setLong(2, arbolId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo cambiar el estado del árbol", e);
        }
    }

    /** Cambia el estado de todos los árboles del tramo, salvo los muertos. */
    public void marcarTramo(long tramoId, EstadoArbol estado) {
        String sql = "UPDATE arbol SET estado = ? WHERE tramo_id = ? AND estado <> 'MUERTO'";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setLong(2, tramoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar el tramo", e);
        }
    }

    /** Fecha de plantación propia del árbol (null = hereda la de su tramo, fila o plantación). */
    public void ponerFecha(long arbolId, LocalDate fecha) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("UPDATE arbol SET fecha_plantacion = ? WHERE id = ?")) {
            Sql.ponerFecha(ps, 1, fecha);
            ps.setLong(2, arbolId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo guardar la fecha del árbol", e);
        }
    }

    public Progreso progreso(long plantacionId) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN a.estado <> 'MUERTO' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN a.estado = 'RECOGIDO' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN a.estado = 'MUERTO' THEN 1 ELSE 0 END), 0)
                FROM arbol a
                JOIN tramo t ON a.tramo_id = t.id
                JOIN fila f ON t.fila_id = f.id
                WHERE f.plantacion_id = ?""";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Progreso(rs.getInt(1), rs.getInt(2), rs.getInt(3));
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular el progreso", e);
        }
    }
}