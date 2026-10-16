package com.karenarias.proyectopmac.ui.recharge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.R
import com.karenarias.proyectopmac.ui.theme.*

/** Presentación visual; el método seleccionado sigue siendo administrado por RechargeScreen. */
@Composable
fun PaymentMethodOption(method: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected, role = Role.RadioButton, onClick = onClick
        ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MauveAccent.copy(alpha = 0.16f) else CardWhite,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PurplePrimary else MauveAccent.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                    when (method) {
                        "Yape" -> Image(
                            painterResource(R.drawable.payment_yape), contentDescription = null,
                            contentScale = ContentScale.Fit, modifier = Modifier.size(56.dp)
                        )
                        "Plin" -> Image(
                            painterResource(R.drawable.payment_plin), contentDescription = null,
                            contentScale = ContentScale.Fit, modifier = Modifier.size(46.dp)
                        )
                        else -> Icon(
                            Icons.Default.CreditCard, contentDescription = null,
                            tint = PurplePrimary, modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (method == "Yape" || method == "Plin") method else "Tarjeta de débito / crédito",
                    color = PurpleDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (method == "Yape" || method == "Plin") "Billetera digital" else "Visa",
                    color = PurplePrimary, fontSize = 12.sp
                )
            }
            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) PurplePrimary else MauveAccent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PaymentMethodsPreview() {
    ProyectoPMACTheme(darkTheme = false) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PaymentMethodOption("Yape", true, {})
            PaymentMethodOption("Plin", false, {})
            PaymentMethodOption("Tarjeta Débito / Crédito Visa", false, {})
        }
    }
}
