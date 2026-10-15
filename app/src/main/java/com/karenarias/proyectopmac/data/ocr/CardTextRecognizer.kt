package com.karenarias.proyectopmac.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HU-003: Envoltorio de ML Kit Text Recognition.
 * Lee una foto (Uri) y devuelve todo el texto reconocido. Funciona sin internet
 * porque usa el modelo incluido (bundled) dentro de la app.
 */
class CardTextRecognizer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun readText(context: Context, imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { error -> cont.resumeWithException(error) }
        }
    }

    fun close() = recognizer.close()
}
