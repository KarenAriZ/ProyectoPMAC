package com.karenarias.proyectopmac

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.karenarias.proyectopmac.notifications.MetroGoNotifications
import com.karenarias.proyectopmac.ui.navigation.MetroGoNavGraph
import com.karenarias.proyectopmac.ui.theme.ProyectoPMACTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MetroGoNotifications.createChannels(this) // HU-004 y HU-006
        setContent {
            ProyectoPMACTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MetroGoNavGraph(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}