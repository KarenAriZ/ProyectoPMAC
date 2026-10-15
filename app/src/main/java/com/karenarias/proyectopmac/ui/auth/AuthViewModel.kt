package com.karenarias.proyectopmac.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.repository.*
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.io.InterruptedIOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * ViewModel que administra los estados y la lógica de autenticación (Login, Registro y Recuperación)
 * para la aplicación MetroGo, manteniendo las pantallas stateless y desacopladas.
 */
class AuthViewModel : ViewModel() {
    private val loginRepository by lazy { LoginRepository() }
    private var loginJob: Job? = null
    private var sessionJob: Job? = null
    var session by mutableStateOf<UserSession?>(null)
        private set

    fun cancelLogin() {
        loginJob?.cancel()
        loginIsLoading = false
        loginPassword = ""
    }

    fun logout() {
        cancelLogin()
        sessionJob?.cancel()
        session = null
        loginErrorMessage = null
    }

    fun checkSessionExpiration() {
        if (session?.isExpired() == true) {
            logout()
            loginErrorMessage = "Tu sesión venció. Inicia sesión nuevamente."
        }
    }

    private val registroRepository by lazy { RegistroRepository() }
    var registerSucceeded by mutableStateOf(false)
        private set

    fun acknowledgeRegistration() { registerSucceeded = false }

    // --- Estado para LoginScreen ---
    var loginEmail by mutableStateOf("")
        private set
    var loginPassword by mutableStateOf("")
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
        if (loginIsLoading) return
        val request = loginRequest(loginEmail, loginPassword)
        loginErrorMessage = validarLogin(request)
        if (loginErrorMessage != null) return
        loginIsLoading = true
        loginJob = viewModelScope.launch {
            try {
                session = loginRepository.login(request)
                loginEmail = request.correoElectronico
                loginPassword = ""
                sessionJob?.cancel()
                sessionJob = viewModelScope.launch {
                    while (session != null) {
                        val remaining = (session!!.expiresAt - System.currentTimeMillis() / 1000).coerceAtLeast(1)
                        delay(remaining * 1000)
                        checkSessionExpiration()
                    }
                }
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: LoginException) {
                loginErrorMessage = error.message
            } catch (error: InterruptedIOException) {
                loginErrorMessage = "El servidor tardó demasiado. Vuelve a intentarlo."
            } catch (error: IOException) {
                loginErrorMessage = "No pudimos conectarnos. Revisa tu conexión e inténtalo nuevamente."
            } catch (error: Exception) {
                loginErrorMessage = "No se pudo interpretar la respuesta del servidor. Inténtalo más tarde."
            } finally {
                loginIsLoading = false
            }
        }
    }
    // --- Estado para RegisterScreen ---
    var registerFirstName by mutableStateOf("")
        private set
    var registerLastName by mutableStateOf("")
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

    fun onRegisterFirstNameChanged(name: String) {
        registerFirstName = name
        registerErrorMessage = null
    }

    fun onRegisterLastNameChanged(lastName: String) {
        registerLastName = lastName
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

    fun register() {
        if (registerIsLoading || registerSucceeded) return
        val request = registroRequest(registerFirstName, registerLastName, registerEmail, registerPassword)
        registerErrorMessage = validarRegistro(request, registerConfirmPassword)
        if (registerErrorMessage != null) return

        registerIsLoading = true
        viewModelScope.launch {
            try {
                registroRepository.registrar(request)
                loginEmail = request.correoElectronico
                loginPassword = ""
                loginErrorMessage = null
                registerFirstName = ""
                registerLastName = ""
                registerEmail = ""
                registerPassword = ""
                registerConfirmPassword = ""
                registerSucceeded = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: RegistroException) {
                registerErrorMessage = error.message
            } catch (error: InterruptedIOException) {
                registerErrorMessage = "Se agotó el tiempo de espera. No pudimos confirmar el registro. Si al reintentar el correo ya existe, la cuenta pudo haberse creado."
            } catch (error: IOException) {
                registerErrorMessage = "No pudimos confirmar el registro. Revisa tu conexión. Si al reintentar el correo ya existe, la cuenta pudo haberse creado."
            } catch (error: Exception) {
                registerErrorMessage = "No se pudo interpretar la respuesta del servidor. Inténtalo más tarde."
            } finally {
                registerIsLoading = false
            }
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




