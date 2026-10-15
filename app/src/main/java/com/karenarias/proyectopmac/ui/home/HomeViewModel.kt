
package com.karenarias.proyectopmac.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.karenarias.proyectopmac.data.local.TarjetaLocalStorage
import com.karenarias.proyectopmac.data.local.TarjetaOffline
import com.karenarias.proyectopmac.ui.auth.RechargeUiModel

/**
 * ViewModel que administra el estado y los datos de la pantalla principal
 * de MetroGo: saldo, información de tarjeta y listado de recargas.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tarjetaLocalStorage = TarjetaLocalStorage(application)

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

    // HU-007: guardar una tarjeta para su consulta offline.
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

    // HU-007: guardar la lista de tarjetas sincronizadas.
    fun guardarTarjetasSincronizadas(tarjetas: List<TarjetaOffline>) {
        tarjetaLocalStorage.guardarTarjetas(tarjetas)
        tarjetasOffline = tarjetas
    }

    // HU-003: mostrar en el inicio la tarjeta escaneada.

    fun updateCard(number: String, type: String) {
        tarjetasOffline = tarjetaLocalStorage.agregarTarjetaEscaneada(
            numero = number,
            tipo = type
        )

        cardNumber = number
        cardType = type

        // El escáner no obtiene el saldo de la tarjeta.
        // Solo mostramos uno si ya estaba guardado para esta misma tarjeta.
        val tarjeta = tarjetasOffline.find { it.numero == number }
        cardBalance = tarjeta?.saldo ?: "Saldo no disponible"
    }


    fun refreshData() {
        // Pendiente: obtener el saldo actualizado desde el servicio de tarjetas.
        // No modificar el último saldo guardado sin una sincronización real.
    }
}
