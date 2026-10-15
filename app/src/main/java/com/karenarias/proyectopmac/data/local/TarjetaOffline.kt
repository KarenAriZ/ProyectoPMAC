
package com.karenarias.proyectopmac.data.local

data class TarjetaOffline(
    val numero: String,
    val tipo: String,
    val saldo: String? = null,
    val ultimaSincronizacion: String? = null
)
