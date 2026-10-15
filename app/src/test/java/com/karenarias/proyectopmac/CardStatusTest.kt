package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.data.card.*
import org.junit.Assert.*
import org.junit.Test

/** HU-004: saldo, viajes estimados y decisión de la alerta de saldo bajo. */
class CardStatusTest {

    private fun status(
        balance: Long,
        threshold: Long = 500,
        enabled: Boolean = true,
        type: String = "Estudiante",
        state: CardState = CardState.ACTIVA
    ) = CardStatus("8754785478", type, balance, state, 0L, threshold, enabled)

    @Test fun detectaSaldoBajoSegunUmbral() {
        assertTrue(status(balance = 499).isLowBalance)
        assertFalse(status(balance = 500).isLowBalance)
        assertTrue(status(balance = 1730, threshold = 2000).isLowBalance)
    }

    @Test fun calculaViajesSegunTipoDeTarjeta() {
        assertEquals(10L, status(balance = 1730, type = "Estudiante").estimatedTrips) // 17.30 / 1.60
        assertEquals(5L, status(balance = 1730, type = "General").estimatedTrips)     // 17.30 / 3.20
        assertEquals(0L, status(balance = 100, type = "General").estimatedTrips)
    }

    @Test fun formateaSolesYEnmascaraNumero() {
        assertEquals("S/. 17.30", formatSoles(1730))
        assertEquals("S/. 0.05", formatSoles(5))
        assertEquals("•••• 5478", status(balance = 0).maskedNumber)
    }

    @Test fun avisaUnaSolaVezAlBajarDelUmbral() {
        val bajo = status(balance = 300)
        assertEquals(true to true, lowBalanceAlertDecision(bajo, alreadyNotified = false))
        assertEquals(false to true, lowBalanceAlertDecision(bajo, alreadyNotified = true))
    }

    @Test fun seRearmaCuandoElSaldoSeRecupera() {
        assertEquals(false to false, lowBalanceAlertDecision(status(balance = 2000), alreadyNotified = true))
    }

    @Test fun noAvisaSiEstaDesactivadoOLaTarjetaNoEstaActiva() {
        assertFalse(lowBalanceAlertDecision(status(balance = 300, enabled = false), false).first)
        assertFalse(lowBalanceAlertDecision(status(balance = 300, state = CardState.BLOQUEADA), false).first)
    }
}
