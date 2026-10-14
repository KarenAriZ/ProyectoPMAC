package com.karenarias.proyectopmac.data.remote

import com.karenarias.proyectopmac.data.model.RecargaDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class RegistroRecargaResponse(
    val mensaje: String,
    val id: String
)
interface RecargaApiService {

    @POST("recargas")
    suspend fun registrarRecarga(
        @Body recarga: RecargaDto
    ): RegistroRecargaResponse

    @GET("recargas")
    suspend fun obtenerRecargas(): List<RecargaDto>
}