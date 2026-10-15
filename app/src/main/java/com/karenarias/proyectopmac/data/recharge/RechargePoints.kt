package com.karenarias.proyectopmac.data.recharge

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HU-006: puntos físicos donde se puede recargar la tarjeta en efectivo.
 * Kotlin puro (sin clases de Android) para poder probarlo con tests unitarios.
 */
data class RechargePoint(
    val id: String,
    val name: String,
    val district: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

data class NearbyPoint(val point: RechargePoint, val distanceMeters: Double)

/** Distancia a partir de la cual se envía la alerta de paradero próximo. */
const val NEARBY_ALERT_RADIUS_METERS = 300.0

/** Centro por defecto del mapa (Estación Central) cuando no hay ubicación del usuario. */
const val DEFAULT_MAP_LATITUDE = -12.05773
const val DEFAULT_MAP_LONGITUDE = -77.03598

object RechargePointsRepository {

    private const val RECARGA = "Recarga en efectivo en boletería y máquinas de la estación."

    /**
     * Estaciones del Metropolitano en el tramo de la Vía Expresa.
     * Estación Central está verificada en OpenStreetMap; las demás son ubicaciones
     * aproximadas (pueden variar 100-300 m). Para corregir una, búscala en Google Maps,
     * mantén presionado sobre la estación y copia las coordenadas aquí.
     * Si el backend llega a tener un endpoint de puntos de recarga, reemplaza esta lista por esa consulta.
     */
    val points: List<RechargePoint> = listOf(
        RechargePoint("central", "Estación Central", "Cercado de Lima", RECARGA, -12.05773, -77.03598),
        RechargePoint("estadio_nacional", "Estación Estadio Nacional", "La Victoria", RECARGA, -12.06750, -77.03350),
        RechargePoint("mexico", "Estación México", "La Victoria", RECARGA, -12.07600, -77.03050),
        RechargePoint("canada", "Estación Canadá", "La Victoria", RECARGA, -12.08300, -77.02900),
        RechargePoint("javier_prado", "Estación Javier Prado", "San Isidro", RECARGA, -12.09050, -77.02750),
        RechargePoint("canaval_moreyra", "Estación Canaval y Moreyra", "San Isidro", RECARGA, -12.09600, -77.02700),
        RechargePoint("aramburu", "Estación Aramburú", "Surquillo", RECARGA, -12.10150, -77.02700),
        RechargePoint("domingo_orue", "Estación Domingo Orué", "Surquillo", RECARGA, -12.10700, -77.02700),
        RechargePoint("angamos", "Estación Angamos", "Surquillo", RECARGA, -12.11350, -77.02650),
        RechargePoint("ricardo_palma", "Estación Ricardo Palma", "Miraflores", RECARGA, -12.11900, -77.02600),
        RechargePoint("benavides", "Estación Benavides", "Miraflores", RECARGA, -12.12650, -77.02500),
        RechargePoint("28_julio", "Estación 28 de Julio", "Miraflores", RECARGA, -12.13200, -77.02400),
        RechargePoint("plaza_flores", "Estación Plaza de Flores", "Barranco", RECARGA, -12.14200, -77.02150)
    )
}

/** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/** Puntos ordenados del más cercano al más lejano. */
fun nearestPoints(
    latitude: Double,
    longitude: Double,
    points: List<RechargePoint> = RechargePointsRepository.points
): List<NearbyPoint> = points
    .map { NearbyPoint(it, distanceMeters(latitude, longitude, it.latitude, it.longitude)) }
    .sortedBy { it.distanceMeters }

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m"
    else String.format(Locale.US, "%.1f km", meters / 1000)

/**
 * Decide a qué paraderos avisar. Cada punto avisa una sola vez al entrar al radio, y
 * se vuelve a activar cuando el usuario se aleja el doble del radio (evita avisos
 * repetidos si el GPS oscila justo en el borde).
 */
class ProximityTracker(private val radiusMeters: Double = NEARBY_ALERT_RADIUS_METERS) {
    private val alerted = mutableSetOf<String>()

    /** @return puntos a los que el usuario acaba de acercarse (hay que notificarlos). */
    fun update(nearby: List<NearbyPoint>): List<NearbyPoint> {
        nearby.filter { it.distanceMeters > radiusMeters * 2 }
            .forEach { alerted.remove(it.point.id) }
        return nearby.filter { it.distanceMeters <= radiusMeters && alerted.add(it.point.id) }
    }

    fun reset() = alerted.clear()
}
