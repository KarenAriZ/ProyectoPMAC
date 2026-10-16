package com.karenarias.proyectopmac.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.data.remote.UserCard
import com.karenarias.proyectopmac.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCardDetailScreen(
    card: UserCard?, isLoading: Boolean, errorMessage: String?,
    onBack: () -> Unit, onCards: () -> Unit, onMap: () -> Unit
) {
    Scaffold(containerColor = BackgroundCream, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        TopAppBar(title = { Text("Mi tarjeta", color = PurpleDark) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = PurplePrimary)
            errorMessage?.let { Text(it, color = ErrorWine) }
            if (card == null) {
                Text("Selecciona o agrega una tarjeta activa para consultar su información.", color = PurpleDark)
                TextButton(onClick = onCards) { Text("Mis tarjetas", color = PurplePrimary) }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(card.alias, color = PurpleDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${card.maskedNumber} · ${card.tipo}", color = PurplePrimary)
                        Text("Saldo disponible", color = PurplePrimary)
                        Text(card.saldoCentimos?.let { String.format(Locale.US, "S/ %.2f", it / 100.0) } ?: "Sin información de saldo",
                            color = PurpleDark, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("Estado de tarjeta: ${card.estadoTarjeta ?: "Sin consultar"}", color = PurpleDark)
                        Text("Estado en tu cuenta: ${card.estado}", color = PurpleDark)
                    }
                }
                TextButton(onClick = onMap) { Text("Ver puntos de recarga", color = PurplePrimary) }
            }
        }
    }
}
