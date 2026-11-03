package com.example.plantaciones.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

final class Sql {

    private Sql() { }

    static void ponerFecha(PreparedStatement ps, int indice, LocalDate fecha) throws SQLException {
        if (fecha == null) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, fecha.toString());
        }
    }

    static LocalDate leerFecha(ResultSet rs, String columna) throws SQLException {
        String texto = rs.getString(columna);
        return texto == null ? null : LocalDate.parse(texto);
    }

    static void ponerDecimal(PreparedStatement ps, int indice, Double valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.REAL);
        } else {
            ps.setDouble(indice, valor);
        }
    }

    static Double leerDecimal(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }

    static void ponerLargo(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setLong(indice, valor);
        }
    }

    static Long leerLargo(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }
}