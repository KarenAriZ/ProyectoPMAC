package com.karenarias.proyectopmac.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karenarias.proyectopmac.R
import com.karenarias.proyectopmac.data.cards.SavedCard

/**
 * HU-008: aviso push (notificación local) que confirma que la tarjeta fue dada de baja.
 */
object CardNotifier {

    private const val CHANNEL_ID = "metrogo_tarjetas"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notifyCardDeactivated(context: Context, card: SavedCard) {
        if (!canNotify(context)) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Tarjeta eliminada")
            .setContentText("${card.alias} (${card.maskedNumber}) se eliminó de tu lista.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${card.alias} (${card.maskedNumber}) se eliminó de tu lista y quedó en estado " +
                        "inactiva. Si fue un error, puedes reactivarla desde Mis tarjetas."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(card.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // El usuario negó el permiso de notificaciones: no se muestra el aviso.
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Mis tarjetas", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Avisos sobre cambios en tus tarjetas" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
