package com.karenarias.proyectopmac.ui.recharge

import java.time.YearMonth

/** Solo se conserva la terminación durante la sesión; nunca el número completo ni el CVV. */
data class PaymentCardSummary(val lastFour: String) {
    init { require(lastFour.matches(Regex("[0-9]{4}"))) { "La terminación debe contener cuatro dígitos." } }
    val label: String get() = "Tarjeta •••• $lastFour"
}

fun validatePaymentCard(number: String, expiry: String, holder: String, cvv: String,
                        today: YearMonth = YearMonth.now()): String? {
    if (!number.matches(Regex("[0-9]{13,19}"))) return "Ingresa un número de tarjeta válido."
    val checksum = number.reversed().mapIndexed { index, digit ->
        val value = digit.digitToInt() * if (index % 2 == 1) 2 else 1
        if (value > 9) value - 9 else value
    }.sum()
    if (checksum == 0 || checksum % 10 != 0) return "Revisa el número de la tarjeta."
    if (!expiry.matches(Regex("[0-9]{4}"))) return "Ingresa la fecha en formato MM/AA."
    val expiration = runCatching { YearMonth.of(2000 + expiry.takeLast(2).toInt(), expiry.take(2).toInt()) }.getOrNull()
    if (expiration == null || expiration < today) return "La fecha de caducidad no es válida."
    if (holder.trim().length !in 3..80 || !holder.any { it.isLetter() }) return "Ingresa el nombre del titular."
    if (!cvv.matches(Regex("[0-9]{3,4}"))) return "Ingresa un CVV de 3 o 4 dígitos."
    return null
}
