package com.karenarias.proyectopmac.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
 * Pantalla de registro stateless para MetroGo.
 * Permite a nuevos usuarios ingresar sus datos para digitalizar sus tarjetas.
 */
@Composable
fun RegisterScreen(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Encabezado
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Logo MetroGo",
                    tint = PurplePrimary,
                    modifier = Modifier.size(42.dp)
                )

                Text(
                    text = "Crea tu cuenta",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurpleDark
                )

                Text(
                    text = "Digitaliza tus tarjetas del Metropolitano",
                    fontSize = 13.sp,
                    color = PurpleDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Campos de texto
                AuthTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    placeholder = "Nombre completo",
                    icon = Icons.Default.Person
                )

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

                AuthTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = "Confirmar contraseña",
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

                Spacer(modifier = Modifier.height(4.dp))

                // Botón Principal
                PrimaryButton(
                    text = "Crear cuenta",
                    onClick = onRegisterClick,
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Enlace a Login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Ya tienes cuenta? ",
                        color = PurpleDark,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "Inicia sesión",
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
fun RegisterScreenPreview() {
    ProyectoPMACTheme {
        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        RegisterScreen(
            fullName = fullName,
            onFullNameChange = { fullName = it },
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            onRegisterClick = {},
            onNavigateToLogin = {},
            isLoading = false,
            errorMessage = null
        )
    }
}
