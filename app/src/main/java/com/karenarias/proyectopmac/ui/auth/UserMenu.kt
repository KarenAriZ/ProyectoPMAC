package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.karenarias.proyectopmac.ui.theme.*

@Composable
fun UserMenu(
    onProfile: () -> Unit,
    onContact: () -> Unit,
    onClaims: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilledIconButton(
            onClick = { expanded = !expanded },
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MauveAccent, contentColor = PurpleDark
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Abrir menú de usuario")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CardWhite,
            shape = RoundedCornerShape(16.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Perfil", color = PurpleDark) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = PurplePrimary) },
                onClick = { expanded = false; onProfile() }
            )
            DropdownMenuItem(
                text = { Text("Contactar", color = PurpleDark) },
                leadingIcon = { Icon(Icons.Default.Phone, null, tint = PurplePrimary) },
                onClick = { expanded = false; onContact() }
            )
            DropdownMenuItem(
                text = { Text("Historial de reclamos", color = PurpleDark) },
                leadingIcon = { Icon(Icons.Default.History, null, tint = PurplePrimary) },
                onClick = { expanded = false; onClaims() }
            )
            HorizontalDivider(color = MauveAccent.copy(alpha = 0.3f))
            DropdownMenuItem(
                text = { Text("Salir", color = ErrorWine) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = ErrorWine) },
                onClick = { expanded = false; onLogout() }
            )
        }
    }
}

@Composable
fun UserProfileDialog(name: String, email: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, null, tint = PurplePrimary) },
        title = { Text("Mi perfil", color = PurpleDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nombre y apellido", color = PurplePrimary)
                Text(name, color = PurpleDark, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Correo electrónico", color = PurplePrimary)
                Text(email, color = PurpleDark)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = PurplePrimary) } },
        containerColor = CardWhite
    )
}

@Composable
fun ContactUnavailableDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Phone, null, tint = PurplePrimary) },
        title = { Text("Contactar", color = PurpleDark) },
        text = { Text("No se pudo abrir el marcador en este dispositivo. Puedes contactarnos al +51 1 203 9001.", color = PurpleDark) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = PurplePrimary) } },
        containerColor = CardWhite
    )
}

@Preview(showBackground = true)
@Composable
private fun UserMenuPreview() {
    ProyectoPMACTheme(darkTheme = false) { UserMenu({}, {}, {}, {}) }
}
