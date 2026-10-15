
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.karenarias.proyectopmac.data.remote.RecargaRepository
import com.karenarias.proyectopmac.data.model.RecargaDto

@Composable
fun RechargeScreen(
    modifier: Modifier = Modifier,
    cardNumber: String = "",
    onRechargeConfirmed: (String, String) -> Unit = { _, _ -> },
    onTabSelected: (Int) -> Unit = {},
    onRechargeRecorded: (RecargaDto) -> Unit = {},
    onCardsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Datos temporales para probar la interfaz
    var selectedAmount by remember { mutableStateOf("20") }
    var selectedPayment by remember { mutableStateOf("Yape") }
    var rechargeConfirmed by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var confirmedAmount by remember { mutableStateOf("") }
    var confirmedPayment by remember { mutableStateOf("") }
    var confirmedDate by remember { mutableStateOf("") }
    var operationNumber by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val recargaRepository = remember { RecargaRepository() }
    var isSavingRecharge by remember { mutableStateOf(false) }
    var rechargeError by remember { mutableStateOf("") }
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
                    text = "Selecciona el monto a abonar en tarjeta *5610",
                    fontSize = 13.sp,
                    color = PurplePrimary
                )

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
                                .clickable {
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
                    value = selectedAmount,
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        focusedLabelColor = PurplePrimary
                    )
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
                    val isSelected = selectedPayment == method

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPayment = method
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected)
                            MauveAccent.copy(alpha = 0.25f)
                        else BackgroundCream,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected)
                                PurplePrimary
                            else MauveAccent.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = method,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurpleDark
                        )
                    }
                }
                val paymentMessage = when (selectedPayment) {
                    "Yape" -> "Continuarás con el proceso de pago mediante Yape."
                    "Plin" -> "Continuarás con el proceso de pago mediante Plin."
                    else -> "Continuarás con el pago mediante tarjeta bancaria."
                }

                Text(
                    text = paymentMessage,
                    fontSize = 12.sp,
                    color = PurplePrimary
                )

                Button(
                    onClick = {
                        if (isValidAmount) {
                            showPaymentDialog = true
                        }
                    },
                    enabled = isValidAmount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    )
                ) {
                    Text(
                        text = "Confirmar Recarga S/ $formattedAmount",
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
                    Text(
                        text = "✓ Recarga simulada exitosa",
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

                    Text(
                        text = "Comprobante de demostración. No válido como pago real.",
                        fontSize = 12.sp,
                        color = PurplePrimary
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

    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSavingRecharge) {
                    showPaymentDialog = false
                }
            },
            title = {
                Text("Confirmar recarga de prueba")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Monto: S/ $formattedAmount")
                    Text("Método de pago: $selectedPayment")
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
                            isSavingRecharge = true
                            rechargeError = ""

                            val montoActual = formattedAmount
                            val metodoActual = selectedPayment
                            val fechaActual = java.text.SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())

                            val idActual = "SIM-" + System.currentTimeMillis()

                            coroutineScope.launch {
                                try {
                                    val recarga = RecargaDto(
                                        id = idActual,
                                        numeroTarjeta = cardNumber,
                                        monto = montoActual,
                                        metodoPago = metodoActual,
                                        fecha = fechaActual,
                                        estado = "SIMULADA"
                                    )

                                    val respuesta = recargaRepository.registrarRecarga(recarga)

                                    if (respuesta.id == idActual) {
                                        onRechargeRecorded(recarga)
                                        confirmedAmount = montoActual
                                        confirmedPayment = metodoActual
                                        confirmedDate = fechaActual
                                        operationNumber = idActual

                                        rechargeConfirmed = true
                                        onRechargeConfirmed(
                                            confirmedAmount,
                                            confirmedPayment
                                        )
                                        showPaymentDialog = false
                                    } else {
                                        rechargeError = "No se pudo confirmar el registro en AWS."
                                    }

                                } catch (e: Exception) {
                                    rechargeError =
                                        "No se pudo confirmar el registro. Verifica tu conexión."
                                } finally {
                                    isSavingRecharge = false
                                }
                            }
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
                    enabled = !isSavingRecharge && cardNumber.isNotBlank(),
                    onClick = {
                        rechargeError = ""
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
        RechargeScreen()
    }
}
