package com.karenarias.proyectopmac.ui.card

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.card.CardRepository
import com.karenarias.proyectopmac.data.card.CardStatus
import com.karenarias.proyectopmac.data.card.CardSyncResult
import com.karenarias.proyectopmac.notifications.MetroGoNotifications
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU-004: estado de la pantalla "Mi tarjeta" (saldo, estado y alerta de saldo bajo).
 * Los datos vienen del Lambda "metrogo-tarjetas" en AWS; si no responde, se usan los guardados.
 */
class CardStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application)

    var status by mutableStateOf(repository.getStatus())
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var notificationsAllowed by mutableStateOf(MetroGoNotifications.canPostNotifications(application))
        private set

    /** Texto bajo el saldo: si los datos vienen de AWS o de la copia guardada. */
    var syncMessage by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    /** Consulta el saldo en AWS. */
    fun refresh() {
        if (isRefreshing) return
        launchSync { repository.refresh() }
    }

    fun onThresholdSelected(cents: Long) = launchSync { repository.setLowBalanceThreshold(cents) }

    fun onAlertsEnabledChanged(enabled: Boolean) = launchSync { repository.setLowBalanceAlertsEnabled(enabled) }

    fun onSimulateTrip() = launchSync { repository.registerDemoTrip() }

    fun onResetDemoBalance() = launchSync { repository.resetDemoBalance() }

    /** Se llama al volver a la pantalla o después de pedir el permiso de notificaciones. */
    fun onNotificationPermissionChanged() {
        notificationsAllowed = MetroGoNotifications.canPostNotifications(getApplication())
        checkLowBalanceAlert()
    }

    private fun launchSync(operation: suspend () -> CardSyncResult) {
        isRefreshing = true
        viewModelScope.launch {
            try {
                val result = operation()
                status = result.status
                syncMessage = result.message ?: if (result.fromAws) {
                    "Sincronizado con AWS · " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                } else null
                checkLowBalanceAlert()
            } finally {
                isRefreshing = false
            }
        }
        // viewModelScope arranca de inmediato hasta la llamada a AWS, así que aquí los datos
        // guardados ya tienen el cambio (por ejemplo, el umbral recién elegido) y se muestran
        // mientras AWS responde.
        status = repository.getStatus()
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
