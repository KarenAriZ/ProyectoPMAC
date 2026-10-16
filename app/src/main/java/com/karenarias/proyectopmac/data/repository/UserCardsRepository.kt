package com.karenarias.proyectopmac.data.repository

import com.google.gson.Gson
import com.karenarias.proyectopmac.data.remote.*
import retrofit2.Response

class CardsException(val status: Int, message: String) : Exception(message)

class UserCardsRepository(
    private val api: UserCardsApiService = UsuarioApiClient.cardsApi,
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
        val rawError = response.errorBody()?.use { it.string() }
        val message = runCatching { Gson().fromJson(rawError, UserCardsResponse::class.java)?.message }
            .getOrNull()?.takeIf { it.isNotBlank() && it.length <= 250 }
        throw CardsException(response.code(), when (response.code()) {
            401 -> "Tu sesión no es válida. Inicia sesión nuevamente."
            400 -> message ?: "Revisa los datos de la tarjeta."
            404 -> "No se encontró la tarjeta o la ruta del servicio. Actualiza e inténtalo nuevamente."
            409 -> message ?: "No se pudo completar el cambio. Actualiza tus tarjetas."
            429 -> "Demasiados intentos. Espera un momento y vuelve a intentarlo."
            in 500..599 -> "No se pudo conectar con el servicio de tarjetas. Inténtalo más tarde."
            else -> "El servidor no confirmó la operación de tarjetas."
        })
    }

    private fun verified(card: UserCard): UserCard {
        // Gson puede recibir campos ausentes a pesar de las propiedades no-null de Kotlin.
        if (!card.numeroTarjeta.orEmpty().matches(Regex("[0-9]{8,16}")) ||
            card.alias.isNullOrBlank() || card.tipo.isNullOrBlank() || card.estado !in listOf("ACTIVA", "INACTIVA"))
            throw CardsException(502, "El servidor devolvió información de tarjeta incompleta.")
        return card
    }

    suspend fun list(session: UserSession): List<UserCard> {
        val result = mutableListOf<UserCard>()
        val seen = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = data(api.list(authorization(session), true, cursor))
            val items = page.items ?: throw CardsException(502, "No se pudo interpretar la lista de tarjetas.")
            result += items.map(::verified)
            cursor = page.nextKey?.takeIf { it.isNotBlank() }
            if (cursor != null && (!seen.add(cursor) || seen.size > 100))
                throw CardsException(502, "No se pudo completar la consulta de tarjetas. Inténtalo nuevamente.")
        } while (cursor != null)
        return result.distinctBy { it.numeroTarjeta }
    }

    suspend fun add(session: UserSession, number: String, alias: String, type: String): UserCard {
        if (!number.matches(Regex("[0-9]{8,16}")) || alias.trim().length !in 1..30 ||
            type !in listOf("General", "Estudiante", "Universitario"))
            throw CardsException(400, "Revisa el número, alias y tipo de tarjeta.")
        val card = verified(data(api.add(authorization(session), UserCardRequest(number, alias.trim(), type))))
        if (card.numeroTarjeta != number || !card.isActive)
            throw CardsException(502, "El servidor no confirmó la vinculación de esta tarjeta.")
        return card
    }

    suspend fun changeState(session: UserSession, number: String, active: Boolean): UserCard {
        val state = if (active) "ACTIVA" else "INACTIVA"
        val card = verified(data(api.changeState(authorization(session), number, UserCardStateRequest(state))))
        if (card.numeroTarjeta != number || card.estado != state)
            throw CardsException(502, "El servidor no confirmó el estado de esta tarjeta.")
        return card
    }
}
