package com.karenarias.proyectopmac.data.repository

import com.google.gson.Gson
import com.karenarias.proyectopmac.data.remote.*
import java.util.Locale

class RegistroException(message: String) : Exception(message)

class RegistroRepository(private val api: UsuarioApiService = UsuarioApiClient.api) {
    suspend fun registrar(request: RegistroUsuarioRequest): UsuarioRegistrado {
        val response = api.registrarUsuario(request)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && !body.data?.id.isNullOrBlank()) {
            return body.data!!
        }

        val errorText = response.errorBody()?.use { it.string() }
        val serverMessage = runCatching {
            Gson().fromJson(errorText, RegistroUsuarioResponse::class.java)?.message
        }.getOrNull()?.takeIf { it.isNotBlank() && it.length <= 300 }

        throw RegistroException(when (response.code()) {
            400 -> serverMessage ?: "Revisa los datos del formulario."
            409 -> "Ya existe una cuenta con ese correo electrónico."
            429 -> "Hay demasiados intentos. Espera un momento y vuelve a intentarlo."
            in 500..599 -> "El servidor no pudo confirmar el registro. Inténtalo más tarde."
            else -> "No se pudo confirmar la creación de la cuenta. Inténtalo más tarde."
        })
    }
}

fun registroRequest(nombre: String, apellido: String, correo: String, password: String) =
    RegistroUsuarioRequest(nombre.trim(), apellido.trim(), correo.trim().lowercase(Locale.ROOT), password)

fun validarRegistro(request: RegistroUsuarioRequest, confirmation: String): String? {
    if (listOf(request.nombre, request.apellido, request.correoElectronico, request.contrasena, confirmation)
            .any { it.isBlank() }) return "Todos los campos son obligatorios"
    // Python len cuenta puntos de código Unicode; mantener el mismo límite de la Lambda.
    fun String.characters() = codePointCount(0, length)
    if (request.nombre.characters() > 100 || request.apellido.characters() > 100)
        return "Nombre y apellido deben tener como máximo 100 caracteres"
    if (request.correoElectronico.characters() > 254 ||
        !Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+").matches(request.correoElectronico))
        return "El correo electrónico no es válido"
    if (request.contrasena.characters() !in 11..128)
        return "La contraseña debe tener entre 11 y 128 caracteres"
    if (request.contrasena != confirmation) return "Las contraseñas no coinciden"
    return null
}

