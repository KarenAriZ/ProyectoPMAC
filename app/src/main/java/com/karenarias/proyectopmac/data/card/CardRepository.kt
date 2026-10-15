package com.karenarias.proyectopmac.data.card

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Resultado de una operación: el estado actual y si vino de AWS o de los datos guardados. */
data class CardSyncResult(
    val status: CardStatus,
    val fromAws: Boolean,
    val message: String? = null
)

/**
 * HU-004: fuente de datos del saldo y estado de la tarjeta.
 *
 * Los datos oficiales están en AWS (Lambda "metrogo-tarjetas" + DynamoDB "Tarjetas").
 * El celular guarda una copia (SharedPreferences) para seguir funcionando si el laboratorio
 * de AWS está apagado o no hay internet: en ese caso se muestran los últimos datos guardados.
 */
class CardRepository(
    context: Context,
    private val api: TarjetasApiService? = TarjetasApiClient.api
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isAwsConfigured: Boolean get() = api != null

    // ------------------------------------------------------------------
    // Lectura local (inmediata, para dibujar la pantalla sin esperar a la red)
    // ------------------------------------------------------------------
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

    // ------------------------------------------------------------------
    // Operaciones con AWS (si falla la conexión, se usa el respaldo local)
    // ------------------------------------------------------------------

    /** Consulta el saldo y estado en AWS (GET /tarjetas/{numero}). */
    suspend fun refresh(): CardSyncResult {
        val local = getStatus()
        return callAws(fallback = { markUpdatedLocally() }) {
            it.consultarTarjeta(local.number, local.type)
        }
    }

    /** Guarda el umbral de saldo bajo (PUT /tarjetas/{numero}/alertas). */
    suspend fun setLowBalanceThreshold(cents: Long): CardSyncResult {
        prefs.edit().putLong(KEY_THRESHOLD, cents.coerceAtLeast(0L)).apply()
        return pushAlertSettings()
    }

    /** Activa o desactiva la alerta de saldo bajo (PUT /tarjetas/{numero}/alertas). */
    suspend fun setLowBalanceAlertsEnabled(enabled: Boolean): CardSyncResult {
        prefs.edit().putBoolean(KEY_ALERTS_ENABLED, enabled).apply()
        return pushAlertSettings()
    }

    /** HU-005 → HU-004: suma una recarga confirmada al saldo (POST movimientos RECARGA). */
    suspend fun applyRecharge(cents: Long): CardSyncResult {
        val local = getStatus()
        val monto = String.format(Locale.US, "%.2f", cents.coerceAtLeast(0L) / 100.0)
        return callAws(fallback = { applyRechargeLocally(cents) }) {
            it.registrarMovimiento(local.number, MovimientoRequest("RECARGA", monto))
        }
    }

    /** Modo demo: registra un viaje; el Lambda descuenta la tarifa (POST movimientos VIAJE). */
    suspend fun registerDemoTrip(): CardSyncResult {
        val local = getStatus()
        return callAws(fallback = { registerTripLocally() }) {
            it.registrarMovimiento(local.number, MovimientoRequest("VIAJE"))
        }
    }

    /** Modo demo: vuelve al saldo inicial. En AWS se hace con una recarga por la diferencia. */
    suspend fun resetDemoBalance(): CardSyncResult {
        prefs.edit().putBoolean(KEY_LOW_NOTIFIED, false).apply()
        val local = getStatus()
        val missing = DEFAULT_BALANCE_CENTS - local.balanceCents
        if (api == null) return CardSyncResult(resetLocally(), fromAws = false, message = LOCAL_MODE_MESSAGE)
        if (missing <= 0) return refresh()
        return applyRecharge(missing)
    }

    /** HU-003: guarda la tarjeta digitalizada y la registra/consulta en AWS. */
    suspend fun updateCard(number: String, type: String): CardSyncResult {
        prefs.edit()
            .putString(KEY_NUMBER, number)
            .putString(KEY_TYPE, type)
            .apply()
        return refresh()
    }

    // ------------------------------------------------------------------
    // Alerta de saldo bajo
    // ------------------------------------------------------------------

    /**
     * Revisa si corresponde avisar por saldo bajo y guarda que ya se avisó.
     * @return true si se debe mostrar la notificación ahora.
     */
    fun consumeLowBalanceAlert(status: CardStatus = getStatus()): Boolean {
        val (notify, notified) = lowBalanceAlertDecision(status, prefs.getBoolean(KEY_LOW_NOTIFIED, false))
        prefs.edit().putBoolean(KEY_LOW_NOTIFIED, notified).apply()
        return notify
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private suspend fun pushAlertSettings(): CardSyncResult {
        val local = getStatus()
        return callAws(fallback = { local }) {
            it.configurarAlertas(
                local.number,
                AlertasRequest(local.lowBalanceThresholdCents, local.lowBalanceAlertsEnabled)
            )
        }
    }

    /**
     * Llama a AWS; si responde bien, guarda la copia local con los datos del servidor.
     * Si AWS no está configurado o no responde, aplica [fallback] sobre los datos guardados.
     */
    private suspend fun callAws(
        fallback: () -> CardStatus,
        request: suspend (TarjetasApiService) -> retrofit2.Response<ApiRespuesta<TarjetaDto>>
    ): CardSyncResult {
        val service = api ?: return CardSyncResult(fallback(), fromAws = false, message = LOCAL_MODE_MESSAGE)
        return try {
            val response = withContext(Dispatchers.IO) { request(service) }
            val body = response.body()
            val dto = body?.data
            when {
                response.isSuccessful && body?.success == true && dto != null ->
                    CardSyncResult(saveFromServer(dto), fromAws = true, message = null)
                response.code() == 409 ->
                    // Error de negocio (por ejemplo, saldo insuficiente): no se cambia nada.
                    CardSyncResult(getStatus(), fromAws = true, message = errorMessage(response) ?: "Operación no permitida.")
                else ->
                    CardSyncResult(fallback(), fromAws = false, message = OFFLINE_MESSAGE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CardSyncResult(fallback(), fromAws = false, message = OFFLINE_MESSAGE)
        }
    }

    private fun errorMessage(response: retrofit2.Response<*>): String? = runCatching {
        val text = response.errorBody()?.use { it.string() } ?: return null
        com.google.gson.Gson().fromJson(text, ApiRespuesta::class.java)?.message
    }.getOrNull()?.takeIf { it.isNotBlank() && it.length <= 200 }

    private fun saveFromServer(dto: TarjetaDto): CardStatus {
        val editor = prefs.edit()
        dto.numeroTarjeta?.takeIf { it.isNotBlank() }?.let { editor.putString(KEY_NUMBER, it) }
        dto.tipo?.takeIf { it.isNotBlank() }?.let { editor.putString(KEY_TYPE, it) }
        dto.saldoCentimos?.let { editor.putLong(KEY_BALANCE, it) }
        dto.estado?.let { estado ->
            val state = runCatching { CardState.valueOf(estado.uppercase(Locale.ROOT)) }.getOrDefault(CardState.ACTIVA)
            editor.putString(KEY_STATE, state.name)
        }
        dto.umbralSaldoBajoCentimos?.let { editor.putLong(KEY_THRESHOLD, it) }
        dto.alertasActivas?.let { editor.putBoolean(KEY_ALERTS_ENABLED, it) }
        editor.putLong(KEY_UPDATED, System.currentTimeMillis())
        editor.apply()
        return getStatus()
    }

    private fun markUpdatedLocally(): CardStatus {
        prefs.edit().putLong(KEY_UPDATED, System.currentTimeMillis()).apply()
        return getStatus()
    }

    private fun applyRechargeLocally(cents: Long): CardStatus {
        val current = getStatus()
        prefs.edit()
            .putLong(KEY_BALANCE, current.balanceCents + cents.coerceAtLeast(0L))
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return getStatus()
    }

    private fun registerTripLocally(): CardStatus {
        val current = getStatus()
        prefs.edit()
            .putLong(KEY_BALANCE, (current.balanceCents - current.fareCents).coerceAtLeast(0L))
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return getStatus()
    }

    private fun resetLocally(): CardStatus {
        prefs.edit()
            .putLong(KEY_BALANCE, DEFAULT_BALANCE_CENTS)
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
        return getStatus()
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

        // Mismos valores de ejemplo que tenía HomeViewModel (y SALDO_INICIAL del Lambda).
        const val DEFAULT_NUMBER = "8754785478"
        const val DEFAULT_TYPE = "Estudiante"
        const val DEFAULT_BALANCE_CENTS = 1730L

        const val LOCAL_MODE_MESSAGE = "Modo local: falta configurar la URL del API de AWS."
        const val OFFLINE_MESSAGE = "Sin conexión con AWS: se muestran los últimos datos guardados."
    }
}
