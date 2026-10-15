package com.karenarias.proyectopmac.ui.support

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.theme.*

/**
 * Datos de la central de atención al cliente.
 * IMPORTANTE: número de EJEMPLO para la demo. Reemplazar por el número oficial
 * de atención del Metropolitano/ATU antes de la entrega.
 */
object SupportContact {
    const val PHONE_NUMBER = "016155000"       // número que se marca
    const val PHONE_DISPLAY = "(01) 615-5000"  // número que se muestra
    const val SCHEDULE = "Lunes a domingo, 5:00 a.m. – 11:00 p.m."
}

private data class SupportReason(val icon: ImageVector, val title: String, val description: String)

private val reasons = listOf(
    SupportReason(Icons.Default.CreditCardOff, "Pérdida o robo de tarjeta", "Bloquea tu tarjeta para proteger tu saldo."),
    SupportReason(Icons.Default.AccountBalanceWallet, "Reclamo de saldo", "Recargaste y el saldo no se actualizó."),
    SupportReason(Icons.Default.Warning, "Emergencias", "Incidentes en estaciones o buses.")
)

/**
 * HU-009: Realizar una llamada telefónica directa a la central de atención al cliente.
 *
 * Usa Intent.ACTION_DIAL: abre el marcador del teléfono con el número ya escrito.
 * Ventaja: no necesita el permiso CALL_PHONE y el usuario confirma la llamada con un toque.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Default.Call, null, tint = PurplePrimary) },
            title = { Text("Llamar a la central", color = PurpleDark) },
            text = { Text("Se abrirá tu teléfono para llamar al ${SupportContact.PHONE_DISPLAY}.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    error = dialSupport(context)
                }) { Text("Llamar", color = PurplePrimary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar", color = PurpleDark) }
            },
            containerColor = CardWhite
        )
    }

    Scaffold(
        containerColor = BackgroundCream,
        topBar = {
            TopAppBar(
                title = { Text("Atención al cliente", color = PurpleDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tarjeta principal con el número y botón de llamada
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleDark),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(MauveAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, null, tint = CardWhite, modifier = Modifier.size(36.dp))
                    }
                    Text("Central de atención", color = MauveAccent, fontSize = 14.sp)
                    Text(
                        SupportContact.PHONE_DISPLAY,
                        color = CardWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                    Text(SupportContact.SCHEDULE, color = MauveAccent, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MauveAccent, contentColor = PurpleDark)
                    ) {
                        Icon(Icons.Default.Call, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Llamar ahora", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            error?.let { Text(it, color = ErrorWine, fontSize = 13.sp) }

            Text("¿En qué te podemos ayudar?", color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            reasons.forEach { reason ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showConfirm = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(BackgroundCream),
                            contentAlignment = Alignment.Center
                        ) { Icon(reason.icon, null, tint = PurplePrimary) }
                        Column(Modifier.weight(1f)) {
                            Text(reason.title, color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(reason.description, color = PurpleDark.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Call, "Llamar", tint = PurplePrimary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Abre el marcador con el número. Devuelve un mensaje de error si el equipo no puede llamar. */
fun dialSupport(context: Context): String? {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${SupportContact.PHONE_NUMBER}"))
    return try {
        context.startActivity(intent)
        null
    } catch (e: ActivityNotFoundException) {
        "Este dispositivo no puede realizar llamadas. Marca al ${SupportContact.PHONE_DISPLAY}."
    }
}

@Preview(showBackground = true)
@Composable
fun SupportScreenPreview() {
    ProyectoPMACTheme { SupportScreen(onBack = {}) }
}
