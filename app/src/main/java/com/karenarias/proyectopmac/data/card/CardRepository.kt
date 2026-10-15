package com.karenarias.proyectopmac.data.card

import android.content.Context
import android.content.SharedPreferences

/**
 * HU-004: fuente de datos del saldo y estado de la tarjeta.
 *
 * Todavía no existe un endpoint de saldo en AWS, así que los datos se guardan en el
 * teléfono (SharedPreferences) partiendo de los mismos valores que mostraba HomeViewModel.
 * Cuando el backend tenga un endpoint (por ejemplo GET /tarjetas/{numero}), solo hay que
 * cambiar [refresh] para que lo consulte; las pantallas no cambian.
 */
class CardRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStatus(): CardStatus = CardStatus(
        number = prefs.getString(KEY_NUMBER, DEFAULT_NUMBER) ?: DEFAULT_NUMBER,
        type = prefs.getString(KEY_TYPE, DEFAULT_TYPE) ?: DEFAULT_TYPE,
        balanceCents = prefs.getLong(KEY_BALANCE, DEFAULT_BALANCE_CENTS),
        state = runCatching {
            CardState.valueOf(prefs.getString(KEY_STATE, CardState.ACTIVA.name)!!)
        }.getOrDefault(CardState.ACTIVA),
        lastUpdatedMillis = prefs.getLong(KEY_UPDATED, 0L),
        lowBalanceThresholdCents = prefs.getLong(KEY_THRESHOLD, DEFAULT_LOW_BALANCE_THRESHOLD_CENTS),
        lowBalanceAlertsEnabled = prefs.getBoolean(KEY_ALERTS_ENABLED, true)
    )

    /** Consulta el saldo actual. Hoy solo marca la hora de la consulta (ver nota de la clase). */
    fun refresh(nowMillis: Long = System.currentTimeMillis()): CardStatus {
        prefs.edit().putLong(KEY_UPDATED, nowMillis).apply()
        return getStatus()
    }

    fun setLowBalanceThreshold(cents: Long): CardStatus {
        prefs.edit().putLong(KEY_THRESHOLD, cents.coerceAtLeast(0L)).apply()
        return getStatus()
    }

    fun setLowBalanceAlertsEnabled(enabled: Boolean): CardStatus {
        prefs.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        return getStatus()
    }

    /** HU-003: guarda la tarjeta digitalizada para que todas las pantallas usen la misma. */
    fun updateCard(number: String, type: String): CardStatus {
        prefs.edit()
            .putString(KEY_NUMBER, number)
            .putString(KEY_TYPE, type)
            .apply()
        return getStatus()
    }

    /** Suma saldo cuando se confirma una recarga (HU-005). */
    fun applyRecharge(cents: Long, nowMillis: Long = System.currentTimeMillis()): CardStatus {
        val current = getStatus()
        prefs.edit()
            .putLong(KEY_BALANCE, current.balanceCents + cents.coerceAtLeast(0L))
            .putLong(KEY_UPDATED, nowMillis)
            .apply()
        return getStatus()
    }

    /** Modo demo: descuenta un pasaje para poder mostrar la alerta de saldo bajo en la sustentación. */
    fun registerDemoTrip(nowMillis: Long = System.currentTimeMillis()): CardStatus {
        val current = getStatus()
        val newBalance = (current.balanceCents - current.fareCents).coerceAtLeast(0L)
        prefs.edit()
            .putLong(KEY_BALANCE, newBalance)
            .putLong(KEY_UPDATED, nowMillis)
            .apply()
        return getStatus()
    }

    /** Modo demo: vuelve al saldo inicial. */
    fun resetDemoBalance(nowMillis: Long = System.currentTimeMillis()): CardStatus {
        prefs.edit()
            .putLong(KEY_BALANCE, DEFAULT_BALANCE_CENTS)
            .putLong(KEY_UPDATED, nowMillis)
            .putBoolean(KEY_LOW_NOTIFIED, false)
            .apply()
        return getStatus()
    }

    /**
     * Revisa si corresponde avisar por saldo bajo y guarda que ya se avisó.
     * @return true si se debe mostrar la notificación ahora.
     */
    fun consumeLowBalanceAlert(status: CardStatus = getStatus()): Boolean {
        val (notify, notified) = lowBalanceAlertDecision(status, prefs.getBoolean(KEY_LOW_NOTIFIED, false))
        prefs.edit().putBoolean(KEY_LOW_NOTIFIED, notified).apply()
        return notify
    }

    private companion object {
        const val PREFS_NAME = "metrogo_card"
        const val KEY_NUMBER = "number"
        const val KEY_TYPE = "type"
        const val KEY_BALANCE = "balance_cents"
        const val KEY_STATE = "state"
        const val KEY_UPDATED = "updated_millis"
        const val KEY_THRESHOLD = "low_threshold_cents"
        const val KEY_ALERTS_ENABLED = "low_alerts_enabled"
        const val KEY_LOW_NOTIFIED = "low_notified"

        // Mismos valores de ejemplo que tenía HomeViewModel.
        const val DEFAULT_NUMBER = "8754785478"
        const val DEFAULT_TYPE = "Estudiante"
        const val DEFAULT_BALANCE_CENTS = 1730L
    }
}
