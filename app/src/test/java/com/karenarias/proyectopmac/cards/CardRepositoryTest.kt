package com.karenarias.proyectopmac.cards

import com.karenarias.proyectopmac.data.cards.CardRepository
import com.karenarias.proyectopmac.data.cards.CardStatus
import com.karenarias.proyectopmac.data.cards.CardStorage
import com.karenarias.proyectopmac.data.cards.SavedCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** HU-008: eliminar tarjeta = borrado lógico (estado INACTIVA), nunca se borra el registro. */
class CardRepositoryTest {

    private class InMemoryStorage : CardStorage {
        var saved: List<SavedCard> = emptyList()
        override fun load() = saved
        override fun save(cards: List<SavedCard>) { saved = cards }
    }

    @Test
    fun `eliminar no borra el registro, lo deja inactivo`() {
        val storage = InMemoryStorage()
        val repo = CardRepository(storage)
        val card = repo.add("8754785478", "Mi tarjeta", "General")

        val result = repo.deactivate(card.id, now = 1000L)

        assertNotNull(result)
        assertEquals(1, repo.cards.value.size)            // sigue existiendo
        assertEquals(CardStatus.INACTIVA, repo.cards.value.first().status)
        assertEquals(1000L, repo.cards.value.first().deactivatedAt)
        assertEquals(1, storage.saved.size)               // también en el almacenamiento
        assertTrue(repo.activeCards().isEmpty())          // ya no aparece como activa
    }

    @Test
    fun `no se puede dar de baja dos veces`() {
        val repo = CardRepository(InMemoryStorage())
        val card = repo.add("1234567890", "Trabajo", "General")
        repo.deactivate(card.id)
        assertNull(repo.deactivate(card.id))
    }

    @Test
    fun `reactivar vuelve la tarjeta a ACTIVA`() {
        val repo = CardRepository(InMemoryStorage())
        val card = repo.add("1234567890", "Trabajo", "General")
        repo.deactivate(card.id)
        repo.reactivate(card.id)
        assertEquals(CardStatus.ACTIVA, repo.cards.value.first().status)
        assertNull(repo.cards.value.first().deactivatedAt)
    }

    @Test
    fun `volver a agregar una tarjeta inactiva la reactiva sin duplicarla`() {
        val repo = CardRepository(InMemoryStorage())
        val card = repo.add("1234567890", "Trabajo", "General")
        repo.deactivate(card.id)
        repo.add("1234567890", "Trabajo nuevo", "Estudiante")
        assertEquals(1, repo.cards.value.size)
        assertTrue(repo.cards.value.first().isActive)
    }
}
