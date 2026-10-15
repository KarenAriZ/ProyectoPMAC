package com.karenarias.proyectopmac.data.ocr

/**
 * HU-003: Extrae el número de la tarjeta del Metropolitano a partir del texto
 * reconocido por ML Kit (OCR).
 *
 * El número impreso en la tarjeta es una secuencia de dígitos (en la app se usan 10,
 * ej. "8754785478"), que puede venir separada por espacios o guiones. El OCR a veces
 * confunde letras con números (O→0, I/l→1, S→5, B→8), por eso se corrigen solo en
 * líneas que son mayormente numéricas.
 */
object CardNumberExtractor {

    const val MIN_DIGITS = 8
    const val MAX_DIGITS = 16
    const val PREFERRED_DIGITS = 10

    // Grupo de dígitos que puede tener espacios o guiones entre medio.
    private val candidateRegex = Regex("""\d[\d \-]*\d""")

    private val ocrFixes = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'I' to '1', 'l' to '1', '|' to '1',
        'S' to '5', 's' to '5',
        'B' to '8', 'Z' to '2'
    )

    /** Devuelve el número más probable de la tarjeta o null si no encontró ninguno. */
    fun extract(rawText: String): String? = candidates(rawText).firstOrNull()

    /** Lista de candidatos ordenados del más probable al menos probable. */
    fun candidates(rawText: String): List<String> {
        return rawText.lines()
            .map { fixLine(it) }
            .flatMap { line -> candidateRegex.findAll(line).map { it.value } }
            .map { it.filter(Char::isDigit) }
            .filter { it.length in MIN_DIGITS..MAX_DIGITS }
            .distinct()
            .sortedWith(
                compareBy<String> { if (it.length == PREFERRED_DIGITS) 0 else 1 }
                    .thenByDescending { it.length }
            )
    }

    /** Valida que el número ingresado/corregido por el usuario tenga un formato aceptable. */
    fun isValid(number: String): Boolean =
        number.all(Char::isDigit) && number.length in MIN_DIGITS..MAX_DIGITS

    /** Corrige confusiones típicas del OCR solo si la línea es mayormente numérica. */
    private fun fixLine(line: String): String {
        val compact = line.filterNot { it == ' ' || it == '-' }
        if (compact.isEmpty()) return line
        val digits = compact.count(Char::isDigit)
        val fixable = compact.count { it in ocrFixes }
        return if (digits >= 6 && digits + fixable == compact.length) {
            line.map { ocrFixes[it] ?: it }.joinToString("")
        } else {
            line
        }
    }
}
