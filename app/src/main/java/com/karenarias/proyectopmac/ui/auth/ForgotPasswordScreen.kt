package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karenarias.proyectopmac.ui.components.AuthTextField
import com.karenarias.proyectopmac.ui.components.PrimaryButton
import com.karenarias.proyectopmac.ui.theme.*

/**
 * Pantalla de recuperación de contraseña stateless para MetroGo.
 * Permite solicitar un enlace de restablecimiento al correo electrónico.
 */
@Composable
fun ForgotPasswordScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    successMessage: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Logo MetroGo",
                    tint = PurplePrimary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Recupera tu acceso",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                Text(
                    text = "Te enviaremos un enlace a tu correo para restablecer tu contraseña",
                    fontSize = 13.sp,
                    color = PurpleDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo email
                AuthTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "Correo electrónico",
                    icon = Icons.Default.Email
                )

                // Mensajes de error o éxito
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorWine,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (!successMessage.isNullOrEmpty()) {
                    Text(
                        text = successMessage,
                        color = PurplePrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botón Enviar Enlace
                PrimaryButton(
                    text = "Enviar enlace",
                    onClick = onSubmitClick,
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Enlace Volver a iniciar sesión
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Volver a iniciar sesión",
                        color = PurplePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ProyectoPMACTheme {
        var email by remember { mutableStateOf("") }

        ForgotPasswordScreen(
            email = email,
            onEmailChange = { email = it },
            onSubmitClick = {},
            onNavigateToLogin = {},
            isLoading = false,
            errorMessage = null,
            successMessage = null
        )
    }
}
