package com.karenarias.proyectopmac.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

// Nombres JSON acordados con la Lambda de registro (con soporte alternate para snake_case/diferentes backends).
class RegistroUsuarioRequest(
    @SerializedName("nombre", alternate = ["first_name", "name"])
    val nombre: String,
    @SerializedName("apellido", alternate = ["last_name", "surname"])
    val apellido: String,
    @SerializedName("correoElectronico", alternate = ["email", "correo"])
    val correoElectronico: String,
    @SerializedName("contrasena", alternate = ["password", "passwordHash"])
    val contrasena: String
)

data class UsuarioRegistrado(
    @SerializedName("id", alternate = ["userId", "user_id", "sub"])
    val id: String? = null,
    @SerializedName("nombre", alternate = ["first_name", "name"])
    val nombre: String? = null,
    @SerializedName("apellido", alternate = ["last_name", "surname"])
    val apellido: String? = null,
    @SerializedName("correoElectronico", alternate = ["email", "correo"])
    val correoElectronico: String? = null
)

data class RegistroUsuarioResponse(
    @SerializedName("success", alternate = ["ok", "status"])
    val success: Boolean = false,
    @SerializedName("message", alternate = ["msg", "error"])
    val message: String? = null,
    @SerializedName("data", alternate = ["result", "payload"])
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
