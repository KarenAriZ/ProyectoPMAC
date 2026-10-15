package com.karenarias.proyectopmac.ui.card

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karenarias.proyectopmac.data.card.*
import com.karenarias.proyectopmac.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ActiveGreen = Color(0xFF5E7D5A)

/**
 * HU-004 (ruta con estado): conecta la pantalla con su ViewModel y con el permiso
 * de notificaciones de Android 13+.
 */
@Composable
fun CardStatusRoute(
    onBack: () -> Unit,
    onOpenRechargePoints: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CardStatusViewModel = viewModel()
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onNotificationPermissionChanged() }

    // Si el usuario activa el permiso desde Ajustes y vuelve, se actualiza el estado.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onNotificationPermissionChanged()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CardStatusScreen(
        status = viewModel.status,
        isRefreshing = viewModel.isRefreshing,
        syncMessage = viewModel.syncMessage,
        notificationsAllowed = viewModel.notificationsAllowed,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onThresholdSelected = viewModel::onThresholdSelected,
        onAlertsEnabledChanged = { enabled ->
            viewModel.onAlertsEnabledChanged(enabled)
            if (enabled && !viewModel.notificationsAllowed &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(notificationSettingsIntent(context.packageName))
            }
        },
        onOpenNotificationSettings = {
            context.startActivity(notificationSettingsIntent(context.packageName))
        },
        onOpenRechargePoints = onOpenRechargePoints,
        onSimulateTrip = viewModel::onSimulateTrip,
        onResetDemoBalance = viewModel::onResetDemoBalance,
        modifier = modifier
    )
}

private fun notificationSettingsIntent(packageName: String) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * HU-004: Consultar el saldo y estado actual de la tarjeta y configurar el aviso de saldo bajo.
 * Pantalla stateless (recibe todo por parámetros), igual que el resto de pantallas del proyecto.
 */
@Composable
fun CardStatusScreen(
    status: CardStatus,
    isRefreshing: Boolean,
    notificationsAllowed: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onThresholdSelected: (Long) -> Unit,
    onAlertsEnabledChanged: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenRechargePoints: () -> Unit,
    onSimulateTrip: () -> Unit,
    onResetDemoBalance: () -> Unit,
    modifier: Modifier = Modifier,
    syncMessage: String? = null
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCream,
        topBar = { SimpleTopBar(title = "Mi tarjeta", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BalanceCard(status = status, isRefreshing = isRefreshing, onRefresh = onRefresh)

            // De dónde vienen los datos: AWS o la copia guardada en el celular.
            if (syncMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val fromAws = syncMessage.startsWith("Sincronizado")
                    Icon(
                        if (fromAws) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (fromAws) ActiveGreen else ErrorWine,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(syncMessage, color = PurpleDark.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            if (status.isLowBalance) {
                LowBalanceWarning(status = status, onOpenRechargePoints = onOpenRechargePoints)
            }

            TripsPlanningCard(status = status)

            AlertSettingsCard(
                status = status,
                notificationsAllowed = notificationsAllowed,
                onThresholdSelected = onThresholdSelected,
                onAlertsEnabledChanged = onAlertsEnabledChanged,
                onRequestNotifications = onRequestNotifications,
                onOpenNotificationSettings = onOpenNotificationSettings
            )

            OutlinedButton(
                onClick = onOpenRechargePoints,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = PurplePrimary)
                Spacer(Modifier.width(8.dp))
                Text("Ver puntos de recarga cercanos", color = PurplePrimary, fontWeight = FontWeight.Bold)
            }

            DemoCard(onSimulateTrip = onSimulateTrip, onResetDemoBalance = onResetDemoBalance)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun SimpleTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundCream)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PurpleDark)
        }
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PurpleDark)
    }
}

@Composable
private fun BalanceCard(status: CardStatus, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MauveAccent)
                    Text(status.number, color = CardWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Surface(color = PurplePrimary, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        status.type,
                        color = CardWhite,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saldo disponible", color = MauveAccent, fontSize = 13.sp)
                Text(
                    formatSoles(status.balanceCents),
                    color = if (status.isLowBalance) DustyRose else CardWhite,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardStateChip(status.state)
                TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = CardWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CardWhite, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRefreshing) "Consultando..." else "Actualizar", color = CardWhite)
                }
            }

            Text(
                text = if (status.lastUpdatedMillis > 0) "Última consulta: ${formatDateTime(status.lastUpdatedMillis)}"
                else "Aún no se ha consultado el saldo",
                color = MauveAccent,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CardStateChip(state: CardState) {
    val color = if (state == CardState.ACTIVA) ActiveGreen else ErrorWine
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CardWhite)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text("Estado: ${state.label}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LowBalanceWarning(status: CardStatus, onOpenRechargePoints: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ErrorWine.copy(alpha = 0.10f))
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorWine)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Saldo bajo", color = ErrorWine, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Tu saldo está por debajo de ${formatSoles(status.lowBalanceThresholdCents)}. " +
                        "Recarga antes de tu próximo viaje para no quedarte varado en la estación.",
                    color = PurpleDark,
                    fontSize = 13.sp
                )
                TextButton(onClick = onOpenRechargePoints, contentPadding = PaddingValues(0.dp)) {
                    Text("Buscar dónde recargar >", color = ErrorWine, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TripsPlanningCard(status: CardStatus) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBox(Icons.Default.CalendarMonth)
            Column(Modifier.weight(1f)) {
                Text("Planifica tus viajes", color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = when (status.estimatedTrips) {
                        0L -> "Tu saldo no alcanza para un viaje."
                        1L -> "Te alcanza para 1 viaje aproximadamente."
                        else -> "Te alcanza para ${status.estimatedTrips} viajes aproximadamente."
                    },
                    color = PurpleDark.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
                Text(
                    "Tarifa referencial ${status.type.lowercase(Locale.getDefault())}: ${formatSoles(status.fareCents)}",
                    color = PurplePrimary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlertSettingsCard(
    status: CardStatus,
    notificationsAllowed: Boolean,
    onThresholdSelected: (Long) -> Unit,
    onAlertsEnabledChanged: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBox(Icons.Default.NotificationsActive)
            Column(Modifier.weight(1f)) {
                Text("Avisarme si mi saldo está bajo", color = PurpleDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Recibirás una notificación en tu celular", color = PurpleDark.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Switch(
                checked = status.lowBalanceAlertsEnabled,
                onCheckedChange = onAlertsEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CardWhite,
                    checkedTrackColor = PurplePrimary,
                    uncheckedBorderColor = MauveAccent,
                    uncheckedThumbColor = MauveAccent
                )
            )
        }

        if (status.lowBalanceAlertsEnabled) {
            Spacer(Modifier.height(12.dp))
            Text("Avisarme cuando baje de:", color = PurpleDark, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LOW_BALANCE_THRESHOLD_OPTIONS_CENTS.forEach { cents ->
                    FilterChip(
                        selected = status.lowBalanceThresholdCents == cents,
                        onClick = { onThresholdSelected(cents) },
                        label = { Text(formatSoles(cents)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = CardWhite,
                            labelColor = PurpleDark
                        )
                    )
                }
            }

            if (!notificationsAllowed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Las notificaciones están desactivadas para MetroGo, así que solo verás el aviso dentro de la app.",
                    color = ErrorWine,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRequestNotifications) {
                        Text("Activar notificaciones", color = PurplePrimary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onOpenNotificationSettings) {
                        Text("Abrir ajustes", color = PurplePrimary)
                    }
                }
            }
        }
    }
}

/** Botones para demostrar la alerta en la sustentación sin esperar a gastar saldo de verdad. */
@Composable
private fun DemoCard(onSimulateTrip: () -> Unit, onResetDemoBalance: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Modo demostración", color = PurpleDark.copy(alpha = 0.6f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSimulateTrip) {
                Text("Simular un viaje", color = PurplePrimary)
            }
            TextButton(onClick = onResetDemoBalance) {
                Text("Restablecer saldo", color = PurplePrimary)
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun IconBox(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundCream),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CardStatusScreenPreview() {
    ProyectoPMACTheme {
        CardStatusScreen(
            status = CardStatus(
                number = "8754785478",
                type = "Estudiante",
                balanceCents = 430,
                state = CardState.ACTIVA,
                lastUpdatedMillis = System.currentTimeMillis(),
                lowBalanceThresholdCents = 500,
                lowBalanceAlertsEnabled = true
            ),
            isRefreshing = false,
            notificationsAllowed = false,
            onBack = {},
            onRefresh = {},
            onThresholdSelected = {},
            onAlertsEnabledChanged = {},
            onRequestNotifications = {},
            onOpenNotificationSettings = {},
            onOpenRechargePoints = {},
            onSimulateTrip = {},
            onResetDemoBalance = {}
        )
    }
}
