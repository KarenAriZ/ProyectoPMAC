package com.karenarias.proyectopmac

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.karenarias.proyectopmac.ui.navigation.MetroGoNavGraph
import com.karenarias.proyectopmac.ui.theme.ProyectoPMACTheme
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.karenarias.proyectopmac.data.network.ConexionMonitor
import com.karenarias.proyectopmac.data.notification.NotificacionHelper
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : FragmentActivity() {
    private lateinit var conexionMonitor: ConexionMonitor
    private val solicitarPermisoNotificaciones =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permitido ->
            // Android devuelve aquí si el usuario aceptó o rechazó.
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificacionHelper = NotificacionHelper(this)

        conexionMonitor = ConexionMonitor(this) {
            notificacionHelper.mostrarConexionRecuperada()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            solicitarPermisoNotificaciones.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        enableEdgeToEdge()
        setContent {
            ProyectoPMACTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MetroGoNavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        conexionMonitor.iniciar()
    }

    override fun onStop() {
        conexionMonitor.detener()
        super.onStop()
    }
}