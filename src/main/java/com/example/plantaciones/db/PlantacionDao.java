package com.example.plantaciones.db;

import com.example.plantaciones.model.Plantacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlantacionDao {

    public long insertar(Plantacion p) {
        String sql = "INSERT INTO plantacion (nombre, descripcion, ruta_foto, hectareas) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.nombre());
            ps.setString(2, p.descripcion());
            ps.setString(3, p.rutaFoto());
            ps.setDouble(4, p.hectareas());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                return claves.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo insertar la plantación", e);
        }
    }

    public List<Plantacion> listar() {
        String sql = "SELECT id, nombre, descripcion, ruta_foto, hectareas FROM plantacion ORDER BY id";
        List<Plantacion> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Plantacion(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("ruta_foto"),
                        rs.getDouble("hectareas")));
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer las plantaciones", e);
        }
    }

    public void actualizar(Plantacion p) {
        String sql = "UPDATE plantacion SET nombre = ?, descripcion = ?, ruta_foto = ?, hectareas = ? WHERE id = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.nombre());
            ps.setString(2, p.descripcion());
            ps.setString(3, p.rutaFoto());
            ps.setDouble(4, p.hectareas());
            ps.setLong(5, p.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar la plantación", e);
        }
    }

    public void borrar(long id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM plantacion WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo borrar la plantación", e);
        }
    }
}