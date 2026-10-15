
package com.karenarias.proyectopmac.data.cards

enum class CardStatus {
    ACTIVA,
    INACTIVA
}

data class SavedCard(
    val id: String,
    val number: String,
    val alias: String,
    val type: String,
    val status: CardStatus = CardStatus.ACTIVA,
    val createdAt: Long = System.currentTimeMillis(),
    val deactivatedAt: Long? = null,

    // HU-007: último saldo guardado para consulta offline
    val saldo: String? = null,
    val ultimaSincronizacion: Long? = null,
    val saldoDemostracion: Boolean = false
) {
    val isActive: Boolean
        get() = status == CardStatus.ACTIVA

    val maskedNumber: String
        get() = if (number.length > 4) {
            "•••• " + number.takeLast(4)
        } else {
            number
        }
}
