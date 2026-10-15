package com.karenarias.proyectopmac

import android.content.Context

class OfflineSessionStorage(context: Context) {

    private val prefs = context.getSharedPreferences(
        "metrogo_offline_session",
        Context.MODE_PRIVATE
    )

    fun guardarUsuario(userId: String) {
        require(userId.isNotBlank())

        prefs.edit()
            .putString("user_id", userId)
            .apply()
    }

    fun obtenerUsuario(): String? {
        return prefs.getString("user_id", null)
            ?.takeIf { it.isNotBlank() }
    }

    fun borrarUsuario() {
        prefs.edit()
            .remove("user_id")
            .apply()
    }
}

