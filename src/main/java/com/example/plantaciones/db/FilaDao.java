package com.example.plantaciones.db;

import com.example.plantaciones.model.Fila;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FilaDao {

    public long insertar(Fila f) {
        String sql = "INSERT INTO fila (plantacion_id, numero, ancho, fecha_plantacion) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, f.plantacionId());
            ps.setInt(2, f.numero());
            ps.setDouble(3, f.ancho());
            Sql.ponerFecha(ps, 4, f.fechaPlantacion());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                return claves.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo insertar la fila", e);
        }
    }

    public List<Fila> listarPorPlantacion(long plantacionId) {
        String sql = "SELECT id, plantacion_id, numero, ancho, fecha_plantacion "
                + "FROM fila WHERE plantacion_id = ? ORDER BY numero";
        List<Fila> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, plantacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Fila(
                            rs.getLong("id"),
                            rs.getLong("plantacion_id"),
                            rs.getInt("numero"),
                            rs.getDouble("ancho"),
                            Sql.leerFecha(rs, "fecha_plantacion")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer las filas", e);
        }
    }

    public void actualizar(Fila f) {
        String sql = "UPDATE fila SET numero = ?, ancho = ?, fecha_plantacion = ? WHERE id = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, f.numero());
            ps.setDouble(2, f.ancho());
            Sql.ponerFecha(ps, 3, f.fechaPlantacion());
            ps.setLong(4, f.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar la fila", e);
        }
    }

    public void borrar(long id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM fila WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo borrar la fila", e);
        }
    }
}