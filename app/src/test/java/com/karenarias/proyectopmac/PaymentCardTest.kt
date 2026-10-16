package com.karenarias.proyectopmac

import com.google.gson.Gson
import com.karenarias.proyectopmac.ui.recharge.PaymentCardSummary
import com.karenarias.proyectopmac.ui.recharge.validatePaymentCard
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class PaymentCardTest {
    private val today = YearMonth.of(2026, 10)

    @Test fun acepta_tarjeta_de_prueba_y_mes_actual() {
        assertNull(validatePaymentCard("4111111111111111", "1026", "Ana Prueba", "123", today))
    }

    @Test fun rechaza_numero_con_digito_incorrecto() {
        assertNotNull(validatePaymentCard("4111111111111112", "1230", "Ana Prueba", "123", today))
    }

    @Test fun rechaza_tarjeta_caducada_o_mes_inexistente() {
        assertNotNull(validatePaymentCard("4111111111111111", "0926", "Ana Prueba", "123", today))
        assertNotNull(validatePaymentCard("4111111111111111", "1330", "Ana Prueba", "123", today))
    }

    @Test fun exige_titular_y_cvv() {
        assertNotNull(validatePaymentCard("4111111111111111", "1230", "", "123", today))
        assertNotNull(validatePaymentCard("4111111111111111", "1230", "Ana Prueba", "12", today))
    }

    @Test fun resumen_no_contiene_numero_completo_ni_cvv() {
        val summary = PaymentCardSummary("1111")
        assertEquals("Tarjeta •••• 1111", summary.label)
        assertEquals("{\"lastFour\":\"1111\"}", Gson().toJson(summary))
    }

    @Test(expected = IllegalArgumentException::class)
    fun resumen_no_acepta_un_numero_completo() {
        PaymentCardSummary("4111111111111111")
    }
}
