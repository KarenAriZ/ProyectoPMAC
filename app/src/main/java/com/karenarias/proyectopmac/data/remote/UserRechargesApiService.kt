package com.karenarias.proyectopmac.data.remote

import retrofit2.Response
import retrofit2.http.*

data class SimulatedRechargeRequest(
    val idOperacion: String, val numeroTarjeta: String,
    val montoCentimos: Long, val metodoPago: String
)

data class UserRecharge(
    val id: String, val numeroTarjeta: String, val aliasTarjeta: String,
    val tipoTarjeta: String, val montoCentimos: Long, val metodoPago: String,
    val fecha: String, val estado: String, val simulada: Boolean,
    val saldoAnteriorCentimos: Long, val saldoPosteriorCentimos: Long
)

data class UserRechargesPage(val items: List<UserRecharge>?, val nextKey: String? = null)

interface UserRechargesApiService {
    @POST("recargas")
    suspend fun create(@Header("Authorization") authorization: String,
                       @Body request: SimulatedRechargeRequest): Response<UserCardsResponse<UserRecharge>>

    @GET("recargas")
    suspend fun list(@Header("Authorization") authorization: String,
                     @Query("nextKey") nextKey: String? = null): Response<UserCardsResponse<UserRechargesPage>>
}
