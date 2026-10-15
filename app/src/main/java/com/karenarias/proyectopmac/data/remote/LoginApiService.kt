package com.karenarias.proyectopmac.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

class LoginRequest(
    @SerializedName("correoElectronico", alternate = ["email", "correo"])
    val correoElectronico: String,
    @SerializedName("contrasena", alternate = ["password"])
    val contrasena: String
)

// Evita generar un toString que incluya el token de sesión.
class LoginData(
    @SerializedName("usuario", alternate = ["user", "userInfo"])
    val usuario: UsuarioRegistrado? = null,
    @SerializedName("accessToken", alternate = ["access_token", "token"])
    val accessToken: String? = null,
    @SerializedName("tokenType", alternate = ["token_type"])
    val tokenType: String? = null,
    @SerializedName("expiresIn", alternate = ["expires_in"])
    val expiresIn: Long? = null,
    @SerializedName("expiresAt", alternate = ["expires_at"])
    val expiresAt: Long? = null
)

class LoginResponse(
    @SerializedName("success", alternate = ["ok", "status"])
    val success: Boolean = false,
    @SerializedName("message", alternate = ["msg", "error"])
    val message: String? = null,
    @SerializedName("data", alternate = ["result", "payload"])
    val data: LoginData? = null
)

interface LoginApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}

