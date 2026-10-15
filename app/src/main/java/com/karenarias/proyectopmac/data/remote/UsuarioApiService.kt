package com.karenarias.proyectopmac.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Nombres JSON acordados con la Lambda de registro.
// Clase normal para no generar un toString que exponga la contraseña.
class RegistroUsuarioRequest(
    val nombre: String,
    val apellido: String,
    val correoElectronico: String,
    val contrasena: String
)

data class UsuarioRegistrado(
    val id: String? = null,
    val nombre: String? = null,
    val apellido: String? = null,
    val correoElectronico: String? = null
)

data class RegistroUsuarioResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: UsuarioRegistrado? = null
)

interface UsuarioApiService {
    @POST("usuarios")
    suspend fun registrarUsuario(@Body usuario: RegistroUsuarioRequest): Response<RegistroUsuarioResponse>
}

object UsuarioApiClient {
    private val retrofit: Retrofit by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            // No repetir automáticamente un POST cuyo resultado podría ser incierto.
            .retryOnConnectionFailure(false)
            .build()
        Retrofit.Builder()
            .baseUrl("https://yxonw4vey5.execute-api.us-east-1.amazonaws.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    }
    val api: UsuarioApiService by lazy { retrofit.create(UsuarioApiService::class.java) }
    val loginApi: LoginApiService by lazy { retrofit.create(LoginApiService::class.java) }
}

