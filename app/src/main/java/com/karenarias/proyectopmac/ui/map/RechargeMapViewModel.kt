package com.karenarias.proyectopmac.ui.map

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.karenarias.proyectopmac.data.recharge.NEARBY_ALERT_RADIUS_METERS
import com.karenarias.proyectopmac.data.recharge.NearbyPoint
import com.karenarias.proyectopmac.data.recharge.ProximityTracker
import com.karenarias.proyectopmac.data.recharge.RechargePoint
import com.karenarias.proyectopmac.data.recharge.RechargePointsRepository
import com.karenarias.proyectopmac.data.recharge.nearestPoints
import com.karenarias.proyectopmac.notifications.MetroGoNotifications

/**
 * HU-006: estado del mapa de puntos de recarga y de las alertas de paraderos próximos.
 */
class RechargeMapViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val tracker = ProximityTracker()

    val points: List<RechargePoint> = RechargePointsRepository.points

    /** null mientras no se conozca la ubicación del usuario. */
    var userLatitude by mutableStateOf<Double?>(null)
        private set
    var userLongitude by mutableStateOf<Double?>(null)
        private set

    /** Puntos ordenados por distancia; null si aún no hay ubicación. */
    var nearby by mutableStateOf<List<NearbyPoint>?>(null)
        private set

    /** Punto dentro del radio de alerta (se muestra como aviso dentro de la app). */
    var closeByPoint by mutableStateOf<NearbyPoint?>(null)
        private set

    var alertsEnabled by mutableStateOf(prefs.getBoolean(KEY_ALERTS_ENABLED, true))
        private set

    var notificationsAllowed by mutableStateOf(MetroGoNotifications.canPostNotifications(application))
        private set

    val mapsApiKeyMissing: Boolean = !hasMapsApiKey(application)

    fun onLocationUpdate(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        val sorted = nearestPoints(latitude, longitude, points)
        nearby = sorted
        closeByPoint = sorted.firstOrNull()?.takeIf { it.distanceMeters <= NEARBY_ALERT_RADIUS_METERS }

        val justArrived = tracker.update(sorted)
        if (alertsEnabled && notificationsAllowed) {
            justArrived.forEach { MetroGoNotifications.showNearbyPoint(getApplication(), it.point, it.distanceMeters) }
        }
    }

    fun onAlertsEnabledChanged(enabled: Boolean) {
        alertsEnabled = enabled
        prefs.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        // Al reactivar, se vuelve a avisar del paradero en el que ya estás.
        if (enabled) tracker.reset()
    }

    fun onNotificationPermissionChanged() {
        notificationsAllowed = MetroGoNotifications.canPostNotifications(getApplication())
    }

    private companion object {
        const val PREFS_NAME = "metrogo_map"
        const val KEY_ALERTS_ENABLED = "nearby_alerts_enabled"
    }
}

/** Revisa que la API key de Google Maps se haya configurado en local.properties. */
@Suppress("DEPRECATION")
private fun hasMapsApiKey(context: Context): Boolean = runCatching {
    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
}.getOrDefault(false)
