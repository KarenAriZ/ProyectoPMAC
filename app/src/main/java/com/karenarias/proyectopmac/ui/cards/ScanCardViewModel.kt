package com.karenarias.proyectopmac.ui.cards

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.karenarias.proyectopmac.data.ocr.CardNumberExtractor
import com.karenarias.proyectopmac.data.ocr.CardTextRecognizer
import kotlinx.coroutines.launch

/** Tarjeta digitalizada por el usuario. */
data class DigitalCard(
    val number: String,
    val alias: String,
    val type: String,
    val photoUri: Uri?
)

/**
 * HU-003: Estado de la pantalla "Agregar tarjeta con cámara".
 * Flujo: foto → OCR (ML Kit) → número detectado → el usuario confirma/corrige → guardar.
 */
class ScanCardViewModel : ViewModel() {

    private val textRecognizer = CardTextRecognizer()

    val cardTypes = listOf("General", "Estudiante", "Universitario")

    var photoUri by mutableStateOf<Uri?>(null)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var ocrMessage by mutableStateOf<String?>(null)
        private set
    var otherCandidates by mutableStateOf<List<String>>(emptyList())
        private set
    var cardNumber by mutableStateOf("")
        private set
    var alias by mutableStateOf("")
        private set
    var cardType by mutableStateOf("General")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var savedCard by mutableStateOf<DigitalCard?>(null)
        private set

    fun onPhotoCaptured(context: Context, uri: Uri) {
        photoUri = uri
        errorMessage = null
        ocrMessage = null
        isProcessing = true
        viewModelScope.launch {
            try {
                val text = textRecognizer.readText(context, uri)
                val candidates = CardNumberExtractor.candidates(text)
                if (candidates.isNotEmpty()) {
                    cardNumber = candidates.first()
                    otherCandidates = candidates.drop(1).take(3)
                    ocrMessage = "Número detectado automáticamente. Verifica que sea correcto."
                } else {
                    otherCandidates = emptyList()
                    ocrMessage = "No se pudo leer el número. Toma otra foto con más luz o escríbelo manualmente."
                }
            } catch (e: Exception) {
                ocrMessage = "Error al procesar la imagen: ${e.localizedMessage ?: "desconocido"}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun onCandidateSelected(number: String) {
        val previous = cardNumber
        cardNumber = number
        otherCandidates = (otherCandidates - number + previous).filter { it.isNotBlank() }
    }

    fun onCardNumberChange(value: String) {
        cardNumber = value.filter(Char::isDigit).take(CardNumberExtractor.MAX_DIGITS)
        errorMessage = null
    }

    fun onAliasChange(value: String) {
        alias = value.take(30)
    }

    fun onCardTypeChange(value: String) {
        cardType = value
    }

    fun saveCard(): DigitalCard? {
        if (photoUri == null) {
            errorMessage = "Primero toma una foto de tu tarjeta."
            return null
        }
        if (!CardNumberExtractor.isValid(cardNumber)) {
            errorMessage = "El número debe tener entre ${CardNumberExtractor.MIN_DIGITS} y ${CardNumberExtractor.MAX_DIGITS} dígitos."
            return null
        }
        val card = DigitalCard(
            number = cardNumber,
            alias = alias.ifBlank { "Mi tarjeta" },
            type = cardType,
            photoUri = photoUri
        )
        savedCard = card
        return card
    }

    fun reset() {
        photoUri = null
        isProcessing = false
        ocrMessage = null
        otherCandidates = emptyList()
        cardNumber = ""
        alias = ""
        cardType = "General"
        errorMessage = null
        savedCard = null
    }

    override fun onCleared() {
        textRecognizer.close()
        super.onCleared()
    }
}
