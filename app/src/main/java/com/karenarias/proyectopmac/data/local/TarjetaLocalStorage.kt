package com.karenarias.proyectopmac.data.local

import android.content.Context

class TarjetaLocalStorage(context: Context) {

    private val preferences = context.getSharedPreferences(
        "metrogo_tarjetas",
        Context.MODE_PRIVATE
    )

    fun guardarTarjeta(
        numero: String,
        tipo: String,
        saldo: String
    ) {
        preferences.edit()
            .putString("numeroTarjeta", numero)
            .putString("tipoTarjeta", tipo)
            .putString("saldoTarjeta", saldo)
            .apply()
    }

    fun obtenerNumeroTarjeta(): String? {
        return preferences.getString("numeroTarjeta", null)
    }

    fun obtenerTipoTarjeta(): String? {
        return preferences.getString("tipoTarjeta", null)
    }

    fun obtenerSaldoTarjeta(): String? {
        return preferences.getString("saldoTarjeta", null)
    }
    fun guardarTarjetas(tarjetas: List<TarjetaOffline>) {
        val json = com.google.gson.Gson().toJson(tarjetas)

        preferences.edit()
            .putString("listaTarjetas", json)
            .apply()
    }

    fun obtenerTarjetas(): List<TarjetaOffline> {
        val json = preferences.getString("listaTarjetas", null)
            ?: return emptyList()

        return try {
            val tipo = object :
                com.google.gson.reflect.TypeToken<List<TarjetaOffline>>() {}.type

            com.google.gson.Gson().fromJson<List<TarjetaOffline>>(
                json,
                tipo
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun agregarTarjetaEscaneada(
        numero: String,
        tipo: String
    ): List<TarjetaOffline> {
        val tarjetasActuales = obtenerTarjetas()

        // Evitar duplicados y conservar el saldo previamente sincronizado.
        val tarjetaExistente = tarjetasActuales.find {
            it.numero == numero
        }

        val tarjetasActualizadas = if (tarjetaExistente != null) {
            tarjetasActuales.map { tarjeta ->
                if (tarjeta.numero == numero) {
                    tarjeta.copy(tipo = tipo)
                } else {
                    tarjeta
                }
            }
        } else {
            tarjetasActuales + TarjetaOffline(
                numero = numero,
                tipo = tipo,
                saldo = null,
                ultimaSincronizacion = null
            )
        }

        guardarTarjetas(tarjetasActualizadas)
        return tarjetasActualizadas
    }

}