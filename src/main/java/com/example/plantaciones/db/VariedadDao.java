package com.example.plantaciones.db;

import com.example.plantaciones.model.Variedad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VariedadDao {

    private static final String[] PALETA = {
            "#e53935", "#fb8c00", "#fdd835", "#43a047", "#1e88e5",
            "#8e24aa", "#00acc1", "#6d4c41", "#d81b60", "#7cb342"
    };

    private static final String COLUMNAS = "id, nombre, color, inicio, fin";

    public long insertar(Variedad v) {
        String sql = "INSERT INTO variedad (nombre, color, inicio, fin) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.nombre());
            ps.setString(2, v.color());
            ps.setString(3, diaATexto(v.inicio()));
            ps.setString(4, diaATexto(v.fin()));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                return claves.getLong(1);
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo insertar la variedad", e);
        }
    }

    public List<Variedad> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM variedad ORDER BY nombre";
        List<Variedad> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer las variedades", e);
        }
    }

    public Optional<Variedad> buscarPorNombre(String nombre) {
        String sql = "SELECT " + COLUMNAS + " FROM variedad WHERE nombre = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo buscar la variedad", e);
        }
    }

    public void actualizar(Variedad v) {
        String sql = "UPDATE variedad SET nombre = ?, color = ?, inicio = ?, fin = ? WHERE id = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.nombre());
            ps.setString(2, v.color());
            ps.setString(3, diaATexto(v.inicio()));
            ps.setString(4, diaATexto(v.fin()));
            ps.setLong(5, v.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar la variedad", e);
        }
    }

    /** Falla si la variedad está en uso en algún tramo. */
    public void borrar(long id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM variedad WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo borrar la variedad (¿está en uso?)", e);
        }
    }

    /** Devuelve el id de la variedad con ese nombre, creándola si no existe. */
    public long obtenerOCrear(String nombre) {
        Optional<Variedad> existente = buscarPorNombre(nombre);
        if (existente.isPresent()) {
            return existente.get().id();
        }
        String color = PALETA[listar().size() % PALETA.length];
        return insertar(new Variedad(0, nombre.trim(), color, null, null));
    }

    private static Variedad mapear(ResultSet rs) throws SQLException {
        return new Variedad(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("color"),
                textoADia(rs.getString("inicio")),
                textoADia(rs.getString("fin")));
    }

    private static String diaATexto(MonthDay dia) {
        return dia == null ? null : dia.toString();
    }

    private static MonthDay textoADia(String texto) {
        return texto == null ? null : MonthDay.parse(texto);
    }
}