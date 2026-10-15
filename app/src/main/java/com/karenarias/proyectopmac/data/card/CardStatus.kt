package com.karenarias.proyectopmac.data.card

import java.util.Locale

/**
 * HU-004: modelo del saldo y estado de la tarjeta del pasajero.
 * Los montos se guardan en céntimos (Long) para evitar errores de redondeo con Double.
 */
enum class CardState(val label: String) {
    ACTIVA("Activa"),
    BLOQUEADA("Bloqueada"),
    VENCIDA("Vencida")
}

data class CardStatus(
    val number: String,
    val type: String,
    val balanceCents: Long,
    val state: CardState,
    val lastUpdatedMillis: Long,
    val lowBalanceThresholdCents: Long,
    val lowBalanceAlertsEnabled: Boolean
) {
    val isLowBalance: Boolean get() = balanceCents < lowBalanceThresholdCents

    /** Tarifa referencial según el tipo de tarjeta (medio pasaje para estudiantes). */
    val fareCents: Long get() = fareFor(type)

    /** Cantidad aproximada de viajes que alcanzan con el saldo actual. */
    val estimatedTrips: Long get() = if (fareCents <= 0) 0 else balanceCents / fareCents

    /** Número enmascarado para mostrar en pantalla y en notificaciones. */
    val maskedNumber: String get() = "•••• " + number.takeLast(4)
}

/** Umbrales de saldo bajo que el usuario puede elegir. */
val LOW_BALANCE_THRESHOLD_OPTIONS_CENTS = listOf(500L, 1000L, 1500L, 2000L)
const val DEFAULT_LOW_BALANCE_THRESHOLD_CENTS = 500L

/**
 * Tarifas referenciales. Ajústalas a las tarifas vigentes del Metropolitano
 * si el docente lo pide o si el backend llega a devolverlas.
 */
const val TARIFA_GENERAL_CENTS = 320L
const val TARIFA_ESTUDIANTE_CENTS = 160L

fun fareFor(cardType: String): Long =
    if (cardType.equals("Estudiante", ignoreCase = true)) TARIFA_ESTUDIANTE_CENTS else TARIFA_GENERAL_CENTS

/** Formatea céntimos con el mismo estilo que usa HomeScreen ("S/. 17.30"). */
fun formatSoles(cents: Long): String =
    String.format(Locale.US, "S/. %.2f", cents / 100.0)

/**
 * Decide si se debe enviar la notificación de saldo bajo.
 * Se avisa una sola vez cada vez que el saldo cruza el umbral hacia abajo,
 * para no repetir la misma notificación en cada consulta.
 *
 * @return Pair(enviarNotificacion, nuevoEstadoYaNotificado)
 */
fun lowBalanceAlertDecision(status: CardStatus, alreadyNotified: Boolean): Pair<Boolean, Boolean> =
    when {
        !status.isLowBalance -> false to false            // saldo recuperado: se rearma la alerta
        !status.lowBalanceAlertsEnabled -> false to alreadyNotified
        status.state != CardState.ACTIVA -> false to alreadyNotified
        alreadyNotified -> false to true
        else -> true to true
    }
