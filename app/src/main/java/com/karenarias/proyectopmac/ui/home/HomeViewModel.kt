package com.karenarias.proyectopmac.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel

/**
 * ViewModel que administra el estado y los datos de la pantalla principal (HomeScreen)
 * para MetroGo (saldo, información de tarjeta y listado de recargas).
 */
class HomeViewModel : ViewModel() {

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

    var recharges by mutableStateOf(
        listOf(
            RechargeUiModel("1", "+ S/. 20.00", "Hoy, 14:30", "Recarga exitosa", true),
            RechargeUiModel("2", "+ S/. 10.00", "Ayer, 09:15", "Recarga exitosa", true),
            RechargeUiModel("3", "+ S/. 15.00", "12 Oct, 18:45", "Error en recarga", false)
        )
    )
        private set

    fun onTabSelected(tabIndex: Int) {
        selectedTab = tabIndex
    }

    fun refreshData() {
        // Simulación de actualización de saldo y recargas
        cardBalance = "S/. 37.30"
    }
}
