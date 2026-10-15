package com.karenarias.proyectopmac

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class OfflineAuthHelper(
    private val activity: FragmentActivity
) {
    private val authenticators =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    fun autenticar(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val manager = BiometricManager.from(activity)

        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Configura un bloqueo de pantalla compatible para consultar tus tarjetas.")
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    onError(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Consulta offline de MetroGo")
            .setSubtitle("Confirma tu identidad para consultar tus tarjetas")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(info)
    }
}

