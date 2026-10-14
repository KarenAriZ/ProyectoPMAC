package com.karenarias.proyectopmac.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.remote.RecargaRepository
import kotlinx.coroutines.launch

/**
 * ViewModel que administra el estado y los datos de la pantalla principal (HomeScreen)
 * para MetroGo (saldo, información de tarjeta y listado de recargas).
 */
class HomeViewModel : ViewModel() {
    private val recargaRepository = RecargaRepository()

    var userName by mutableStateOf("Juan")
        private set

    var cardNumber by mutableStateOf("8754785478")
        private set

    var cardType by mutableStateOf("Estudiante")
        private set

    var cardBalance by mutableStateOf("S/. 17.30")
        private set

    var selectedTab by mutableStateOf(0)
        private set

    var recharges by mutableStateOf<List<RechargeUiModel>>(emptyList())
        private set

    fun cargarRecargas() {
        viewModelScope.launch {
            try {
                val recargasAws = recargaRepository.obtenerRecargas()

                recharges = recargasAws
                    .filter { it.numeroTarjeta == cardNumber }
                    .sortedByDescending { it.id }
                    .map { recarga ->
                        RechargeUiModel(
                            id = recarga.id,
                            amount = "+ S/. ${recarga.monto}",
                            date = recarga.fecha,
                            status = "Recarga simulada (${recarga.metodoPago})",
                            isSuccess = true
                        )
                    }

            } catch (e: Exception) {
                android.util.Log.e(
                    "METROGO_AWS",
                    "Error al consultar el historial",
                    e
                )
            }
        }
    }


    fun addRecharge(amount: String, paymentMethod: String) {
        val currentDate = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val newRecharge = RechargeUiModel(
            id = System.currentTimeMillis().toString(),
            amount = "+ S/. $amount",
            date = currentDate,
            status = "Recarga simulada ($paymentMethod)",
            isSuccess = true
        )

        recharges = listOf(newRecharge) + recharges
    }


    fun onTabSelected(tabIndex: Int) {
        selectedTab = tabIndex
    }

    fun refreshData() {
        // Simulación de actualización de saldo y recargas
        cardBalance = "S/. 37.30"
    }
}
