package com.karenarias.proyectopmac.ui.claims

data class RechargeRecord(
    val id: String, val amount: String, val date: String, val time: String,
    val status: String, val card: ClaimCard
)

object RechargeHistorySamples {
    val records = listOf(
        RechargeRecord("R-1001", "S/ 20.00", "14/10/2026", "14:30", "Recarga exitosa", ClaimSamples.cards[0]),
        RechargeRecord("R-1002", "S/ 10.00", "13/10/2026", "09:15", "Recarga exitosa", ClaimSamples.cards[0]),
        RechargeRecord("R-1003", "S/ 15.00", "12/10/2026", "18:45", "Error en recarga", ClaimSamples.cards[0]),
        RechargeRecord("R-1004", "S/ 20.00", "10/10/2026", "10:20", "Operación cancelada", ClaimSamples.cards[1]),
        RechargeRecord("R-1005", "S/ 10.00", "08/10/2026", "08:10", "Recarga exitosa", ClaimSamples.cards[0]),
        RechargeRecord("R-1006", "S/ 30.00", "06/10/2026", "12:40", "Recarga exitosa", ClaimSamples.cards[1]),
        RechargeRecord("R-1007", "S/ 25.00", "04/10/2026", "16:00", "Recarga exitosa", ClaimSamples.cards[0]),
        RechargeRecord("R-1008", "S/ 8.00", "02/10/2026", "07:35", "Recarga exitosa", ClaimSamples.cards[0]),
        RechargeRecord("R-1009", "S/ 12.00", "30/09/2026", "11:20", "Error en recarga", ClaimSamples.cards[1]),
        RechargeRecord("R-1010", "S/ 15.00", "28/09/2026", "17:45", "Recarga exitosa", ClaimSamples.cards[0])
    )
    fun find(id: String?) = records.find { it.id == id }
}

val claimReasons = listOf("Recarga no aplicada", "Cobro duplicado", "Saldo incorrecto", "Otro motivo")

fun validateClaim(reason: String, description: String): String? = when {
    reason !in claimReasons -> "Selecciona el motivo del reclamo."
    description.trim().length < 20 -> "Describe lo ocurrido con al menos 20 caracteres."
    description.trim().length > 1000 -> "La descripción debe tener como máximo 1000 caracteres."
    else -> null
}
