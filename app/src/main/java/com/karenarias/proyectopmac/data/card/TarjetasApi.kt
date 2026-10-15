package com.karenarias.proyectopmac.data.card

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


object TarjetasApiConfig {
    const val BASE_URL = "https://kragcuw563.execute-api.us-east-1.amazonaws.com/"
}

/** Mismo formato de respuesta que las Lambdas de login y registro. */
class ApiRespuesta<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

class TarjetaDto(
    @SerializedName("numeroTarjeta") val numeroTarjeta: String? = null,
    @SerializedName("tipo") val tipo: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("saldoCentimos") val saldoCentimos: Long? = null,
    @SerializedName("umbralSaldoBajoCentimos") val umbralSaldoBajoCentimos: Long? = null,
    @SerializedName("alertasActivas") val alertasActivas: Boolean? = null
)

class AlertasRequest(
    @SerializedName("umbralSaldoBajoCentimos") val umbralSaldoBajoCentimos: Long,
    @SerializedName("alertasActivas") val alertasActivas: Boolean
)

/** tipo: "RECARGA" (con monto, ej. "20.00") o "VIAJE" (el Lambda descuenta la tarifa). */
class MovimientoRequest(
    @SerializedName("tipo") val tipo: String,
    @SerializedName("monto") val monto: String? = null
)

class PuntoRecargaDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("distrito") val distrito: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null
)

interface TarjetasApiService {
    @GET("tarjetas/{numeroTarjeta}")
    suspend fun consultarTarjeta(
        @Path("numeroTarjeta") numeroTarjeta: String,
        @Query("tipo") tipo: String
    ): Response<ApiRespuesta<TarjetaDto>>

    @PUT("tarjetas/{numeroTarjeta}/alertas")
    suspend fun configurarAlertas(
        @Path("numeroTarjeta") numeroTarjeta: String,
        @Body body: AlertasRequest
    ): Response<ApiRespuesta<TarjetaDto>>

    @POST("tarjetas/{numeroTarjeta}/movimientos")
    suspend fun registrarMovimiento(
        @Path("numeroTarjeta") numeroTarjeta: String,
        @Body body: MovimientoRequest
    ): Response<ApiRespuesta<TarjetaDto>>

    @GET("puntos-recarga")
    suspend fun listarPuntosRecarga(): Response<ApiRespuesta<List<PuntoRecargaDto>>>
}

object TarjetasApiClient {
    /** null cuando no hay URL configurada (modo local). */
    val api: TarjetasApiService? by lazy {
        val url = TarjetasApiConfig.BASE_URL.trim()
        if (url.isBlank()) return@lazy null
        val client = OkHttpClient.Builder()
            // Tiempos cortos: si el laboratorio de AWS está apagado, la app usa los datos guardados.
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
        Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TarjetasApiService::class.java)
    }
}
