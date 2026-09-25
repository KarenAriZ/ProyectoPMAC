package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
 * Pantalla de inicio de sesión stateless para MetroGo.
 * Muestra una tarjeta centrada con campos de acceso, botones de acción y enlaces de recuperación o registro.
 */
@Composable
fun LoginScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null
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
                    text = "MetroGo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                Text(
                    text = "Recarga tu Metropolitano sin hacer cola",
                    fontSize = 14.sp,
                    color = PurpleDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campos de texto
                AuthTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "Correo electrónico",
                    icon = Icons.Default.Email
                )

                AuthTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "Contraseña",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                // Mensaje de error opcional
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorWine,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Enlace ¿Olvidaste tu contraseña?
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onNavigateToForgotPassword) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = PurplePrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Botón Principal
                PrimaryButton(
                    text = "Iniciar sesión",
                    onClick = onLoginClick,
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Enlace a Registro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿No tienes cuenta? ",
                        color = PurpleDark,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            text = "Regístrate",
                            color = PurplePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    ProyectoPMACTheme {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        LoginScreen(
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            onLoginClick = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {},
            isLoading = false,
            errorMessage = null
        )
    }
}
