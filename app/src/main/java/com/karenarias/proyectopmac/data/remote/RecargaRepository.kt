package com.karenarias.proyectopmac.data.remote

import com.karenarias.proyectopmac.data.model.RecargaDto

class RecargaRepository {

    suspend fun obtenerRecargas(): List<RecargaDto> {
        return RetrofitClient.recargaApi.obtenerRecargas()
    }
    suspend fun registrarRecarga(
        recarga: RecargaDto
    ): RegistroRecargaResponse {
        return RetrofitClient.recargaApi.registrarRecarga(recarga)
    }
}