package com.karenarias.proyectopmac.data.cards

/** Estados posibles de una tarjeta registrada por el usuario. */
enum class CardStatus { ACTIVA, INACTIVA }

/**
 * Tarjeta del Metropolitano guardada en la cuenta del usuario.
 *
 * HU-008: las tarjetas NO se eliminan físicamente. Al "eliminar" solo cambia
 * [status] a INACTIVA y se registra la fecha en [deactivatedAt] (borrado lógico).
 */
data class SavedCard(
    val id: String,
    val number: String,
    val alias: String,
    val type: String,
    val status: CardStatus = CardStatus.ACTIVA,
    val createdAt: Long = System.currentTimeMillis(),
    val deactivatedAt: Long? = null
) {
    val isActive: Boolean get() = status == CardStatus.ACTIVA
    val maskedNumber: String get() = if (number.length > 4) "•••• " + number.takeLast(4) else number
}
