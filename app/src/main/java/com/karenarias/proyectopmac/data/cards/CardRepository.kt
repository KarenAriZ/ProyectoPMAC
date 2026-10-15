package com.karenarias.proyectopmac.data.cards

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Dónde se guardan las tarjetas (permite usar otro almacenamiento en las pruebas). */
interface CardStorage {
    fun load(): List<SavedCard>
    fun save(cards: List<SavedCard>)
}

/** Almacenamiento local con SharedPreferences (las tarjetas sobreviven al cerrar la app). */
class SharedPrefsCardStorage(
    context: Context,
    userId: String
) : CardStorage {
    private val prefs = context.getSharedPreferences(
        "metrogo_cards_$userId",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    override fun load(): List<SavedCard> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedCard>>() {}.type
        return runCatching { gson.fromJson<List<SavedCard>>(json, type) }.getOrNull() ?: emptyList()
    }

    override fun save(cards: List<SavedCard>) {
        prefs.edit().putString(KEY, gson.toJson(cards)).apply()
    }

    private companion object { const val KEY = "cards" }
}

/**
 * Repositorio de tarjetas del usuario (HU-003 agrega, HU-008 da de baja).
 *
 * Regla del profesor: NO se elimina el registro de la base de datos.
 * [deactivate] hace un borrado lógico: status = INACTIVA + fecha de baja.
 * Cuando exista el backend, esto equivale a un UPDATE del campo estado (no un DELETE).
 */
class CardRepository(private val storage: CardStorage) {

    private val _cards = MutableStateFlow(storage.load())

    /** Todas las tarjetas, incluidas las inactivas (nunca se borran). */
    val cards: StateFlow<List<SavedCard>> = _cards.asStateFlow()

    fun activeCards(): List<SavedCard> = _cards.value.filter { it.isActive }

    fun add(number: String, alias: String, type: String): SavedCard {
        // Si ya existía (aunque esté inactiva) se reactiva en lugar de duplicarla.
        _cards.value.firstOrNull { it.number == number }?.let { existing ->
            val updated = existing.copy(alias = alias, type = type, status = CardStatus.ACTIVA, deactivatedAt = null)
            replace(updated)
            return updated
        }
        val card = SavedCard(id = UUID.randomUUID().toString(), number = number, alias = alias, type = type)
        update(_cards.value + card)
        return card
    }

    /** HU-008: "Eliminar" tarjeta = pasarla a INACTIVA. El registro se conserva. */
    fun deactivate(cardId: String, now: Long = System.currentTimeMillis()): SavedCard? {
        val card = _cards.value.firstOrNull { it.id == cardId && it.isActive } ?: return null
        val inactive = card.copy(status = CardStatus.INACTIVA, deactivatedAt = now)
        replace(inactive)
        return inactive
    }

    /** Permite volver a activar una tarjeta dada de baja (prueba de que no se borró). */
    fun reactivate(cardId: String): SavedCard? {
        val card = _cards.value.firstOrNull { it.id == cardId && !it.isActive } ?: return null
        val active = card.copy(status = CardStatus.ACTIVA, deactivatedAt = null)
        replace(active)
        return active
    }


    /**
     * HU-007: guarda el último saldo de demostración
     * y la fecha de sincronización de una tarjeta.
     */
    fun sincronizarSaldoDemostracion(
        cardId: String,
        saldo: String
    ): SavedCard? {
        val card = _cards.value.firstOrNull {
            it.id == cardId && it.isActive
        } ?: return null

        val updated = card.copy(
            saldo = saldo,
            ultimaSincronizacion = System.currentTimeMillis(),
            saldoDemostracion = true
        )

        replace(updated)
        return updated
    }
    fun aplicarRecarga(
        numeroTarjeta: String,
        monto: String
    ): SavedCard? {
        val importe = monto.toBigDecimalOrNull() ?: return null
        if (importe <= java.math.BigDecimal.ZERO) return null

        val tarjeta = _cards.value.firstOrNull {
            it.number == numeroTarjeta && it.isActive
        } ?: return null

        val saldoAnterior = tarjeta.saldo?.toBigDecimalOrNull()
            ?: return null

        val nuevoSaldo = saldoAnterior.add(importe)
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toPlainString()

        val actualizada = tarjeta.copy(
            saldo = nuevoSaldo,
            ultimaSincronizacion = System.currentTimeMillis()
        )

        replace(actualizada)
        return actualizada
    }

    private fun replace(card: SavedCard) = update(_cards.value.map { if (it.id == card.id) card else it })

    private fun update(list: List<SavedCard>) {
        _cards.value = list
        storage.save(list)
    }

    companion object {
        private val instances = mutableMapOf<String, CardRepository>()

        fun get(context: Context, userId: String): CardRepository =
            synchronized(this) {
                require(userId.isNotBlank()) {
                    "Se necesita un usuario autenticado"
                }

                instances.getOrPut(userId) {
                    val nuevoStorage = SharedPrefsCardStorage(
                        context.applicationContext,
                        userId
                    )

                    val prefsAntiguas = context.getSharedPreferences(
                        "metrogo_cards",
                        Context.MODE_PRIVATE
                    )

                    val migracionRealizada = prefsAntiguas.getBoolean(
                        "migracion_realizada",
                        false
                    )

                    if (!migracionRealizada) {
                        val tarjetasAntiguas = prefsAntiguas.getString("cards", null)

                        if (tarjetasAntiguas != null && nuevoStorage.load().isEmpty()) {
                            val tipo = object : TypeToken<List<SavedCard>>() {}.type

                            val tarjetas = runCatching {
                                Gson().fromJson<List<SavedCard>>(
                                    tarjetasAntiguas,
                                    tipo
                                )
                            }.getOrNull()

                            if (tarjetas != null) {
                                nuevoStorage.save(tarjetas)
                            }
                        }

                        prefsAntiguas.edit()
                            .putBoolean("migracion_realizada", true)
                            .apply()
                    }

                    CardRepository(nuevoStorage)
                }
            }
    }
}
