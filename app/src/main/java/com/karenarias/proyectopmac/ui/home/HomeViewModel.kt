package com.karenarias.proyectopmac.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.remote.RecargaRepository
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.karenarias.proyectopmac.data.local.TarjetaLocalStorage
import com.karenarias.proyectopmac.data.local.TarjetaOffline

/**
 * ViewModel que administra el estado y los datos de la pantalla principal (HomeScreen)
 * para MetroGo (saldo, información de tarjeta y listado de recargas).
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tarjetaLocalStorage = TarjetaLocalStorage(application)

    private val recargaRepository = RecargaRepository()

    var userName by mutableStateOf("Juan")
        private set

    var cardNumber by mutableStateOf(
        tarjetaLocalStorage.obtenerNumeroTarjeta() ?: "8754785478"
    )
        private set

    var cardType by mutableStateOf(
        tarjetaLocalStorage.obtenerTipoTarjeta() ?: "Estudiante"
    )
        private set

    var cardBalance by mutableStateOf(
        tarjetaLocalStorage.obtenerSaldoTarjeta() ?: "S/. 17.30"
    )
        private set

    var tarjetasOffline by mutableStateOf<List<TarjetaOffline>>(
        tarjetaLocalStorage.obtenerTarjetas()
    )
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

    fun guardarTarjetaLocal(
        numero: String,
        tipo: String,
        saldo: String
    ) {
        tarjetaLocalStorage.guardarTarjeta(numero, tipo, saldo)

        cardNumber = numero
        cardType = tipo
        cardBalance = saldo
    }
    fun guardarTarjetasSincronizadas(tarjetas: List<TarjetaOffline>) {
        tarjetaLocalStorage.guardarTarjetas(tarjetas)
        tarjetasOffline = tarjetas
    }

    fun refreshData() {
        // Pendiente: obtener el saldo actualizado desde el servicio de tarjetas.
        // No modificar el último saldo guardado sin una sincronización real.
    }


}
