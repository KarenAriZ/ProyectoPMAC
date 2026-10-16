package com.karenarias.proyectopmac.data.remote

import retrofit2.Response
import retrofit2.http.*

data class UserCard(
    val numeroTarjeta: String,
    val alias: String,
    val tipo: String,
    val estado: String,
    val saldoCentimos: Long? = null,
    val saldoSimulado: Boolean = false,
    val estadoTarjeta: String? = null,
    val fechaAlta: String? = null,
    val fechaBaja: String? = null,
    val umbralSaldoBajoCentimos: Long = 500,
    val alertasActivas: Boolean = true
) {
    val isActive: Boolean get() = estado == "ACTIVA"
    val maskedNumber: String get() = "•••• ${numeroTarjeta.takeLast(4)}"
}

class UserCardRequest(val numeroTarjeta: String, val alias: String, val tipo: String)
class UserCardStateRequest(val estado: String)
class UserCardsPage(val items: List<UserCard>? = null, val nextKey: String? = null)
class UserCardsResponse<T>(val success: Boolean = false, val message: String? = null, val data: T? = null)

interface UserCardsApiService {
    @GET("mis-tarjetas")
    suspend fun list(
        @Header("Authorization") authorization: String,
        @Query("mostrarInactivas") includeInactive: Boolean = true,
        @Query("nextKey") nextKey: String? = null
    ): Response<UserCardsResponse<UserCardsPage>>

    @POST("mis-tarjetas")
    suspend fun add(@Header("Authorization") authorization: String, @Body card: UserCardRequest): Response<UserCardsResponse<UserCard>>

    @PATCH("mis-tarjetas/{numeroTarjeta}")
    suspend fun changeState(
        @Header("Authorization") authorization: String,
        @Path("numeroTarjeta") number: String,
        @Body state: UserCardStateRequest
    ): Response<UserCardsResponse<UserCard>>
}
