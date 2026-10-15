package com.karenarias.proyectopmac.ui.cards

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.data.cards.CardRepository
import com.karenarias.proyectopmac.data.cards.SavedCard
import com.karenarias.proyectopmac.data.notifications.CardNotifier
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import android.content.Context
/**
 * HU-008: Mis tarjetas — eliminar una tarjeta guardada con aviso push de confirmación.
 *
 * Importante (indicación del profesor): la tarjeta NO se borra de la base de datos,
 * solo pasa a estado INACTIVA. Por eso se puede ver en "Mostrar inactivas" y reactivar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCardsScreen(
    onBack: () -> Unit,
    onAddCard: () -> Unit,
    userId: String
) {
    val context = LocalContext.current

    fun hayInternet(): Boolean {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork ?: return false
        val capabilities =
            manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    val repository = remember(userId) {
        CardRepository.get(context, userId)
    }
    val allCards by repository.cards.collectAsState()
    var showInactive by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<SavedCard?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val visibleCards = allCards.filter { showInactive || it.isActive }
    val inactiveCount = allCards.count { !it.isActive }

    fun deactivate(card: SavedCard) {
        val result = repository.deactivate(card.id) ?: return
        CardNotifier.notifyCardDeactivated(context, result)
        scope.launch {
            val action = snackbarHostState.showSnackbar(
                message = "Tarjeta ${result.maskedNumber} eliminada (estado: inactiva)",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Short
            )
            if (action == SnackbarResult.ActionPerformed) repository.reactivate(result.id)
        }
    }

    // Android 13+: hay que pedir permiso para mostrar notificaciones push.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> cardToDelete?.let { deactivate(it) }; cardToDelete = null }

    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, null, tint = ErrorWine) },
            title = { Text("¿Eliminar tarjeta?", color = PurpleDark) },
            text = {
                Text(
                    "La tarjeta ${card.alias} (${card.maskedNumber}) dejará de aparecer en tu lista.\n\n" +
                        "No se borra del sistema: queda en estado INACTIVA y puedes reactivarla cuando quieras."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !CardNotifier.canNotify(context)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        deactivate(card)
                        cardToDelete = null
                    }
                }) { Text("Eliminar", color = ErrorWine, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) { Text("Cancelar", color = PurpleDark) }
            },
            containerColor = CardWhite
        )
    }

    Scaffold(
        containerColor = BackgroundCream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis tarjetas", color = PurpleDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCard,
                containerColor = PurplePrimary,
                contentColor = CardWhite,
                icon = { Icon(Icons.Default.PhotoCamera, null) },
                text = { Text("Agregar tarjeta") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${allCards.count { it.isActive }} activa(s)",
                        color = PurpleDark.copy(alpha = 0.7f), fontSize = 14.sp
                    )
                    FilterChip(
                        selected = showInactive,
                        onClick = { showInactive = !showInactive },
                        label = { Text("Mostrar inactivas ($inactiveCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = CardWhite
                        )
                    )
                }
            }

            if (visibleCards.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CreditCardOff, null, tint = MauveAccent, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No tienes tarjetas activas", color = PurpleDark, fontWeight = FontWeight.Bold)
                        Text("Agrega una escaneándola con la cámara.", color = PurpleDark.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }


            items(visibleCards, key = { it.id }) { card ->

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SavedCardItem(
                        card = card,
                        onDelete = { cardToDelete = card },
                        onReactivate = {
                            repository.reactivate(card.id)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Tarjeta ${card.maskedNumber} reactivada"
                                )
                            }
                        }
                    )

                    if (card.isActive) {


                        Text(
                            text = if (card.saldo != null) {
                                "Último saldo guardado: S/ ${card.saldo}"
                            } else {
                                "Saldo no disponible"
                            },
                            style = MaterialTheme.typography.bodyMedium ,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SavedCardItem(
    card: SavedCard,
    onDelete: () -> Unit,
    onReactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (card.isActive) 1f else 0.6f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (card.isActive) PurpleDark else CardWhite),
        elevation = CardDefaults.cardElevation(if (card.isActive) 4.dp else 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (card.isActive) MauveAccent else BackgroundCream),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsBus, null, tint = if (card.isActive) CardWhite else PurplePrimary)
            }
            Column(Modifier.weight(1f)) {
                val textColor = if (card.isActive) CardWhite else PurpleDark
                Text(card.alias, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(card.number, color = textColor.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(text = card.type, background = PurplePrimary)
                    StatusBadge(
                        text = card.status.name,
                        background = if (card.isActive) MauveAccent.copy(alpha = 0.5f) else ErrorWine
                    )
                }
                card.deactivatedAt?.let {
                    Text("Dada de baja: ${formatDate(it)}", color = ErrorWine, fontSize = 11.sp)
                }
            }
            if (card.isActive) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, "Eliminar tarjeta", tint = CardWhite)
                }
            } else {
                TextButton(onClick = onReactivate) {
                    Text("Reactivar", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, background: androidx.compose.ui.graphics.Color) {
    Surface(color = background, shape = RoundedCornerShape(6.dp)) {
        Text(
            text, color = CardWhite, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE")).format(Date(millis))
