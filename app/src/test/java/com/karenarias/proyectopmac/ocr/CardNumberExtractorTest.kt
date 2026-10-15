package com.karenarias.proyectopmac.ocr

import com.karenarias.proyectopmac.data.ocr.CardNumberExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-003: pruebas de la lógica que extrae el número de la tarjeta del texto OCR. */
class CardNumberExtractorTest {

    @Test
    fun `extrae numero de 10 digitos`() {
        val text = "METROPOLITANO\nTarjeta\n8754785478"
        assertEquals("8754785478", CardNumberExtractor.extract(text))
    }

    @Test
    fun `une digitos separados por espacios o guiones`() {
        assertEquals("8754785478", CardNumberExtractor.extract("N° 8754 785 478"))
        assertEquals("1234567890", CardNumberExtractor.extract("12-3456-7890"))
    }

    @Test
    fun `corrige confusiones tipicas del OCR`() {
        assertEquals("8754705478", CardNumberExtractor.extract("B754 7O5 478"))
    }

    @Test
    fun `prefiere 10 digitos sobre otros numeros`() {
        val text = "Lote 20240115\nRUC 20123456789\n8754785478"
        assertEquals("8754785478", CardNumberExtractor.extract(text))
    }

    @Test
    fun `devuelve null si no hay numero`() {
        assertNull(CardNumberExtractor.extract("METROPOLITANO Lima 2026"))
    }

    @Test
    fun `valida formato`() {
        assertTrue(CardNumberExtractor.isValid("8754785478"))
        assertFalse(CardNumberExtractor.isValid("1234"))
        assertFalse(CardNumberExtractor.isValid("12345abc90"))
    }
}
