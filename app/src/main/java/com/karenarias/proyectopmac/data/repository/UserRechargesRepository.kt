package com.karenarias.proyectopmac.data.repository

import com.karenarias.proyectopmac.data.remote.*
import retrofit2.Response
import java.time.Instant
import java.util.UUID

class UserRechargesRepository(
    private val api: UserRechargesApiService = UsuarioApiClient.rechargesApi,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    private fun authorization(session: UserSession): String {
        if (session.isExpired(nowSeconds()) || session.accessToken.isBlank())
            throw CardsException(401, "Tu sesión venció. Inicia sesión nuevamente.")
        return "Bearer ${session.accessToken}"
    }

    private fun <T> data(response: Response<UserCardsResponse<T>>): T {
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) return body.data
        response.errorBody()?.close()
        throw CardsException(response.code(), when (response.code()) {
            401 -> "Tu sesión no es válida. Inicia sesión nuevamente."
            400 -> "Revisa la tarjeta, el monto y el método elegido."
            404 -> "No se encontró la tarjeta o la ruta de recargas."
            409 -> "No se pudo completar la operación. Actualiza tus tarjetas y reintenta con el mismo código."
            else -> "No se pudo confirmar el registro de la recarga. Reintenta con el mismo código."
        })
    }

    private fun verified(item: UserRecharge): UserRecharge {
        if (item.id.isNullOrBlank() || !item.numeroTarjeta.orEmpty().matches(Regex("[0-9]{8,16}")) ||
            item.aliasTarjeta.isNullOrBlank() || item.tipoTarjeta.isNullOrBlank() ||
            item.montoCentimos !in 1..50000 || !item.simulada || item.estado != "SIMULADA" ||
            item.metodoPago !in listOf("Yape", "Plin", "Tarjeta Débito / Crédito Visa") ||
            item.saldoAnteriorCentimos < 0 || item.saldoPosteriorCentimos != item.saldoAnteriorCentimos + item.montoCentimos ||
            runCatching { Instant.parse(item.fecha) }.isFailure)
            throw CardsException(502, "El servidor devolvió un comprobante incompleto. Reintenta con el mismo código.")
        return item
    }

    suspend fun create(session: UserSession, request: SimulatedRechargeRequest): UserRecharge {
        if (runCatching { UUID.fromString(request.idOperacion).toString() == request.idOperacion }.getOrDefault(false).not() ||
            !request.numeroTarjeta.matches(Regex("[0-9]{8,16}")) || request.montoCentimos !in 1..50000 ||
            request.metodoPago !in listOf("Yape", "Plin", "Tarjeta Débito / Crédito Visa"))
            throw CardsException(400, "Revisa los datos de la recarga.")
        val item = verified(data(api.create(authorization(session), request)))
        if (item.id != request.idOperacion || item.numeroTarjeta != request.numeroTarjeta ||
            item.montoCentimos != request.montoCentimos || item.metodoPago != request.metodoPago)
            throw CardsException(502, "El comprobante no corresponde a la recarga solicitada.")
        return item
    }

    suspend fun list(session: UserSession): List<UserRecharge> {
        val items = mutableListOf<UserRecharge>()
        val seen = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = data(api.list(authorization(session), cursor))
            items += (page.items ?: throw CardsException(502, "No se pudo interpretar el historial.")).map(::verified)
            cursor = page.nextKey?.takeIf { it.isNotBlank() }
            if (cursor != null && (!seen.add(cursor) || seen.size > 100))
                throw CardsException(502, "No se pudo completar el historial.")
        } while (cursor != null)
        return items.distinctBy { it.id }.sortedByDescending { Instant.parse(it.fecha) }
    }
}
