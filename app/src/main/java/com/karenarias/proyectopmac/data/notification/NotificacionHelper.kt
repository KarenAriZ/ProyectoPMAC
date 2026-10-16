package com.karenarias.proyectopmac.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificacionHelper(private val context: Context) {

    private val canalId = "metrogo_conexion_alertas"
    fun mostrarConexionRecuperada() {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val canal = NotificationChannel(
            canalId,
            "Conexión MetroGo",
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(canal)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificacion = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle("MetroGo")
            .setContentText("Se recuperó la conexión a internet. Ya puedes consultar tus tarjetas.")
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notificacion)
    }
}