package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.components.MetroGoTopBar
import com.karenarias.proyectopmac.ui.recharge.PaymentCardSummary
import com.karenarias.proyectopmac.ui.recharge.validatePaymentCard
import com.karenarias.proyectopmac.ui.theme.*

@Composable
fun AddPaymentCardScreen(
    onBack: () -> Unit, onSave: (PaymentCardSummary) -> Unit,
    onCards: () -> Unit = {}, onProfile: () -> Unit = {}
) {
    // No usar rememberSaveable: estos campos no se guardan en el estado restaurable.
    var number by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = BackgroundCream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { MetroGoTopBar(onTitleClick = onCards, onProfileClick = onProfile) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()
            .verticalScroll(rememberScrollState()).padding(20.dp)) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardWhite)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PurpleDark)
                    }
                    Text("Añadir nueva tarjeta", color = PurpleDark, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(Modifier.weight(0.2f).height(80.dp), color = MauveAccent.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)) {}
                        Surface(Modifier.weight(1f), color = PurplePrimary, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CreditCard, null, tint = CardWhite)
                                Text("••••  ••••  ••••  ${number.takeLast(4).padStart(4, '•')}", color = CardWhite, fontSize = 14.sp)
                                Text("TITULAR                     MM/AA", color = CardWhite, fontSize = 9.sp)
                            }
                        }
                        Surface(Modifier.weight(0.2f).height(80.dp), color = MauveAccent.copy(alpha = 0.35f), shape = RoundedCornerShape(8.dp)) {}
                    }
                    Text("Usa datos de prueba. Esta tarjeta se utilizará únicamente para simular el cobro.", color = PurplePrimary, fontSize = 12.sp)
                    Text("Número de la tarjeta", color = PurpleDark, fontSize = 14.sp)
                    OutlinedTextField(
                        value = number, onValueChange = { value ->
                            val digits = value.filter { it in '0'..'9' }
                            if (digits.length <= 19) { number = digits; error = null }
                        },
                        placeholder = { Text("Ejemplo: 4111111111111111") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = metroGoTextFieldColors()
                    )
                    Text("Fecha de caducidad", color = PurpleDark, fontSize = 14.sp)
                    OutlinedTextField(
                        value = if (expiry.length > 2) expiry.take(2) + "/" + expiry.drop(2) else expiry,
                        onValueChange = { value ->
                            val digits = value.filter { it in '0'..'9' }
                            if (digits.length <= 4) { expiry = digits; error = null }
                        }, placeholder = { Text("MM/AA") }, singleLine = true,
                        modifier = Modifier.width(140.dp), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = metroGoTextFieldColors()
                    )
                    Text("Nombre del titular", color = PurpleDark, fontSize = 14.sp)
                    OutlinedTextField(
                        value = holder, onValueChange = { if (it.length <= 80) { holder = it; error = null } },
                        placeholder = { Text("Nombres y apellidos") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), colors = metroGoTextFieldColors()
                    )
                    Text("CVV", color = PurpleDark, fontSize = 14.sp)
                    OutlinedTextField(
                        value = cvv, onValueChange = { value ->
                            if (value.length <= 4 && value.all { it in '0'..'9' }) { cvv = value; error = null }
                        }, placeholder = { Text("XXX") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.width(140.dp), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), colors = metroGoTextFieldColors()
                    )
                    error?.let { Text(it, color = ErrorWine, fontSize = 13.sp) }
                    Button(onClick = {
                        error = validatePaymentCard(number, expiry, holder, cvv)
                        if (error == null) {
                            val summary = PaymentCardSummary(number.takeLast(4))
                            number = ""; cvv = ""; holder = ""; expiry = ""
                            onSave(summary)
                        }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary, contentColor = CardWhite)) {
                        Text("Guardar tarjeta", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun AddPaymentCardPreview() {
    ProyectoPMACTheme { AddPaymentCardScreen({}, {}) }
}
