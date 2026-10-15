package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.ui.claims.*
import org.junit.Assert.*
import org.junit.Test

class ClaimValidationTest {
    @Test fun requiereMotivoYDescripcionUtil() {
        assertNotNull(validateClaim("", "La recarga no se refleja en mi saldo."))
        assertNotNull(validateClaim("Motivo desconocido", "La recarga no se refleja en mi saldo."))
        assertNotNull(validateClaim("Recarga no aplicada", " ".repeat(30)))
        assertNotNull(validateClaim("Recarga no aplicada", "a".repeat(19)))
        assertNull(validateClaim("Recarga no aplicada", "a".repeat(20)))
        assertNull(validateClaim("Recarga no aplicada", "a".repeat(1000)))
        assertNotNull(validateClaim("Recarga no aplicada", "a".repeat(1001)))
    }

    @Test fun filtrosCombinanTarjetaYEstado() {
        val filtered = filterClaims(ClaimSamples.claims, "student", ClaimFilter.PENDING)
        assertEquals(listOf("REC-024"), filtered.map { it.id })
        assertTrue(filterClaims(ClaimSamples.claims, "general", ClaimFilter.RESOLVED).isEmpty())
    }

    @Test fun detalleRecuperaLaRecargaSeleccionada() {
        val record = RechargeHistorySamples.find("R-1003")!!
        assertEquals("S/ 15.00", record.amount)
        assertEquals("Error en recarga", record.status)
        assertEquals("5478", record.card.lastDigits)
        assertNull(RechargeHistorySamples.find("inexistente"))
    }
}
