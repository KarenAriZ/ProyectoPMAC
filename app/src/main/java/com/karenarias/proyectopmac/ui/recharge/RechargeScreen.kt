
package com.karenarias.proyectopmac.ui.recharge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.theme.*
import com.karenarias.proyectopmac.ui.components.MetroGoBottomBar
import com.karenarias.proyectopmac.ui.components.MetroGoTopBar
import com.karenarias.proyectopmac.data.remote.UserCard
import com.karenarias.proyectopmac.data.remote.UserRecharge
import com.karenarias.proyectopmac.data.remote.SimulatedRechargeRequest

@Composable
fun RechargeScreen(
    modifier: Modifier = Modifier,
    cardNumber: String = "",
    onTabSelected: (Int) -> Unit = {},
    onCardsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    allowRecharge: Boolean = true,
    cards: List<UserCard> = emptyList(),
    onCardSelected: (String) -> Unit = {},
    isSavingRecharge: Boolean = false,
    saveError: String? = null,
    pending: SimulatedRechargeRequest? = null,
    receipt: UserRecharge? = null,
    onSubmit: (String, Long, String) -> Unit = { _, _, _ -> },
    paymentCard: PaymentCardSummary? = null,
    onAddPaymentCard: () -> Unit = {}
) {
    // El servidor confirma el comprobante; la pantalla no inventa registros.
    var selectedAmount by rememberSaveable { mutableStateOf("20") }
    var selectedPayment by rememberSaveable { mutableStateOf("Yape") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val rechargeConfirmed = receipt != null
    val confirmedAmount = receipt?.let { "%.2f".format(java.util.Locale.US, it.montoCentimos / 100.0) }.orEmpty()
    val confirmedPayment = receipt?.metodoPago.orEmpty()
    val confirmedDate = receipt?.historyRecord()?.let { "${it.date} ${it.time}" }.orEmpty()
    val operationNumber = receipt?.id.orEmpty()
    val rechargeError = saveError.orEmpty()
    val locked = isSavingRecharge || pending != null
    val needsPaymentCard = selectedPayment == "Tarjeta Débito / Crédito Visa" && paymentCard == null
    val activeCards = cards.filter { it.isActive }
    val selectedCard = activeCards.firstOrNull { it.numeroTarjeta == cardNumber }
    var cardMenuExpanded by remember { mutableStateOf(false) }
    val confirmedCard = receipt?.let { "${it.aliasTarjeta} · •••• ${it.numeroTarjeta.takeLast(4)}" }.orEmpty()
    val targetNumber = pending?.numeroTarjeta ?: cardNumber
    val targetLabel = cards.firstOrNull { it.numeroTarjeta == targetNumber }?.let { "${it.alias} · ${it.maskedNumber}" }
        ?: "•••• ${targetNumber.takeLast(4)}"
    LaunchedEffect(receipt?.id) { if (receipt != null) showPaymentDialog = false }
    LaunchedEffect(cardNumber) {
        showPaymentDialog = false
    }
    val amountValue = selectedAmount.toDoubleOrNull()
    val isValidAmount = amountValue != null &&
            amountValue > 0.0 &&
            amountValue <= 500.0

    val formattedAmount = if (isValidAmount) {
        String.format(
            java.util.Locale.US,
            "%.2f",
            amountValue
        )
    } else {
        "0.00"
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundCream,
        topBar = {
            MetroGoTopBar(
                title = "Tarjetas", onTitleClick = onCardsClick, onProfileClick = onProfileClick)
        },
        bottomBar = {
            MetroGoBottomBar(
                selectedTab = 3,
                onTabSelected = onTabSelected
            )
        }
    ) { innerPadding ->

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(BackgroundCream)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardWhite
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Recarga de Saldo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                Text(
                    text = "Selecciona la tarjeta y el monto de tu recarga",
                    fontSize = 13.sp,
                    color = PurplePrimary
                )

                if (!allowRecharge) Text("La recarga de tu cuenta estará disponible cuando conectemos el servicio de recargas.", color = PurplePrimary)
                if (pending != null) {
                    Text("Hay una recarga pendiente de confirmar para $targetLabel. Reintenta con los mismos datos para evitar duplicados.", color = PurpleDark)
                    Text("Código: ${pending.idOperacion}", color = PurplePrimary, fontSize = 12.sp)
                }
                if (saveError != null) Text(saveError, color = MaterialTheme.colorScheme.error)

                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { cardMenuExpanded = true },
                        enabled = activeCards.isNotEmpty() && !locked,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tarjeta a recargar", color = PurplePrimary, fontSize = 12.sp)
                            Text(
                                pending?.let { targetLabel } ?: selectedCard?.let { "${it.alias} · ${it.maskedNumber}" }
                                    ?: "Selecciona una tarjeta",
                                color = PurpleDark, fontWeight = FontWeight.Bold
                            )
                        }
                        Text("▾", color = PurpleDark)
                    }
                    DropdownMenu(expanded = cardMenuExpanded, onDismissRequest = { cardMenuExpanded = false }) {
                        activeCards.forEach { card ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(card.alias, color = PurpleDark, fontWeight = FontWeight.Bold)
                                        Text("${card.maskedNumber} · ${card.tipo}", color = PurplePrimary)
                                    }
                                },
                                onClick = { onCardSelected(card.numeroTarjeta); cardMenuExpanded = false }
                            )
                        }
                    }
                }
                if (activeCards.isEmpty()) {
                    Text("Agrega o reactiva una tarjeta para poder recargar.", color = PurpleDark)
                    TextButton(onClick = onCardsClick) { Text("Ir a Mis tarjetas", color = PurplePrimary) }
                }

                // Selección del monto
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(10, 20, 50).forEach { amount ->
                        val isSelected = selectedAmount == amount.toString()

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !locked) {
                                    selectedAmount = amount.toString()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PurplePrimary
                            else BackgroundCream
                        ) {
                            Box(
                                modifier = Modifier.padding(
                                    vertical = 16.dp
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "S/ $amount.00",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CardWhite
                                    else PurpleDark
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    enabled = !locked,
                    value = pending?.let { "%.2f".format(java.util.Locale.US, it.montoCentimos / 100.0) } ?: selectedAmount,
                    onValueChange = { newValue ->
                        if (
                            newValue.isEmpty() ||
                            (newValue.matches(Regex("\\d{0,5}(\\.\\d{0,2})?")))
                        ) {
                            selectedAmount = newValue
                        }
                    },
                    label = {
                        Text("Otro monto (S/)")
                    },
                    placeholder = {
                        Text("Ejemplo: 8.50")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = metroGoTextFieldColors()
                )

                Text(
                    text = "Método de Pago",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                // Métodos de pago
                listOf(
                    "Yape",
                    "Plin",
                    "Tarjeta Débito / Crédito Visa"
                ).forEach { method ->
                    val isSelected = (pending?.metodoPago ?: selectedPayment) == method

                    PaymentMethodOption(
                        method = method,
                        selected = isSelected,
                        onClick = {
                            if (!locked) {
                                selectedPayment = method
                                if (method == "Tarjeta Débito / Crédito Visa" && paymentCard == null) onAddPaymentCard()
                            }
                        }
                    )
                }
                if ((pending?.metodoPago ?: selectedPayment) == "Tarjeta Débito / Crédito Visa") {
                    paymentCard?.let { Text(it.label, color = PurpleDark, fontWeight = FontWeight.Bold) }
                    if (!locked) OutlinedButton(onClick = onAddPaymentCard, modifier = Modifier.fillMaxWidth()) {
                        Text(if (paymentCard == null) "Añadir nueva tarjeta" else "Cambiar tarjeta bancaria", color = PurplePrimary)
                    }
                }
                val paymentMessage = when (pending?.metodoPago ?: selectedPayment) {
                    "Yape" -> "Simulación de recarga con Yape. No se realizará ningún cobro."
                    "Plin" -> "Simulación de recarga con Plin. No se realizará ningún cobro."
                    else -> "Simulación de recarga con tarjeta bancaria. No se realizará ningún cobro."
                }

                Text(
                    text = paymentMessage,
                    fontSize = 12.sp,
                    color = PurplePrimary
                )

                Button(
                    onClick = {
                        if (allowRecharge && (pending != null || (isValidAmount && selectedCard != null && !needsPaymentCard))) {
                            showPaymentDialog = true
                        }
                    },
                    enabled = allowRecharge && !isSavingRecharge && (pending != null || (isValidAmount && selectedCard != null && !needsPaymentCard)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    )
                ) {
                    Text(
                        text = if (isSavingRecharge) "Registrando recarga…" else if (pending != null) "Reintentar registro" else "Confirmar Recarga S/ $formattedAmount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardWhite
                    )
                }

                HorizontalDivider(
                    color = MauveAccent.copy(alpha = 0.3f)
                )

                Text(
                    text = "Comprobante de Recarga",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                if (rechargeConfirmed) {
                    Text("Tarjeta: $confirmedCard", color = PurpleDark, fontSize = 14.sp)
                    Text(
                        text = "✓ Recarga exitosa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    Text(
                        text = "Monto: S/ $confirmedAmount",
                        fontSize = 14.sp,
                        color = PurpleDark
                    )

                    Text(
                        text = "Método de pago: $confirmedPayment",
                        fontSize = 14.sp,
                        color = PurpleDark
                    )
                    Text(
                        text = "Fecha: $confirmedDate",
                        fontSize = 14.sp,
                        color = PurpleDark
                    )

                    Text(
                        text = "N.º de operación: $operationNumber",
                        fontSize = 13.sp,
                        color = PurpleDark
                    )

                } else {
                    Text(
                        text = "El comprobante aparecerá aquí cuando se confirme una recarga.",
                        fontSize = 13.sp,
                        color = PurpleDark
                    )
                }
            }
        }
    }
    }

    if (showPaymentDialog && (selectedCard != null || pending != null)) {
        AlertDialog(
            onDismissRequest = {
                if (!isSavingRecharge) {
                    showPaymentDialog = false
                }
            },
            title = {
                Text("Confirmar recarga")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tarjeta: $targetLabel")
                    Text("Monto: S/ ${pending?.let { "%.2f".format(java.util.Locale.US, it.montoCentimos / 100.0) } ?: formattedAmount}")
                    Text("Método de pago: ${pending?.metodoPago ?: selectedPayment}")
                    if ((pending?.metodoPago ?: selectedPayment) == "Tarjeta Débito / Crédito Visa")
                        paymentCard?.let { Text(it.label) }
                    Text(
                        "Esta es una simulación académica. " +
                                "No se realizará ningún cobro real."
                    )
                    if (rechargeError.isNotEmpty()) {
                        Text(
                            text = rechargeError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSavingRecharge,
                    onClick = {
                        if (!isSavingRecharge) {
                            val cents = pending?.montoCentimos ?: selectedAmount.toBigDecimal().movePointRight(2).longValueExact()
                            onSubmit(targetNumber, cents, pending?.metodoPago ?: selectedPayment)
                        }
                    }
                ) {
                    Text(
                        if (isSavingRecharge) "Registrando..."
                        else "Simular recarga"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSavingRecharge,
                    onClick = {
                        showPaymentDialog = false
                    }
                ){
                    Text("Cancelar")
                }
            },
            containerColor = CardWhite,
            titleContentColor = PurpleDark,
            textContentColor = PurpleDark
        )
    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
fun RechargeScreenPreview() {
    ProyectoPMACTheme {
        var selectedNumber by remember { mutableStateOf("8754785478") }
        RechargeScreen(
            cardNumber = selectedNumber,
            cards = listOf(
                UserCard("8754785478", "Mi tarjeta", "General", "ACTIVA"),
                UserCard("1234567890", "Universidad", "Universitario", "ACTIVA")
            ),
            onCardSelected = { selectedNumber = it },
            allowRecharge = false
        )
    }
}
