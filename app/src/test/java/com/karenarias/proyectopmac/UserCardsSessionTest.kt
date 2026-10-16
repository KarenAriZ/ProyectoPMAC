package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import com.karenarias.proyectopmac.ui.home.HomeViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class UserCardsSessionTest {
    private fun session(id: String) = UserSession(UsuarioRegistrado(id = id), "token-$id", 5000L)

    @Test fun cambiarDeCuentaNoConservaTarjetasNiMovimientosDeLaAnterior() {
        val api = FakeUserCardsApi()
        api.listResponse = { header, _ -> Response.success(UserCardsResponse(true, data =
            UserCardsPage(if (header == "Bearer token-A") listOf(api.card) else emptyList()))) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = HomeViewModel(UserCardsRepository(api, { 1000L }), scope)
            vm.bindSession(session("A"))
            assertEquals(api.card.numeroTarjeta, vm.cardNumber)
            vm.bindSession(session("B"))
            assertEquals("B", vm.sessionUserId)
            assertTrue(vm.cards.isEmpty())
            assertEquals("", vm.cardNumber)
            assertTrue(vm.recharges.isEmpty())
            vm.bindSession(null)
            assertNull(vm.sessionUserId)
            assertNull(vm.selectedCard)
        } finally { scope.cancel() }
    }

    @Test fun respuestaTardiaDeCuentaAnteriorNoSeMuestraEnLaNueva() {
        val api = FakeUserCardsApi()
        var pending: Continuation<Response<UserCardsResponse<UserCardsPage>>>? = null
        api.listResponse = { header, _ ->
            if (header == "Bearer token-A") suspendCoroutine { pending = it }
            else Response.success(UserCardsResponse(true, data = UserCardsPage(emptyList())))
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = HomeViewModel(UserCardsRepository(api, { 1000L }), scope)
            vm.bindSession(session("A"))
            assertTrue(vm.isLoading)
            vm.bindSession(session("B"))
            pending!!.resumeWith(Result.success(Response.success(UserCardsResponse(true, data = UserCardsPage(listOf(api.card))))))
            assertEquals("B", vm.sessionUserId)
            assertTrue(vm.cards.isEmpty())
            assertFalse(vm.isLoading)
        } finally { scope.cancel() }
    }

    @Test fun actualizacionAntiguaNoReemplazaLaRespuestaMasReciente() {
        val api = FakeUserCardsApi()
        var pending: Continuation<Response<UserCardsResponse<UserCardsPage>>>? = null
        var requests = 0
        api.listResponse = { _, _ ->
            if (++requests == 1) suspendCoroutine { pending = it }
            else Response.success(UserCardsResponse(true, data = UserCardsPage(emptyList())))
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = HomeViewModel(UserCardsRepository(api, { 1000L }), scope)
            vm.bindSession(session("A"))
            vm.refreshData()
            pending!!.resumeWith(Result.success(Response.success(UserCardsResponse(true, data = UserCardsPage(listOf(api.card))))))
            assertTrue(vm.cards.isEmpty())
            assertFalse(vm.isLoading)
        } finally { scope.cancel() }
    }

    @Test fun desactivarTarjetaSeleccionadaNoDejaSaldoDeUnaTarjetaInactiva() {
        val api = FakeUserCardsApi()
        api.listResponse = { _, _ -> Response.success(UserCardsResponse(true, data = UserCardsPage(listOf(api.card)))) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = HomeViewModel(UserCardsRepository(api, { 1000L }), scope)
            vm.bindSession(session("A"))
            var confirmed = false
            vm.changeCardState(api.card.numeroTarjeta, false) { confirmed = true }
            assertTrue(confirmed)
            assertNull(vm.selectedCard)
            assertEquals("", vm.cardNumber)
            assertFalse(vm.cards.single().isActive)
        } finally { scope.cancel() }
    }
}
