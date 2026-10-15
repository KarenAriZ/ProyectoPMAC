package com.karenarias.proyectopmac.ui.card

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.card.CardRepository
import com.karenarias.proyectopmac.data.card.CardStatus
import com.karenarias.proyectopmac.notifications.MetroGoNotifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HU-004: estado de la pantalla "Mi tarjeta" (saldo, estado y alerta de saldo bajo).
 */
class CardStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)

    var status by mutableStateOf(repository.getStatus())
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var notificationsAllowed by mutableStateOf(MetroGoNotifications.canPostNotifications(application))
        private set

    init {
        refresh()
    }

    /** Consulta el saldo. El pequeño retraso imita la consulta al servidor. */
    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        viewModelScope.launch {
            try {
                delay(600)
                status = repository.refresh()
                checkLowBalanceAlert()
            } finally {
                isRefreshing = false
            }
        }
    }

    fun onThresholdSelected(cents: Long) {
        status = repository.setLowBalanceThreshold(cents)
        checkLowBalanceAlert()
    }

    fun onAlertsEnabledChanged(enabled: Boolean) {
        status = repository.setLowBalanceAlertsEnabled(enabled)
        checkLowBalanceAlert()
    }

    fun onSimulateTrip() {
        status = repository.registerDemoTrip()
        checkLowBalanceAlert()
    }

    fun onResetDemoBalance() {
        status = repository.resetDemoBalance()
        checkLowBalanceAlert()
    }

    /** Se llama al volver a la pantalla o después de pedir el permiso de notificaciones. */
    fun onNotificationPermissionChanged() {
        notificationsAllowed = MetroGoNotifications.canPostNotifications(getApplication())
        checkLowBalanceAlert()
    }

    private fun checkLowBalanceAlert() {
        val context = getApplication<Application>()
        // Sin permiso no se marca como avisado: así la alerta llega cuando el usuario lo conceda.
        if (!MetroGoNotifications.canPostNotifications(context)) return
        val current: CardStatus = status
        if (repository.consumeLowBalanceAlert(current)) {
            MetroGoNotifications.showLowBalance(context, current)
        }
    }
}
