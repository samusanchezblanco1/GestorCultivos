package com.example.plantaciones.db;

import com.example.plantaciones.db.EstadisticasDao.Filtro;
import com.example.plantaciones.db.EstadisticasDao.KilosAnio;
import com.example.plantaciones.db.EstadisticasDao.ResumenArboles;
import com.example.plantaciones.db.EstadisticasDao.ResumenKilos;
import com.example.plantaciones.model.Arbol;
import com.example.plantaciones.model.EstadoArbol;
import com.example.plantaciones.model.Fila;
import com.example.plantaciones.model.Plantacion;
import com.example.plantaciones.model.Tramo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadisticasDaoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 6);

    private final EstadisticasDao estadisticas = new EstadisticasDao();
    private long plantacionA;
    private long plantacionB;
    private long fuji;
    private long gala;

    @BeforeEach
    void prepararDatos(@TempDir Path carpeta) {
        Database.usarCarpeta(carpeta);
        Database.inicializar();

        VariedadDao variedades = new VariedadDao();
        PlantacionDao plantaciones = new PlantacionDao();
        FilaDao filas = new FilaDao();
        TramoDao tramos = new TramoDao();
        ArbolDao arboles = new ArbolDao();
        ProduccionDao producciones = new ProduccionDao();
        ProduccionAnualDao anuales = new ProduccionAnualDao();

        fuji = variedades.obtenerOCrear("Fuji");
        gala = variedades.obtenerOCrear("Gala");

        // A tiene fecha de plantación (2015); B no tiene ninguna
        plantacionA = plantaciones.insertar(
                new Plantacion(0, "A", "", null, 2.0, LocalDate.of(2015, 3, 1), 3.0, 0.0));
        plantacionB = plantaciones.insertar(new Plantacion(0, "B", "", null, 1.0));

        long filaA = filas.insertar(new Fila(0, plantacionA, 1, 4.0, null));
        long tramoA1 = tramos.insertar(new Tramo(0, filaA, 1, fuji, 4, LocalDate.of(2018, 3, 15), null));
        long tramoA2 = tramos.insertar(new Tramo(0, filaA, 2, gala, 3, null, null));
        long filaB = filas.insertar(new Fila(0, plantacionB, 1, 4.0, null));
        long tramoB1 = tramos.insertar(new Tramo(0, filaB, 1, fuji, 2, null, null));

        // Tramo A1 (Fuji, 2018): 1 muerto, 2 recogidos, 1 pendiente
        List<Arbol> a1 = arboles.listarPorTramo(tramoA1);
        arboles.cambiarEstado(a1.get(0).id(), EstadoArbol.MUERTO);
        arboles.cambiarEstado(a1.get(1).id(), EstadoArbol.RECOGIDO);
        arboles.cambiarEstado(a1.get(2).id(), EstadoArbol.RECOGIDO);

        // Tramo A2 (Gala, hereda 2015): un árbol con fecha propia (2024) y otro recogido
        List<Arbol> a2 = arboles.listarPorTramo(tramoA2);
        arboles.ponerFecha(a2.get(0).id(), LocalDate.of(2024, 4, 1));
        arboles.cambiarEstado(a2.get(1).id(), EstadoArbol.RECOGIDO);

        // Tramo B1 (Fuji, sin ninguna fecha): uno recogido
        List<Arbol> b1 = arboles.listarPorTramo(tramoB1);
        arboles.cambiarEstado(b1.get(0).id(), EstadoArbol.RECOGIDO);

        // Kilos por tramo (detalle)
        producciones.guardarEstimado(tramoA1, 2025, 900.0);
        producciones.guardarReal(tramoA1, 2025, 1000.0);
        producciones.guardarEstimado(tramoA1, 2026, 1100.0);
        producciones.guardarReal(tramoA2, 2025, 400.0);
        producciones.guardarReal(tramoB1, 2025, 500.0);
        producciones.guardarEstimado(tramoB1, 2026, 300.0);

        // Totales anuales: Fuji de toda la explotación en 2025 (1700, mejor dato que la suma 1500)
        // y Gala de la plantación A en 2026 (sin detalle por tramo)
        anuales.guardarReal(2025, fuji, null, 1700.0);
        anuales.guardarReal(2026, gala, plantacionA, 250.0);
    }

    @Test
    void arbolesDeTodasLasPlantaciones() {
        ResumenArboles r = estadisticas.arboles(Filtro.todo(), HOY);

        assertEquals(8, r.vivos());
        assertEquals(4, r.recogidos());
        assertEquals(1, r.muertos());
        assertEquals(2, r.sinFecha());
        assertEquals(50.0, r.porcentajeRecogido(), 0.001);
        assertEquals(Map.of(2015, 2, 2018, 3, 2024, 1), r.plantadosPorAnio());
        assertEquals(8.65, r.edadMedia(), 0.01);
        assertEquals(2.60, r.edadMinima(), 0.01);
        assertEquals(11.69, r.edadMaxima(), 0.01);
    }

    @Test
    void arbolesDeUnaVariedad() {
        ResumenArboles r = estadisticas.arboles(new Filtro(null, fuji, null), HOY);

        assertEquals(5, r.vivos());
        assertEquals(3, r.recogidos());
        assertEquals(1, r.muertos());
        assertEquals(2, r.sinFecha());
        assertEquals(Map.of(2018, 3), r.plantadosPorAnio());
        assertEquals(8.65, r.edadMedia(), 0.01);
        assertEquals(8.65, r.edadMinima(), 0.01);
        assertEquals(8.65, r.edadMaxima(), 0.01);
    }

    @Test
    void arbolesDeUnaPlantacionSinFechas() {
        ResumenArboles r = estadisticas.arboles(new Filtro(plantacionB, null, null), HOY);

        assertEquals(2, r.vivos());
        assertEquals(1, r.recogidos());
        assertEquals(2, r.sinFecha());
        assertTrue(r.plantadosPorAnio().isEmpty());
        assertNull(r.edadMedia());
    }

    @Test
    void kilosGlobalesMandaElTotalMasAgregado() {
        ResumenKilos k = estadisticas.kilos(Filtro.todo());

        assertEquals(2, k.porAnio().size());
        KilosAnio y2025 = k.porAnio().get(0);
        assertEquals(2025, y2025.anio());
        assertEquals(900.0, y2025.estimados(), 0.001);
        assertEquals(2100.0, y2025.reales(), 0.001);   // Fuji 1700 (total anual) + Gala 400

        KilosAnio y2026 = k.porAnio().get(1);
        assertEquals(2026, y2026.anio());
        assertEquals(1400.0, y2026.estimados(), 0.001);
        assertEquals(250.0, y2026.reales(), 0.001);

        assertEquals(2300.0, k.totalEstimados(), 0.001);
        assertEquals(2350.0, k.totalReales(), 0.001);
        assertEquals(1175.0, k.mediaRealesPorAnio(), 0.001);
        assertFalse(k.hayTotalesSinAsignar());
    }

    @Test
    void kilosDeUnaPlantacion() {
        ResumenKilos k = estadisticas.kilos(new Filtro(plantacionA, null, null));

        assertEquals(2, k.porAnio().size());
        assertEquals(900.0, k.porAnio().get(0).estimados(), 0.001);
        assertEquals(1400.0, k.porAnio().get(0).reales(), 0.001);   // detalle: Fuji 1000 + Gala 400
        assertEquals(1100.0, k.porAnio().get(1).estimados(), 0.001);
        assertEquals(250.0, k.porAnio().get(1).reales(), 0.001);    // total anual de la plantación

        assertEquals(2000.0, k.totalEstimados(), 0.001);
        assertEquals(1650.0, k.totalReales(), 0.001);
        assertEquals(825.0, k.mediaRealesPorAnio(), 0.001);
        assertTrue(k.hayTotalesSinAsignar());                        // el total de Fuji 2025 no se atribuye
    }

    @Test
    void kilosDeUnaVariedad() {
        ResumenKilos k = estadisticas.kilos(new Filtro(null, fuji, null));

        assertEquals(2, k.porAnio().size());
        assertEquals(900.0, k.porAnio().get(0).estimados(), 0.001);
        assertEquals(1700.0, k.porAnio().get(0).reales(), 0.001);
        assertEquals(1400.0, k.porAnio().get(1).estimados(), 0.001);
        assertNull(k.porAnio().get(1).reales());

        assertEquals(2300.0, k.totalEstimados(), 0.001);
        assertEquals(1700.0, k.totalReales(), 0.001);
        assertEquals(1700.0, k.mediaRealesPorAnio(), 0.001);         // solo cuenta años con kilos reales
    }

    @Test
    void kilosDeUnAnio() {
        ResumenKilos k = estadisticas.kilos(new Filtro(null, null, 2026));

        assertEquals(1, k.porAnio().size());
        assertEquals(1400.0, k.porAnio().get(0).estimados(), 0.001);
        assertEquals(250.0, k.porAnio().get(0).reales(), 0.001);
    }

    @Test
    void aniosConDatos() {
        assertEquals(List.of(2026, 2025), estadisticas.aniosConDatos());
    }
}