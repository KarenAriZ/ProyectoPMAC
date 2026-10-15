package com.karenarias.proyectopmac.ui.auth

import com.karenarias.proyectopmac.ui.claims.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.theme.*
import kotlinx.coroutines.launch

private val ResolvedGreen = Color(0xFF52644E)

@Composable
private fun ClaimStatusBadge(status: ClaimStatus) {
    val tint = if (status == ClaimStatus.RESOLVED) ResolvedGreen else PurplePrimary
    Surface(color = tint.copy(alpha = 0.12f), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(when (status) {
                ClaimStatus.RECEIVED -> Icons.Default.Inbox
                ClaimStatus.REVIEW -> Icons.Default.Schedule
                ClaimStatus.RESOLVED -> Icons.Default.CheckCircle
            }, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(status.label, color = tint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ClaimsFrame(
    title: String, onBack: () -> Unit, onHome: () -> Unit, onClaims: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable (PaddingValues) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier,
        // MainActivity already applies system bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = BackgroundCream,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.DirectionsBus, null, tint = PurplePrimary)
                    Text("MetroGo", color = PurpleDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.padding(top = 8.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark)
                    }
                    Text(title, color = PurpleDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            Surface(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), shadowElevation = 8.dp) {
                NavigationBar(containerColor = CardWhite, windowInsets = WindowInsets(0, 0, 0, 0)) {
                    val labels = listOf("Inicio", "Mis tarjetas", "Historial", "Recargar")
                    val icons = listOf(Icons.Default.Home, Icons.Default.CreditCard, Icons.Default.History, Icons.Default.FlashOn)
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(selected = index == 1,
                            onClick = {
                                when (index) {
                                    0 -> onHome()
                                    1 -> onClaims()
                                    else -> scope.launch { snackbar.showSnackbar("$label estará disponible próximamente") }
                                }
                            },
                            icon = { Icon(icons[index], null) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurpleDark, selectedTextColor = PurpleDark,
                                indicatorColor = MauveAccent.copy(alpha = 0.25f),
                                unselectedIconColor = PurplePrimary, unselectedTextColor = PurplePrimary))
                    }
                }
            }
        }, content = content
    )
}

@Composable
fun ClaimsScreen(
    claims: List<CardClaim>, cards: List<ClaimCard>, selectedCard: String,
    selectedFilter: ClaimFilter, onCardSelected: (String) -> Unit,
    onFilterSelected: (ClaimFilter) -> Unit, onClaimClick: (String) -> Unit,
    onBack: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val visibleClaims = filterClaims(claims, selectedCard, selectedFilter)
    ClaimsFrame("Mis reclamos", onBack, onHome, {}, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Consulta el estado de tus incidencias", color = PurplePrimary, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Box {
                    OutlinedButton(onClick = { menuExpanded = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(12.dp)) {
                        Text(cards.find { it.id == selectedCard }?.label ?: "Todas mis tarjetas",
                            modifier = Modifier.weight(1f), color = PurpleDark)
                        Icon(Icons.Default.KeyboardArrowDown, "Filtrar por tarjeta", tint = PurplePrimary)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Todas mis tarjetas") }, onClick = {
                            onCardSelected("all"); menuExpanded = false
                        })
                        cards.forEach { card ->
                            DropdownMenuItem(text = { Text(card.label) }, onClick = {
                                onCardSelected(card.id); menuExpanded = false
                            })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClaimFilter.entries.forEach { filter ->
                        FilterChip(selected = selectedFilter == filter, onClick = { onFilterSelected(filter) },
                            label = { Text(filter.label) }, shape = RoundedCornerShape(24.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary, selectedLabelColor = CardWhite,
                                labelColor = PurpleDark))
                    }
                }
            }
            if (visibleClaims.isEmpty()) {
                item {
                    ClaimPanel {
                        Icon(Icons.Default.Inbox, null, tint = MauveAccent, modifier = Modifier.size(36.dp))
                        Text("No hay reclamos para estos filtros", color = PurpleDark, fontWeight = FontWeight.Bold)
                        Text("Prueba con otra tarjeta o selecciona Todos.", color = PurplePrimary)
                    }
                }
            }
            items(visibleClaims, key = { it.id }) { claim ->
                Card(onClick = { onClaimClick(claim.id) }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(claim.title, color = PurpleDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        ClaimStatusBadge(claim.status)
                        Text("Tarjeta ${claim.card.label}", color = PurpleDark, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Reclamo #${claim.id}", color = PurplePrimary, fontSize = 13.sp)
                                Text(claim.date, color = PurplePrimary, fontSize = 13.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Ver detalle", tint = PurplePrimary)
                        }
                    }
                }
            }
            item {
                Text("Selecciona un reclamo para ver el detalle.", color = PurplePrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ClaimPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = CardWhite, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun ClaimDetailScreen(
    claim: CardClaim?, onBack: () -> Unit, onHome: () -> Unit, onClaims: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClaimsFrame("Detalle del reclamo", onBack, onHome, onClaims, modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (claim == null) {
                item {
                    ClaimPanel {
                        Text("No se encontró el reclamo", color = PurpleDark)
                        TextButton(onClick = onClaims) { Text("Volver a mis reclamos") }
                    }
                }
            } else {
                item {
                    Text(claim.title, color = PurpleDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    ClaimStatusBadge(claim.status)
                    Spacer(Modifier.height(8.dp))
                    Text("Reclamo #${claim.id}", color = PurplePrimary)
                }
                item {
                    Surface(color = PurpleDark, shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.DirectionsBus, null, tint = CardWhite, modifier = Modifier.size(36.dp))
                            Column {
                                Text("Tarjeta ${claim.card.type}", color = CardWhite, fontWeight = FontWeight.Medium)
                                Text("•••• ${claim.card.lastDigits}", color = CardWhite, fontSize = 20.sp)
                            }
                        }
                    }
                }
                item {
                    ClaimPanel {
                        Text("Información del reclamo", color = PurpleDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        DetailField("Fecha de registro", claim.receivedAt)
                        DetailField("Monto reclamado", claim.amount)
                        DetailField("Descripción", claim.description)
                    }
                }
                item {
                    ClaimPanel {
                        Text("Seguimiento", color = PurpleDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        ClaimStatus.entries.forEachIndexed { index, status ->
                            val reached = index <= claim.status.ordinal
                            val current = status == claim.status
                            val time = when (status) {
                                ClaimStatus.RECEIVED -> claim.receivedAt
                                ClaimStatus.REVIEW -> claim.reviewedAt
                                ClaimStatus.RESOLVED -> claim.resolvedAt
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape,
                                        color = if (reached) PurplePrimary else MauveAccent.copy(alpha = 0.2f),
                                        modifier = Modifier.size(28.dp)) {
                                        Icon(if (reached && !current || status == ClaimStatus.RESOLVED && reached)
                                            Icons.Default.Check else Icons.Default.Schedule,
                                            null, tint = if (reached) CardWhite else PurplePrimary,
                                            modifier = Modifier.padding(5.dp))
                                    }
                                    if (index < 2) Box(Modifier.width(2.dp).height(38.dp).background(MauveAccent.copy(alpha = 0.5f)))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(status.label, color = PurpleDark,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium)
                                    Text(time ?: "Pendiente", color = PurplePrimary, fontSize = 13.sp)
                                    if (current && status == ClaimStatus.REVIEW)
                                        Text("Estamos verificando tu recarga.", color = PurplePrimary, fontSize = 13.sp)
                                    if (current && status == ClaimStatus.RECEIVED)
                                        Text("Recibimos tu reclamo para evaluación.", color = PurplePrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                claim.resolution?.let { resolution ->
                    item { ClaimPanel { DetailField("Respuesta al reclamo", resolution) } }
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = PurplePrimary, fontSize = 14.sp)
        Text(value, color = PurpleDark, fontSize = 16.sp)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ClaimsScreenPreview() {
    ProyectoPMACTheme(darkTheme = false) {
        ClaimsScreen(ClaimSamples.claims, ClaimSamples.cards, "all", ClaimFilter.ALL, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ClaimDetailScreenPreview() {
    ProyectoPMACTheme(darkTheme = false) { ClaimDetailScreen(ClaimSamples.claims.first(), {}, {}, {}) }
}

