package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.claims.*
import com.karenarias.proyectopmac.ui.components.PrimaryButton
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.launch

@Composable
private fun RechargeFrame(
    title: String, onBack: () -> Unit, onHome: () -> Unit,
    onProfile: () -> Unit, onContact: () -> Unit, onClaims: () -> Unit, onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = BackgroundCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsBus, null, tint = PurplePrimary)
                    Text("MetroGo", color = PurpleDark, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp))
                    UserMenu(onProfile, onContact, onClaims, onLogout)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark) }
                    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = PurpleDark)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = CardWhite, windowInsets = WindowInsets(0, 0, 0, 0)) {
                val labels = listOf("Inicio", "Mis tarjetas", "Historial", "Recargar")
                val icons = listOf(Icons.Default.Home, Icons.Default.CreditCard, Icons.Default.History, Icons.Default.FlashOn)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = index == 2,
                        onClick = {
                            when (index) {
                                0 -> onHome()
                                2 -> Unit
                                else -> scope.launch { snackbar.showSnackbar("$label estará disponible próximamente") }
                            }
                        },
                        icon = { Icon(icons[index], null) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PurpleDark, selectedTextColor = PurpleDark,
                            indicatorColor = MauveAccent.copy(alpha = 0.3f),
                            unselectedIconColor = PurplePrimary, unselectedTextColor = PurplePrimary
                        )
                    )
                }
            }
        }, content = content
    )
}

@Composable
fun RechargeHistoryScreen(
    records: List<RechargeRecord>, onRecharge: (String) -> Unit,
    onBack: () -> Unit, onHome: () -> Unit,
    onProfile: () -> Unit = {}, onContact: () -> Unit = {},
    onClaims: () -> Unit = {}, onLogout: () -> Unit = {}
) {
    var visibleCount by rememberSaveable { mutableStateOf(8) }
    RechargeFrame("Historial de recargas", onBack, onHome, onProfile, onContact, onClaims, onLogout) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp)) {
            item {
                Text("Datos de ejemplo", color = PurplePrimary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                Surface(color = PurplePrimary, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
                        Text("Monto", Modifier.weight(0.85f), color = CardWhite, fontSize = 12.sp)
                        Text("Fecha", Modifier.weight(1.1f), color = CardWhite, fontSize = 12.sp)
                        Text("Observación", Modifier.weight(1.5f), color = CardWhite, fontSize = 12.sp)
                    }
                }
            }
            items(records.take(visibleCount), key = { it.id }) { recharge ->
                Surface(color = CardWhite) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable { onRecharge(recharge.id) }
                            .heightIn(min = 64.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val tint = if (recharge.status == "Error en recarga") ErrorWine else PurpleDark
                            Text(recharge.amount, Modifier.weight(0.85f), color = tint, fontSize = 14.sp)
                            Text(recharge.date, Modifier.weight(1.1f), color = PurplePrimary, fontSize = 12.sp)
                            Text(recharge.status, Modifier.weight(1.5f), color = tint, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = BackgroundCream)
                    }
                }
            }
            if (records.isEmpty()) item { Text("Aún no hay recargas.", color = PurpleDark, modifier = Modifier.padding(20.dp)) }
            if (visibleCount < records.size) item {
                TextButton(onClick = { visibleCount += 8 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver más", color = PurplePrimary)
                }
            }
            item { Text("Selecciona una recarga para ver su detalle.", color = PurplePrimary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)) }
        }
    }
}

@Composable
private fun RechargeInfo(record: RechargeRecord) {
    Card(colors = CardDefaults.cardColors(containerColor = CardWhite), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(record.amount, color = PurpleDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(record.status, color = if (record.status == "Error en recarga") ErrorWine else PurplePrimary)
            HorizontalDivider(color = BackgroundCream)
            Text("Operación: ${record.id}", color = PurpleDark)
            Text("Fecha: ${record.date} · ${record.time}", color = PurpleDark)
            Text("Tarjeta: ${record.card.label}", color = PurpleDark)
        }
    }
}

@Composable
private fun RechargeDetailFrame(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = BackgroundCream, contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark) }
            Text(title, color = PurpleDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(20.dp)) {
            item { Column(verticalArrangement = Arrangement.spacedBy(20.dp), content = content) }
        }
    }
}

@Composable
fun RechargeDetailScreen(record: RechargeRecord?, existingClaim: String?, onBack: () -> Unit, onClaim: () -> Unit) {
    RechargeDetailFrame("Detalle de recarga", onBack) {
        if (record == null) {
            Text("No se encontró la recarga.", color = ErrorWine)
        } else {
            RechargeInfo(record)
            Text("¿Tienes algún problema con esta recarga? Puedes registrar un reclamo para solicitar una revisión.", color = PurpleDark)
            PrimaryButton(if (existingClaim == null) "Realizar reclamo" else "Ver reclamo registrado", onClaim)
            Text("Esta recarga es un ejemplo de demostración.", color = PurplePrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun RegisterClaimScreen(
    record: RechargeRecord?, onBack: () -> Unit,
    onSubmit: (String, String) -> String?, onViewClaims: () -> Unit
) {
    var reason by rememberSaveable(record?.id) { mutableStateOf("") }
    var description by rememberSaveable(record?.id) { mutableStateOf("") }
    var error by rememberSaveable(record?.id) { mutableStateOf<String?>(null) }
    var createdId by rememberSaveable(record?.id) { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    if (createdId != null) {
        AlertDialog(
            onDismissRequest = onViewClaims,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = PurplePrimary) },
            title = { Text("Reclamo guardado", color = PurpleDark) },
            text = { Text("Tu reclamo de demostración se guardó con el código $createdId. Puedes consultarlo en Mis reclamos.", color = PurpleDark) },
            confirmButton = { TextButton(onClick = onViewClaims) { Text("Ver mis reclamos") } },
            containerColor = CardWhite
        )
    }
    RechargeDetailFrame("Registrar reclamo", onBack) {
        if (record == null) Text("No se encontró la recarga.", color = ErrorWine)
        else {
            RechargeInfo(record)
            Text("Cuéntanos qué sucedió", color = PurpleDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = createdId == null) {
                    Text(reason.ifBlank { "Selecciona un motivo" }, Modifier.weight(1f), color = PurpleDark)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = PurplePrimary)
                }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    claimReasons.forEach { option -> DropdownMenuItem(
                        text = { Text(option) }, onClick = { reason = option; expanded = false; error = null }
                    ) }
                }
            }
            OutlinedTextField(
                value = description, onValueChange = { if (it.length <= 1000) { description = it; error = null } },
                label = { Text("Descripción del problema") },
                placeholder = { Text("Indica qué ocurrió con esta recarga.") },
                supportingText = { Text("${description.length}/1000 · Mínimo 20 caracteres") },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8, enabled = createdId == null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PurpleDark, unfocusedTextColor = PurpleDark,
                    focusedContainerColor = CardWhite, unfocusedContainerColor = CardWhite,
                    focusedBorderColor = PurplePrimary, unfocusedBorderColor = MauveAccent
                )
            )
            if (error != null) Text(error!!, color = ErrorWine)
            Text("Modo de prueba: el reclamo se guarda solo durante esta sesión y no se envía a atención al cliente.", color = PurplePrimary, fontSize = 12.sp)
            Button(onClick = {
                error = validateClaim(reason, description)
                if (error == null) {
                    createdId = onSubmit(reason, description.trim())
                    if (createdId == null) error = "No se pudo guardar el reclamo. Vuelve a intentarlo."
                }
            }, enabled = createdId == null, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary, contentColor = CardWhite)) {
                Text("Registrar reclamo")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RechargeHistoryPreview() {
    ProyectoPMACTheme(darkTheme = false) { RechargeHistoryScreen(RechargeHistorySamples.records, {}, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RechargeDetailPreview() {
    ProyectoPMACTheme(darkTheme = false) { RechargeDetailScreen(RechargeHistorySamples.records.first(), null, {}, {}) }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RegisterClaimPreview() {
    ProyectoPMACTheme(darkTheme = false) { RegisterClaimScreen(RechargeHistorySamples.records.first(), {}, { _, _ -> "REC-DEMO" }, {}) }
}
