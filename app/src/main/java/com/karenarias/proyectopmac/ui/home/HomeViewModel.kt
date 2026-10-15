package com.karenarias.proyectopmac.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.karenarias.proyectopmac.data.card.CardRepository
import com.karenarias.proyectopmac.data.card.formatSoles
import com.karenarias.proyectopmac.notifications.MetroGoNotifications
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel

/**
 * ViewModel que administra el estado y los datos de la pantalla principal (HomeScreen)
 * para MetroGo (saldo, información de tarjeta y listado de recargas).
 * HU-004: el saldo y la tarjeta vienen de CardRepository (los mismos datos que "Mi tarjeta").
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CardRepository(application)

    var userName by mutableStateOf("Juan")
        private set

    var cardNumber by mutableStateOf("")
        private set

    var cardType by mutableStateOf("")
        private set

    var cardBalance by mutableStateOf("")
        private set

    var isLowBalance by mutableStateOf(false)
        private set

    var selectedTab by mutableStateOf(0)
        private set

    var recharges by mutableStateOf(
        listOf(
            RechargeUiModel("1", "+ S/. 20.00", "Hoy, 14:30", "Recarga exitosa", true),
            RechargeUiModel("2", "+ S/. 10.00", "Ayer, 09:15", "Recarga exitosa", true),
            RechargeUiModel("3", "+ S/. 15.00", "12 Oct, 18:45", "Error en recarga", false)
        )
    )
        private set

    init {
        refreshData()
    }

    fun onTabSelected(tabIndex: Int) {
        selectedTab = tabIndex
    }

    /**
     * HU-003: muestra en el inicio la tarjeta digitalizada con la cámara.
     * HU-004: también se guarda en CardRepository para que "Mi tarjeta" muestre la misma tarjeta.
     */
    fun updateCard(number: String, type: String) {
        cardRepository.updateCard(number, type)
        refreshData()
    }

    /**
     * HU-005: registra la recarga en la lista de Inicio.
     * HU-004: suma el monto al saldo guardado, así Home y "Mi tarjeta" muestran el saldo nuevo
     * y desaparece el aviso de saldo bajo si corresponde.
     */
    fun recordRecharge(amount: String, method: String) {
        val value = amount.toBigDecimalOrNull() ?: return
        val cents = value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        cardRepository.applyRecharge(cents)
        recharges = listOf(RechargeUiModel(
            "SIM-" + java.util.UUID.randomUUID(), "+ S/. " + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
            "Hoy · $method", "Recarga simulada", true
        )) + recharges
        refreshData()
    }

    /** Vuelve a leer el saldo (se llama cada vez que Home aparece en pantalla). */
    fun refreshData() {
        val status = cardRepository.getStatus()
        cardNumber = status.number
        cardType = status.type
        cardBalance = formatSoles(status.balanceCents)
        isLowBalance = status.isLowBalance

        val context = getApplication<Application>()
        if (MetroGoNotifications.canPostNotifications(context) &&
            cardRepository.consumeLowBalanceAlert(status)
        ) {
            MetroGoNotifications.showLowBalance(context, status)
        }
    }
}
