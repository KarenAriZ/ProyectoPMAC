package com.karenarias.proyectopmac.data.recharge

import com.karenarias.proyectopmac.data.card.TarjetasApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HU-006: trae los puntos de recarga del Lambda "metrogo-tarjetas" (GET /puntos-recarga,
 * tabla DynamoDB "PuntosRecarga").
 * Devuelve null si AWS no está configurado o no responde; en ese caso la app usa la lista local.
 */
suspend fun RechargePointsRepository.fetchFromAws(): List<RechargePoint>? {
    val api = TarjetasApiClient.api ?: return null
    return try {
        val response = withContext(Dispatchers.IO) { api.listarPuntosRecarga() }
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) return null
        body.data.orEmpty().mapNotNull { dto ->
            val id = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val lat = dto.latitud ?: return@mapNotNull null
            val lng = dto.longitud ?: return@mapNotNull null
            RechargePoint(
                id = id,
                name = dto.nombre.orEmpty().ifBlank { id },
                district = dto.distrito.orEmpty(),
                description = dto.descripcion.orEmpty(),
                latitude = lat,
                longitude = lng
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
