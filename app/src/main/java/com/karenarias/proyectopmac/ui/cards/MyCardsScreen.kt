package com.karenarias.proyectopmac.ui.cards

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.data.cards.SavedCard
import com.karenarias.proyectopmac.data.cards.CardStatus
import com.karenarias.proyectopmac.data.remote.UserCard
import com.karenarias.proyectopmac.data.notifications.CardNotifier
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.launch

/** Las tarjetas y los cambios vienen de AWS, no de la caché compartida anterior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCardsScreen(
    cards: List<UserCard>, isLoading: Boolean, isSaving: Boolean, errorMessage: String?,
    onBack: () -> Unit, onAddCard: () -> Unit, onRefresh: () -> Unit,
    onSelectCard: (String) -> Unit,
    onChangeState: (String, Boolean, (UserCard) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showInactive by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<UserCard?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun deactivate(card: UserCard) {
        cardToDelete = null
        onChangeState(card.numeroTarjeta, false) { confirmed ->
            CardNotifier.notifyCardDeactivated(context, SavedCard(
                id = confirmed.numeroTarjeta, number = confirmed.numeroTarjeta,
                alias = confirmed.alias, type = confirmed.tipo, status = CardStatus.INACTIVA
            ))
            scope.launch {
                if (snackbar.showSnackbar("Tarjeta desactivada en tu cuenta", "Deshacer") == SnackbarResult.ActionPerformed)
                    onChangeState(confirmed.numeroTarjeta, true) {}
            }
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cardToDelete?.let(::deactivate)
    }
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("¿Eliminar tarjeta?", color = PurpleDark) },
            text = { Text("${card.alias} (${card.maskedNumber}) quedará INACTIVA en tu cuenta. Puedes reactivarla después.", color = PurpleDark) },
            confirmButton = { TextButton(enabled = !isSaving, onClick = {
                if (Build.VERSION.SDK_INT >= 33 && !CardNotifier.canNotify(context))
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else deactivate(card)
            }) { Text("Eliminar", color = ErrorWine) } },
            dismissButton = { TextButton(onClick = { cardToDelete = null }) { Text("Cancelar", color = PurplePrimary) } },
            containerColor = CardWhite
        )
    }
    Scaffold(
        containerColor = BackgroundCream, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(
            title = { Text("Mis tarjetas", color = PurpleDark, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark) } },
            actions = { IconButton(onClick = onRefresh, enabled = !isLoading && !isSaving) { Icon(Icons.Default.Refresh, "Actualizar tarjetas", tint = PurplePrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundCream)
        ) },
        floatingActionButton = { ExtendedFloatingActionButton(
            onClick = { if (!isSaving) onAddCard() }, containerColor = PurplePrimary, contentColor = CardWhite,
            icon = { Icon(Icons.Default.PhotoCamera, null) }, text = { Text("Agregar tarjeta") }
        ) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { FilterChip(selected = showInactive, onClick = { showInactive = !showInactive },
                label = { Text("Mostrar inactivas (${cards.count { !it.isActive }})") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PurplePrimary, selectedLabelColor = CardWhite)) }
            if (isLoading || isSaving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = PurplePrimary) }
            if (errorMessage != null) item {
                Column {
                    Text(errorMessage, color = ErrorWine)
                    TextButton(onClick = onRefresh, enabled = !isLoading && !isSaving) { Text("Actualizar", color = PurplePrimary) }
                }
            }
            val visible = cards.filter { showInactive || it.isActive }
            if (visible.isEmpty() && !isLoading && errorMessage == null) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CreditCardOff, null, tint = PurplePrimary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(if (showInactive) "Aún no tienes tarjetas" else "No tienes tarjetas activas", color = PurpleDark, fontWeight = FontWeight.Bold)
                    Text("Agrega tu tarjeta del Metropolitano para comenzar.", color = PurplePrimary)
                }
            }
            items(visible, key = { it.numeroTarjeta }) { card ->
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(card.alias, color = PurpleDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${card.maskedNumber} · ${card.tipo}", color = PurpleDark)
                        Text(if (card.isActive) "Activa en tu cuenta" else "Inactiva en tu cuenta", color = PurplePrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (card.isActive) {
                                TextButton(onClick = { onSelectCard(card.numeroTarjeta) }, enabled = !isSaving) { Text("Ver saldo", color = PurplePrimary) }
                                TextButton(onClick = { cardToDelete = card }, enabled = !isSaving) { Text("Eliminar", color = ErrorWine) }
                            } else TextButton(onClick = {
                                onChangeState(card.numeroTarjeta, true) {
                                    scope.launch { snackbar.showSnackbar("Tarjeta reactivada en tu cuenta") }
                                }
                            }, enabled = !isSaving) { Text("Reactivar", color = PurplePrimary) }
                        }
                    }
                }
            }
        }
    }
}
