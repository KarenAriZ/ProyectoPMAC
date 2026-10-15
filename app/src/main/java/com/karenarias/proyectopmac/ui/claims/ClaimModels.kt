package com.karenarias.proyectopmac.ui.claims

enum class ClaimStatus(val label: String) {
    RECEIVED("Recibido"), REVIEW("En revisión"), RESOLVED("Resuelto")
}

enum class ClaimFilter(val label: String) {
    ALL("Todos"), PENDING("Pendientes"), RESOLVED("Resueltos")
}

data class ClaimCard(val id: String, val lastDigits: String, val type: String) {
    val label: String get() = "•••• $lastDigits · $type"
}

data class CardClaim(
    val id: String,
    val title: String,
    val card: ClaimCard,
    val status: ClaimStatus,
    val date: String,
    val receivedAt: String,
    val amount: String,
    val description: String,
    val reviewedAt: String? = null,
    val resolvedAt: String? = null,
    val resolution: String? = null,
    val rechargeId: String? = null
)

/** Datos locales de demostración; no realiza solicitudes ni registra reclamos reales. */
object ClaimSamples {
    val cards = listOf(
        ClaimCard("student", "5478", "Estudiante"),
        ClaimCard("general", "9032", "General")
    )
    val claims = listOf(
        CardClaim("REC-024", "Recarga no aplicada", cards[0], ClaimStatus.REVIEW,
            "14 oct. 2026", "14 oct. 2026 · 09:40", "S/ 20.00",
            "Realicé una recarga, pero el saldo no se actualizó en mi tarjeta.",
            reviewedAt = "14 oct. 2026 · 11:15"),
        CardClaim("REC-023", "Cobro duplicado", cards[1], ClaimStatus.RECEIVED,
            "12 oct. 2026", "12 oct. 2026 · 16:20", "S/ 3.20",
            "Se registraron dos cobros por el mismo viaje en mi tarjeta."),
        CardClaim("REC-019", "Saldo incorrecto", cards[0], ClaimStatus.RESOLVED,
            "08 oct. 2026", "08 oct. 2026 · 10:05", "S/ 5.00",
            "El saldo mostrado no corresponde al monto disponible después de mi último viaje.",
            reviewedAt = "08 oct. 2026 · 14:30", resolvedAt = "09 oct. 2026 · 09:10",
            resolution = "Se verificaron los movimientos y se corrigió el saldo de tu tarjeta.")
    )
}

fun filterClaims(claims: List<CardClaim>, cardId: String, filter: ClaimFilter): List<CardClaim> =
    claims.filter { claim ->
        (cardId == "all" || claim.card.id == cardId) && when (filter) {
            ClaimFilter.ALL -> true
            ClaimFilter.PENDING -> claim.status != ClaimStatus.RESOLVED
            ClaimFilter.RESOLVED -> claim.status == ClaimStatus.RESOLVED
        }
    }
