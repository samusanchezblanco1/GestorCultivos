package com.example.plantaciones.db;

import com.example.plantaciones.model.Fila;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FilaDao {

    public long insertar(Fila f) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                long filaId;
                String sql = "INSERT INTO fila (plantacion_id, numero, variedad, num_arboles, fecha_recogida) "
                        + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, f.plantacionId());
                    ps.setInt(2, f.numero());
                    ps.setString(3, f.variedad());
                    ps.setInt(4, f.numArboles());
                    ps.setString(5, fechaATexto(f.fechaRecogida()));
                    ps.executeUpdate();
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        claves.next();
                        filaId = claves.getLong(1);
                    }
                }
                crearArboles(c, filaId, 1, f.numArboles());
                c.commit();
                return filaId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo insertar la fila", e);
        }
    }

    public List<Fila> listarPorPlantacion(long plantacionId) {
        String sql = "SELECT id, plantacion_id, numero, variedad, num_arboles, fecha_recogida "
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
                            rs.getString("variedad"),
                            rs.getInt("num_arboles"),
                            textoAFecha(rs.getString("fecha_recogida"))));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer las filas", e);
        }
    }

    public void actualizar(Fila f) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                int anteriores;
                try (PreparedStatement ps = c.prepareStatement("SELECT num_arboles FROM fila WHERE id = ?")) {
                    ps.setLong(1, f.id());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        anteriores = rs.getInt(1);
                    }
                }

                String sql = "UPDATE fila SET numero = ?, variedad = ?, num_arboles = ?, fecha_recogida = ? WHERE id = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, f.numero());
                    ps.setString(2, f.variedad());
                    ps.setInt(3, f.numArboles());
                    ps.setString(4, fechaATexto(f.fechaRecogida()));
                    ps.setLong(5, f.id());
                    ps.executeUpdate();
                }

                if (f.numArboles() > anteriores) {
                    crearArboles(c, f.id(), anteriores + 1, f.numArboles());
                } else if (f.numArboles() < anteriores) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM arbol WHERE fila_id = ? AND posicion > ?")) {
                        ps.setLong(1, f.id());
                        ps.setInt(2, f.numArboles());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
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

    private void crearArboles(Connection c, long filaId, int desde, int hasta) throws SQLException {
        String sql = "INSERT INTO arbol (fila_id, posicion, recogido) VALUES (?, ?, 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int posicion = desde; posicion <= hasta; posicion++) {
                ps.setLong(1, filaId);
                ps.setInt(2, posicion);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static String fechaATexto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static LocalDate textoAFecha(String texto) {
        return texto == null ? null : LocalDate.parse(texto);
    }
}