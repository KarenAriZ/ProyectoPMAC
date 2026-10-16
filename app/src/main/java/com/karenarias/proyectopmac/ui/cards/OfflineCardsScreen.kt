package com.karenarias.proyectopmac.ui.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.karenarias.proyectopmac.OfflineSessionStorage
import com.karenarias.proyectopmac.data.cards.CardRepository

@Composable
fun OfflineCardsScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val userId = remember {
        OfflineSessionStorage(context).obtenerUsuario()
    }

    val repository = remember(userId) {
        userId?.let { CardRepository.get(context, it) }
    }

    val tarjetas by repository?.cards?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Mis tarjetas sin conexión",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Puedes consultar tus tarjetas guardadas. " +
                    "Las recargas y modificaciones requieren internet."
        )

        if (tarjetas.none { it.isActive }) {
            Text("No tienes tarjetas activas guardadas.")
        } else {
            tarjetas.filter { it.isActive }.forEach { tarjeta ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            tarjeta.alias,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Tipo: ${tarjeta.type}")

                        Text("Número: ${tarjeta.maskedNumber}")


                        if (tarjeta.saldo != null &&
                            tarjeta.ultimaSincronizacion != null
                        ) {
                            Text("Último saldo: S/ ${tarjeta.saldo}")

                            if (tarjeta.saldoDemostracion) {
                                Text("Saldo de demostración")
                            }

                            val fecha = remember(tarjeta.ultimaSincronizacion) {
                                java.text.SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(
                                    java.util.Date(tarjeta.ultimaSincronizacion)
                                )
                            }

                            Text("Última sincronización: $fecha")
                        } else {
                            Text("Saldo: no disponible")
                            Text("Esta tarjeta aún no tiene un saldo sincronizado.")
                        }

                    }
                }
            }
        }

        Button(onClick = onBack) {
            Text("Volver")
        }
    }
}