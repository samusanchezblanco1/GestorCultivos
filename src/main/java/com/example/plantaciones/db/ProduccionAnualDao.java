package com.example.plantaciones.db;

import com.example.plantaciones.model.ProduccionAnual;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Totales anuales por variedad. plantacionId = null significa "toda la explotación". */
public class ProduccionAnualDao {

    public void guardarEstimado(int anio, long variedadId, Long plantacionId, Double kilos) {
        guardar("kilos_estimados", anio, variedadId, plantacionId, kilos);
    }

    public void guardarReal(int anio, long variedadId, Long plantacionId, Double kilos) {
        guardar("kilos_reales", anio, variedadId, plantacionId, kilos);
    }

    public List<ProduccionAnual> listar() {
        String sql = "SELECT id, anio, variedad_id, plantacion_id, kilos_estimados, kilos_reales "
                + "FROM produccion_anual ORDER BY anio, variedad_id";
        List<ProduccionAnual> lista = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new ProduccionAnual(
                        rs.getLong("id"),
                        rs.getInt("anio"),
                        rs.getLong("variedad_id"),
                        Sql.leerLargo(rs, "plantacion_id"),
                        Sql.leerDecimal(rs, "kilos_estimados"),
                        Sql.leerDecimal(rs, "kilos_reales")));
            }
            return lista;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los totales anuales", e);
        }
    }

    private void guardar(String columna, int anio, long variedadId, Long plantacionId, Double kilos) {
        String actualizar = "UPDATE produccion_anual SET " + columna
                + " = ? WHERE anio = ? AND variedad_id = ? AND plantacion_id IS ?";
        String insertar = "INSERT INTO produccion_anual (anio, variedad_id, plantacion_id, " + columna
                + ") VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar()) {
            int modificadas;
            try (PreparedStatement ps = c.prepareStatement(actualizar)) {
                Sql.ponerDecimal(ps, 1, kilos);
                ps.setInt(2, anio);
                ps.setLong(3, variedadId);
                Sql.ponerLargo(ps, 4, plantacionId);
                modificadas = ps.executeUpdate();
            }
            if (modificadas == 0) {
                try (PreparedStatement ps = c.prepareStatement(insertar)) {
                    ps.setInt(1, anio);
                    ps.setLong(2, variedadId);
                    Sql.ponerLargo(ps, 3, plantacionId);
                    Sql.ponerDecimal(ps, 4, kilos);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo guardar el total anual", e);
        }
    }
}