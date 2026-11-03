package com.example.plantaciones.db;

import com.example.plantaciones.model.Tramo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TramoDao {

    private static final String COLUMNAS =
            "t.id, t.fila_id, t.orden, t.variedad_id, t.num_arboles, t.fecha_plantacion, t.fecha_recogida";

    public long insertar(Tramo t) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                long tramoId;
                String sql = "INSERT INTO tramo (fila_id, orden, variedad_id, num_arboles, "
                        + "fecha_plantacion, fecha_recogida) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, t.filaId());
                    ps.setInt(2, t.orden());
                    ps.setLong(3, t.variedadId());
                    ps.setInt(4, t.numArboles());
                    Sql.ponerFecha(ps, 5, t.fechaPlantacion());
                    Sql.ponerFecha(ps, 6, t.fechaRecogida());
                    ps.executeUpdate();
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        claves.next();
                        tramoId = claves.getLong(1);
                    }
                }
                crearArboles(c, tramoId, 1, t.numArboles());
                c.commit();
                return tramoId;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo insertar el tramo", e);
        }
    }

    public List<Tramo> listarPorFila(long filaId) {
        String sql = "SELECT " + COLUMNAS + " FROM tramo t WHERE t.fila_id = ? ORDER BY t.orden";
        return consultar(sql, filaId);
    }

    public List<Tramo> listarPorPlantacion(long plantacionId) {
        String sql = "SELECT " + COLUMNAS + " FROM tramo t JOIN fila f ON t.fila_id = f.id "
                + "WHERE f.plantacion_id = ? ORDER BY f.numero, t.orden";
        return consultar(sql, plantacionId);
    }

    public void actualizar(Tramo t) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                int anteriores;
                try (PreparedStatement ps = c.prepareStatement("SELECT num_arboles FROM tramo WHERE id = ?")) {
                    ps.setLong(1, t.id());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        anteriores = rs.getInt(1);
                    }
                }

                String sql = "UPDATE tramo SET orden = ?, variedad_id = ?, num_arboles = ?, "
                        + "fecha_plantacion = ?, fecha_recogida = ? WHERE id = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, t.orden());
                    ps.setLong(2, t.variedadId());
                    ps.setInt(3, t.numArboles());
                    Sql.ponerFecha(ps, 4, t.fechaPlantacion());
                    Sql.ponerFecha(ps, 5, t.fechaRecogida());
                    ps.setLong(6, t.id());
                    ps.executeUpdate();
                }

                if (t.numArboles() > anteriores) {
                    crearArboles(c, t.id(), anteriores + 1, t.numArboles());
                } else if (t.numArboles() < anteriores) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM arbol WHERE tramo_id = ? AND posicion > ?")) {
                        ps.setLong(1, t.id());
                        ps.setInt(2, t.numArboles());
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo actualizar el tramo", e);
        }
    }

    public void borrar(long id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tramo WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatosException("No se pudo borrar el tramo", e);
        }
    }

    private List<Tramo> consultar(String sql, long parametro) {
        List<Tramo> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Tramo(
                            rs.getLong("id"),
                            rs.getLong("fila_id"),
                            rs.getInt("orden"),
                            rs.getLong("variedad_id"),
                            rs.getInt("num_arboles"),
                            Sql.leerFecha(rs, "fecha_plantacion"),
                            Sql.leerFecha(rs, "fecha_recogida")));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los tramos", e);
        }
    }

    private void crearArboles(Connection c, long tramoId, int desde, int hasta) throws SQLException {
        String sql = "INSERT INTO arbol (tramo_id, posicion) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int posicion = desde; posicion <= hasta; posicion++) {
                ps.setLong(1, tramoId);
                ps.setInt(2, posicion);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}