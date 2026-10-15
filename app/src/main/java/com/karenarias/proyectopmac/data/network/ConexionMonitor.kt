package com.karenarias.proyectopmac.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class ConexionMonitor(
    context: Context,
    private val alRecuperarConexion: () -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var estabaSinConexion = false

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) {
            estabaSinConexion = !tieneInternet()
        }

        override fun onAvailable(network: Network) {
            // Esperamos a que Android confirme que hay internet.
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val internetValidado = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

            if (estabaSinConexion && internetValidado) {
                estabaSinConexion = false
                alRecuperarConexion()
            }
        }
    }

    fun iniciar() {
        estabaSinConexion = !tieneInternet()
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    fun detener() {
        connectivityManager.unregisterNetworkCallback(callback)
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