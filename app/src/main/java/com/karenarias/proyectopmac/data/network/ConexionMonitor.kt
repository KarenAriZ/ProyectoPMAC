
package com.karenarias.proyectopmac.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class ConexionMonitor(
    context: Context,
    private val alRecuperarConexion: () -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var estabaSinConexion = false
    private var iniciado = false

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) {
            if (!tieneInternet()) {
                estabaSinConexion = true
            }
        }

        override fun onUnavailable() {
            estabaSinConexion = true
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val internetValidado = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

            if (internetValidado && estabaSinConexion) {
                estabaSinConexion = false
                alRecuperarConexion()
            }
        }
    }

    fun iniciar() {
        if (iniciado) return

        estabaSinConexion = !tieneInternet()

        val solicitud = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(solicitud, callback)
        iniciado = true
    }

    fun detener() {
        if (!iniciado) return

        connectivityManager.unregisterNetworkCallback(callback)
        iniciado = false
    }

    private fun tieneInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }
}
