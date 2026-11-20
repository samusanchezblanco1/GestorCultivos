package com.example.plantaciones.db;

import com.example.plantaciones.model.EstadoArbol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EstadisticasDao {

    /** Filtros combinables. null = "todos". El año solo afecta a los kilos. */
    public record Filtro(Long plantacionId, Long variedadId, Integer anio) {
        public static Filtro todo() {
            return new Filtro(null, null, null);
        }
    }

    public record ResumenArboles(int vivos, int recogidos, int muertos, int sinFecha,
                                 Map<Integer, Integer> plantadosPorAnio,
                                 Double edadMedia, Double edadMinima, Double edadMaxima) {
        public double porcentajeRecogido() {
            return vivos == 0 ? 0 : 100.0 * recogidos / vivos;
        }
    }

    public record KilosAnio(int anio, Double estimados, Double reales) { }

    public record ResumenKilos(List<KilosAnio> porAnio, double totalEstimados, double totalReales,
                               double mediaRealesPorAnio, boolean hayTotalesSinAsignar) { }

    private record Clave(int anio, long variedadId, Long plantacionId) { }

    private record Valores(Double estimados, Double reales) {
        static final Valores VACIO = new Valores(null, null);

        Valores sumar(Valores otro) {
            return new Valores(suma(estimados, otro.estimados), suma(reales, otro.reales));
        }

        /** Usa los valores propios y, donde falten, los del otro. */
        Valores preferirSobre(Valores otro) {
            return new Valores(
                    estimados != null ? estimados : otro.estimados,
                    reales != null ? reales : otro.reales);
        }

        private static Double suma(Double a, Double b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            return a + b;
        }
    }

    // ---------------------------------------------------------------- árboles

    public ResumenArboles arboles(Filtro filtro) {
        return arboles(filtro, LocalDate.now());
    }

    public ResumenArboles arboles(Filtro filtro, LocalDate hoy) {
        String sql = """
                SELECT a.estado,
                       COALESCE(a.fecha_plantacion, t.fecha_plantacion,
                                f.fecha_plantacion, p.fecha_plantacion) AS fecha
                FROM arbol a
                JOIN tramo t ON a.tramo_id = t.id
                JOIN fila f ON t.fila_id = f.id
                JOIN plantacion p ON f.plantacion_id = p.id
                WHERE (? IS NULL OR p.id = ?) AND (? IS NULL OR t.variedad_id = ?)""";

        int vivos = 0;
        int recogidos = 0;
        int muertos = 0;
        int sinFecha = 0;
        int conFecha = 0;
        double sumaEdades = 0;
        Double minima = null;
        Double maxima = null;
        Map<Integer, Integer> plantadosPorAnio = new TreeMap<>();

        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            Sql.ponerLargo(ps, 1, filtro.plantacionId());
            Sql.ponerLargo(ps, 2, filtro.plantacionId());
            Sql.ponerLargo(ps, 3, filtro.variedadId());
            Sql.ponerLargo(ps, 4, filtro.variedadId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EstadoArbol estado = EstadoArbol.valueOf(rs.getString("estado"));
                    if (estado == EstadoArbol.MUERTO) {
                        muertos++;
                        continue;
                    }
                    vivos++;
                    if (estado == EstadoArbol.RECOGIDO) {
                        recogidos++;
                    }

                    LocalDate fecha = Sql.leerFecha(rs, "fecha");
                    if (fecha == null) {
                        sinFecha++;
                        continue;
                    }
                    conFecha++;
                    plantadosPorAnio.merge(fecha.getYear(), 1, Integer::sum);
                    double edad = ChronoUnit.DAYS.between(fecha, hoy) / 365.25;
                    sumaEdades += edad;
                    minima = minima == null ? edad : Math.min(minima, edad);
                    maxima = maxima == null ? edad : Math.max(maxima, edad);
                }
            }
        } catch (SQLException e) {
            throw new DatosException("No se pudo calcular las estadísticas de árboles", e);
        }

        Double media = conFecha == 0 ? null : sumaEdades / conFecha;
        return new ResumenArboles(vivos, recogidos, muertos, sinFecha,
                plantadosPorAnio, media, minima, maxima);
    }

    // ----------------------------------------------------------------- kilos

    public ResumenKilos kilos(Filtro filtro) {
        Map<Clave, Valores> detalle = leerDetalle();
        Map<Clave, Valores> anuales = leerAnuales();

        // 1) Valor de cada (año, variedad, plantación): su total anual si existe, y si no, el detalle de tramos.
        Set<Clave> claves = new HashSet<>(detalle.keySet());
        for (Clave k : anuales.keySet()) {
            if (k.plantacionId() != null) {
                claves.add(k);
            }
        }
        Map<Clave, Valores> porPlantacion = new HashMap<>();
        for (Clave k : claves) {
            Valores total = anuales.getOrDefault(k, Valores.VACIO);
            Valores tramos = detalle.getOrDefault(k, Valores.VACIO);
            porPlantacion.put(k, total.preferirSobre(tramos));
        }

        Map<Integer, Valores> resultado = new TreeMap<>();
        if (filtro.plantacionId() != null) {
            // Vista de una plantación: los totales "de toda la explotación" no se pueden atribuir.
            for (Map.Entry<Clave, Valores> e : porPlantacion.entrySet()) {
                Clave k = e.getKey();
                if (filtro.plantacionId().equals(k.plantacionId()) && coincide(filtro, k)) {
                    resultado.merge(k.anio(), e.getValue(), Valores::sumar);
                }
            }
        } else {
            // 2) Vista global: para cada (año, variedad) manda el total de la explotación; si no, la suma de plantaciones.
            Map<Clave, Valores> sumas = new HashMap<>();
            for (Map.Entry<Clave, Valores> e : porPlantacion.entrySet()) {
                Clave k = e.getKey();
                sumas.merge(new Clave(k.anio(), k.variedadId(), null), e.getValue(), Valores::sumar);
            }
            Set<Clave> globales = new HashSet<>(sumas.keySet());
            for (Clave k : anuales.keySet()) {
                if (k.plantacionId() == null) {
                    globales.add(k);
                }
            }
            for (Clave k : globales) {
                if (coincide(filtro, k)) {
                    Valores valor = anuales.getOrDefault(k, Valores.VACIO)
                            .preferirSobre(sumas.getOrDefault(k, Valores.VACIO));
                    resultado.merge(k.anio(), valor, Valores::sumar);
                }
            }
        }

        List<KilosAnio> lista = new ArrayList<>();
        double totalEstimados = 0;
        double totalReales = 0;
        int aniosConReales = 0;
        for (Map.Entry<Integer, Valores> e : resultado.entrySet()) {
            Valores v = e.getValue();
            lista.add(new KilosAnio(e.getKey(), v.estimados(), v.reales()));
            if (v.estimados() != null) {
                totalEstimados += v.estimados();
            }
            if (v.reales() != null) {
                totalReales += v.reales();
                aniosConReales++;
            }
        }
        double media = aniosConReales == 0 ? 0 : totalReales / aniosConReales;

        boolean sinAsignar = filtro.plantacionId() != null
                && anuales.keySet().stream().anyMatch(k -> k.plantacionId() == null && coincide(filtro, k));

        return new ResumenKilos(lista, totalEstimados, totalReales, media, sinAsignar);
    }

    /** Años que tienen algún dato de producción (del más reciente al más antiguo). */
    public List<Integer> aniosConDatos() {
        String sql = "SELECT anio FROM produccion UNION SELECT anio FROM produccion_anual ORDER BY anio DESC";
        List<Integer> anios = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                anios.add(rs.getInt(1));
            }
            return anios;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los años", e);
        }
    }

    private static boolean coincide(Filtro filtro, Clave k) {
        boolean variedadOk = filtro.variedadId() == null || filtro.variedadId() == k.variedadId();
        boolean anioOk = filtro.anio() == null || filtro.anio() == k.anio();
        return variedadOk && anioOk;
    }

    private Map<Clave, Valores> leerDetalle() {
        String sql = """
                SELECT pr.anio, t.variedad_id, f.plantacion_id,
                       SUM(pr.kilos_estimados) AS estimados, SUM(pr.kilos_reales) AS reales
                FROM produccion pr
                JOIN tramo t ON pr.tramo_id = t.id
                JOIN fila f ON t.fila_id = f.id
                GROUP BY pr.anio, t.variedad_id, f.plantacion_id""";
        Map<Clave, Valores> mapa = new HashMap<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Clave clave = new Clave(rs.getInt("anio"), rs.getLong("variedad_id"), rs.getLong("plantacion_id"));
                mapa.put(clave, new Valores(Sql.leerDecimal(rs, "estimados"), Sql.leerDecimal(rs, "reales")));
            }
            return mapa;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer el detalle de producción", e);
        }
    }

    private Map<Clave, Valores> leerAnuales() {
        String sql = "SELECT anio, variedad_id, plantacion_id, kilos_estimados, kilos_reales FROM produccion_anual";
        Map<Clave, Valores> mapa = new HashMap<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Clave clave = new Clave(rs.getInt("anio"), rs.getLong("variedad_id"),
                        Sql.leerLargo(rs, "plantacion_id"));
                mapa.put(clave, new Valores(
                        Sql.leerDecimal(rs, "kilos_estimados"), Sql.leerDecimal(rs, "kilos_reales")));
            }
            return mapa;
        } catch (SQLException e) {
            throw new DatosException("No se pudo leer los totales anuales", e);
        }
    }
}