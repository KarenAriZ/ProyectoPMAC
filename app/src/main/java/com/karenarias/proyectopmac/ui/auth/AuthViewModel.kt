package com.karenarias.proyectopmac.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel que administra los estados y la lógica de autenticación (Login, Registro y Recuperación)
 * para la aplicación MetroGo, manteniendo las pantallas stateless y desacopladas.
 */
class AuthViewModel : ViewModel() {

    // --- Estado para LoginScreen ---
    var loginEmail by mutableStateOf("karen@upc.edu")
        private set
    var loginPassword by mutableStateOf("123456")
        private set
    var loginIsLoading by mutableStateOf(false)
        private set
    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    fun onLoginEmailChanged(email: String) {
        loginEmail = email
        loginErrorMessage = null
    }

    fun onLoginPasswordChanged(password: String) {
        loginPassword = password
        loginErrorMessage = null
    }

    fun login(onSuccess: () -> Unit) {
        if (loginEmail.isBlank() || loginPassword.isBlank()) {
            loginErrorMessage = "Por favor completa todos los campos"
            return
        }

        viewModelScope.launch {
            loginIsLoading = true
            loginErrorMessage = null
            delay(1500) // Simulación de petición de red (backend / Firebase)
            
            // Simulación de credenciales válidas
            if (loginEmail.contains("@") && loginPassword.length >= 6) {
                loginIsLoading = false
                onSuccess()
            } else {
                loginIsLoading = false
                loginErrorMessage = "Correo inválido o contraseña menor a 6 caracteres"
            }
        }
    }

    // --- Estado para RegisterScreen ---
    var registerFullName by mutableStateOf("")
        private set
    var registerEmail by mutableStateOf("")
        private set
    var registerPassword by mutableStateOf("")
        private set
    var registerConfirmPassword by mutableStateOf("")
        private set
    var registerIsLoading by mutableStateOf(false)
        private set
    var registerErrorMessage by mutableStateOf<String?>(null)
        private set

    fun onRegisterFullNameChanged(name: String) {
        registerFullName = name
        registerErrorMessage = null
    }

    fun onRegisterEmailChanged(email: String) {
        registerEmail = email
        registerErrorMessage = null
    }

    fun onRegisterPasswordChanged(password: String) {
        registerPassword = password
        registerErrorMessage = null
    }

    fun onRegisterConfirmPasswordChanged(confirm: String) {
        registerConfirmPassword = confirm
        registerErrorMessage = null
    }

    fun register(onSuccess: () -> Unit) {
        if (registerFullName.isBlank() || registerEmail.isBlank() || registerPassword.isBlank() || registerConfirmPassword.isBlank()) {
            registerErrorMessage = "Todos los campos son obligatorios"
            return
        }
        if (registerPassword != registerConfirmPassword) {
            registerErrorMessage = "Las contraseñas no coinciden"
            return
        }

        viewModelScope.launch {
            registerIsLoading = true
            registerErrorMessage = null
            delay(1500) // Simulación de registro exitoso
            registerIsLoading = false
            onSuccess()
        }
    }

    // --- Estado para ForgotPasswordScreen ---
    var forgotEmail by mutableStateOf("")
        private set
    var forgotIsLoading by mutableStateOf(false)
        private set
    var forgotErrorMessage by mutableStateOf<String?>(null)
        private set
    var forgotSuccessMessage by mutableStateOf<String?>(null)
        private set

    fun onForgotEmailChanged(email: String) {
        forgotEmail = email
        forgotErrorMessage = null
        forgotSuccessMessage = null
    }

    fun sendRecoveryEmail() {
        if (forgotEmail.isBlank()) {
            forgotErrorMessage = "Ingresa tu correo electrónico"
            return
        }

        viewModelScope.launch {
            forgotIsLoading = true
            forgotErrorMessage = null
            forgotSuccessMessage = null
            delay(1500) // Simulación de envío de correo
            forgotIsLoading = false
            forgotSuccessMessage = "Enlace de recuperación enviado a $forgotEmail"
        }
    }
}
