package com.karenarias.proyectopmac.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karenarias.proyectopmac.MainActivity
import com.karenarias.proyectopmac.R
import com.karenarias.proyectopmac.data.card.CardStatus
import com.karenarias.proyectopmac.data.card.formatSoles
import com.karenarias.proyectopmac.data.recharge.RechargePoint
import com.karenarias.proyectopmac.data.recharge.formatDistance

/**
 * Notificaciones de MetroGo:
 * - HU-004: aviso de saldo bajo.
 * - HU-006: aviso al acercarse a un paradero / punto de recarga.
 *
 * Son notificaciones locales (las genera la propia app). Para un "push" enviado desde
 * el servidor se usaría Firebase Cloud Messaging, que necesita cambios en el backend.
 */
object MetroGoNotifications {

    const val CHANNEL_LOW_BALANCE = "saldo_bajo"
    const val CHANNEL_NEARBY_POINTS = "puntos_cercanos"

    private const val ID_LOW_BALANCE = 4001
    private const val ID_NEARBY_BASE = 6000

    /** Crear los canales es seguro aunque ya existan; se llama desde MainActivity. */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LOW_BALANCE,
                "Saldo bajo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos cuando el saldo de tu tarjeta está por debajo del mínimo" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEARBY_POINTS,
                "Puntos de recarga cercanos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos cuando estás cerca de un paradero donde puedes recargar" }
        )
    }

    /** true si la app puede mostrar notificaciones (permiso en Android 13+ y no bloqueadas). */
    fun canPostNotifications(context: Context): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return permissionOk && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showLowBalance(context: Context, status: CardStatus) {
        val text = "Tu tarjeta ${status.maskedNumber} tiene ${formatSoles(status.balanceCents)}. " +
            "Recarga antes de tu próximo viaje para no quedarte varado."
        notify(
            context = context,
            id = ID_LOW_BALANCE,
            channel = CHANNEL_LOW_BALANCE,
            title = "Saldo bajo en tu tarjeta",
            text = text,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    fun showNearbyPoint(context: Context, point: RechargePoint, distanceMeters: Double) {
        notify(
            context = context,
            id = ID_NEARBY_BASE + (point.id.hashCode() and 0x0FFF),
            channel = CHANNEL_NEARBY_POINTS,
            title = "Punto de recarga cerca: ${point.name}",
            text = "Estás a ${formatDistance(distanceMeters)}. ${point.description}",
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    @SuppressLint("MissingPermission") // el permiso se valida en canPostNotifications()
    private fun notify(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        priority: Int
    ) {
        if (!canPostNotifications(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            id,
            // Igual que tocar el ícono de la app: trae la app al frente sin reiniciarla
            // (así no se pierde la sesión, que se guarda solo en memoria).
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_metrogo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // El usuario retiró el permiso justo ahora; no hay nada más que hacer.
        }
    }
}
