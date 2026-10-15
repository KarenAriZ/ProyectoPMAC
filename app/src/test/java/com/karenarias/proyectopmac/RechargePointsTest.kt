package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.data.recharge.*
import org.junit.Assert.*
import org.junit.Test

/** HU-006: distancias, orden por cercanía y alertas de paraderos próximos. */
class RechargePointsTest {

    private val central = RechargePointsRepository.points.first { it.id == "central" }

    @Test fun distanciaEntreDosPuntosConocidos() {
        // 0.01° de latitud ≈ 1.11 km
        val d = distanceMeters(-12.0, -77.0, -12.01, -77.0)
        assertEquals(1112.0, d, 5.0)
        assertEquals(0.0, distanceMeters(-12.0, -77.0, -12.0, -77.0), 0.001)
    }

    @Test fun ordenaDelMasCercanoAlMasLejano() {
        val lista = nearestPoints(central.latitude + 0.0005, central.longitude)
        assertEquals("central", lista.first().point.id)
        assertTrue(lista.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
        assertEquals(RechargePointsRepository.points.size, lista.size)
    }

    @Test fun formateaDistancia() {
        assertEquals("250 m", formatDistance(250.4))
        assertEquals("1.5 km", formatDistance(1500.0))
    }

    @Test fun alertaSoloAlEntrarAlRadioYSeRearmaAlAlejarse() {
        val tracker = ProximityTracker(radiusMeters = 300.0)
        fun en(metros: Double) = listOf(NearbyPoint(central, metros))

        assertTrue(tracker.update(en(1000.0)).isEmpty())        // lejos: no avisa
        assertEquals(1, tracker.update(en(200.0)).size)         // entra al radio: avisa
        assertTrue(tracker.update(en(150.0)).isEmpty())         // sigue cerca: no repite
        assertTrue(tracker.update(en(450.0)).isEmpty())         // borde: aún no se rearma
        assertTrue(tracker.update(en(250.0)).isEmpty())         // vuelve a entrar sin haberse alejado
        assertTrue(tracker.update(en(700.0)).isEmpty())         // se aleja > 2x radio: se rearma
        assertEquals(1, tracker.update(en(100.0)).size)         // vuelve: avisa otra vez
    }

    @Test fun todosLosPuntosEstanEnLima() {
        RechargePointsRepository.points.forEach {
            assertTrue(it.name, it.latitude in -12.30..-11.80 && it.longitude in -77.20..-76.90)
        }
        assertEquals(
            RechargePointsRepository.points.size,
            RechargePointsRepository.points.map { it.id }.toSet().size
        )
    }
}
