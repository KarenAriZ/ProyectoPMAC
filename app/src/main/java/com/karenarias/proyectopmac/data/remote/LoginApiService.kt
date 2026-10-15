package com.karenarias.proyectopmac.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

class LoginRequest(val correoElectronico: String, val contrasena: String)

// No generar toString: la respuesta contiene un token privado.
class LoginData(
    val usuario: UsuarioRegistrado? = null,
    val accessToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Long? = null,
    val expiresAt: Long? = null
)

class LoginResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: LoginData? = null
)

interface LoginApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
