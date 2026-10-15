package com.karenarias.proyectopmac.data.repository

import com.karenarias.proyectopmac.data.remote.*
import java.util.Locale

class LoginException(message: String) : Exception(message)

/** Sesión solo en memoria; no guarda contraseña ni token en archivos o logs. */
class UserSession(val user: UsuarioRegistrado, val accessToken: String, val expiresAt: Long) {
    val displayName: String get() = listOfNotNull(user.nombre, user.apellido)
        .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Usuario" }
    fun isExpired(nowSeconds: Long = System.currentTimeMillis() / 1000) = nowSeconds >= expiresAt
}

class LoginRepository(
    private val api: LoginApiService = UsuarioApiClient.loginApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    suspend fun login(request: LoginRequest): UserSession {
        val response = api.login(request)
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw LoginException(when (response.code()) {
                400 -> "Revisa el correo y la contraseña ingresados."
                401 -> "Correo o contraseña incorrectos."
                429 -> "Demasiados intentos. Espera un momento y vuelve a intentarlo."
                in 500..599 -> "No se pudo iniciar sesión. Inténtalo más tarde."
                else -> "El servicio de inicio de sesión no está disponible. Inténtalo más tarde."
            })
        }
        val body = response.body()
        val data = body?.data
        val user = data?.usuario
        val token = data?.accessToken
        val expiry = data?.expiresAt
        if (body?.success != true || user?.id.isNullOrBlank() || token.isNullOrBlank() ||
            data?.tokenType != "Bearer" || expiry == null || expiry <= nowSeconds()) {
            throw LoginException("El servidor no devolvió una sesión válida. Vuelve a intentarlo.")
        }
        return UserSession(user!!, token, expiry)
    }
}

fun loginRequest(email: String, password: String) = LoginRequest(email.trim().lowercase(Locale.ROOT), password)

fun validarLogin(request: LoginRequest): String? = when {
    request.correoElectronico.isBlank() || request.contrasena.isBlank() -> "Ingresa tu correo electrónico y contraseña."
    request.correoElectronico.length > 254 ||
        !Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+").matches(request.correoElectronico) -> "Ingresa un correo electrónico válido."
    request.contrasena.codePointCount(0, request.contrasena.length) > 128 -> "La contraseña debe tener como máximo 128 caracteres."
    else -> null
}
