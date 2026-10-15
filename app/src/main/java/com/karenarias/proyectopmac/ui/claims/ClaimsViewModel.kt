package com.karenarias.proyectopmac.ui.claims

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class ClaimsViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val selectedCard: StateFlow<String> = savedState.getStateFlow("claimCard", "all")
    val selectedFilter: StateFlow<String> = savedState.getStateFlow("claimFilter", ClaimFilter.ALL.name)
    val cards = ClaimSamples.cards
    var claims by mutableStateOf(ClaimSamples.claims)
        private set
    private var activeUser: String? = null

    fun bindUser(userId: String?) {
        if (activeUser != userId) {
            activeUser = userId
            claims = ClaimSamples.claims
            selectCard("all")
            selectFilter(ClaimFilter.ALL)
        }
    }

    fun claimForRecharge(rechargeId: String): String? = claims.find { it.rechargeId == rechargeId }?.id

    fun registerClaim(rechargeId: String, reason: String, description: String): String? {
        if (activeUser == null || validateClaim(reason, description) != null) return null
        val record = RechargeHistorySamples.find(rechargeId) ?: return null
        claimForRecharge(rechargeId)?.let { return it }
        val now = ZonedDateTime.now(ZoneId.of("America/Lima"))
        val date = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-PE")))
        val id = "REC-" + UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)
        claims = listOf(CardClaim(
            id, reason, record.card, ClaimStatus.RECEIVED, date,
            "$date · ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}",
            record.amount, description.trim(), rechargeId = record.id
        )) + claims
        selectCard("all")
        selectFilter(ClaimFilter.ALL)
        return id
    }

    fun selectCard(id: String) { savedState["claimCard"] = id }
    fun selectFilter(filter: ClaimFilter) { savedState["claimFilter"] = filter.name }
    fun findClaim(id: String?): CardClaim? = claims.find { it.id == id }
}
