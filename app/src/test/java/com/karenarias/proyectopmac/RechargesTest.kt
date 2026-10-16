package com.karenarias.proyectopmac

import androidx.lifecycle.SavedStateHandle
import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import com.karenarias.proyectopmac.ui.recharge.UserRechargesViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.UUID

class FakeRechargesApi : UserRechargesApiService {
    val requests = mutableListOf<SimulatedRechargeRequest>()
    val headers = mutableListOf<String>()
    var failCreate = false
    var wrongReceipt = false
    var page: (String?) -> Response<UserCardsResponse<UserRechargesPage>> = {
        Response.success(UserCardsResponse(true, data = UserRechargesPage(emptyList())))
    }
    override suspend fun create(authorization: String, request: SimulatedRechargeRequest): Response<UserCardsResponse<UserRecharge>> {
        headers += authorization; requests += request
        if (failCreate) throw IOException("Resultado incierto")
        return Response.success(UserCardsResponse(true, data = UserRecharge(
            if (wrongReceipt) UUID.randomUUID().toString() else request.idOperacion,
            request.numeroTarjeta, "Mi tarjeta", "General", request.montoCentimos,
            request.metodoPago, "2026-10-15T12:00:00.000Z", "SIMULADA", true, 0, request.montoCentimos)))
    }
    override suspend fun list(authorization: String, nextKey: String?): Response<UserCardsResponse<UserRechargesPage>> {
        headers += authorization
        return page(nextKey)
    }
}

class RechargesTest {
    private val session = UserSession(UsuarioRegistrado(id = "A"), "test-token", 5000)
    private fun request() = SimulatedRechargeRequest(UUID.randomUUID().toString(), "8754785478", 2050, "Yape")

    @Test fun envia_token_y_confirma_comprobante_exacto() = runBlocking {
        val api = FakeRechargesApi()
        val repo = UserRechargesRepository(api, { 1000 })
        val request = request()
        val result = repo.create(session, request)
        assertEquals("Bearer test-token", api.headers.single())
        assertEquals(request.idOperacion, result.id)
        assertEquals(2050L, result.saldoPosteriorCentimos)
    }

    @Test fun rechaza_comprobante_de_otra_operacion() = runBlocking {
        val api = FakeRechargesApi(); api.wrongReceipt = true
        try { UserRechargesRepository(api, { 1000 }).create(session, request()); fail() }
        catch (e: CardsException) { assertEquals(502, e.status) }
    }

    @Test fun sesion_vencida_no_realiza_peticion() = runBlocking {
        val api = FakeRechargesApi()
        try { UserRechargesRepository(api, { 5000 }).create(session, request()); fail() }
        catch (e: CardsException) { assertEquals(401, e.status) }
        assertTrue(api.requests.isEmpty())
    }

    @Test fun paginas_vacias_intermedias_se_consultan() = runBlocking {
        val api = FakeRechargesApi()
        val repo = UserRechargesRepository(api, { 1000 })
        val item = repo.create(session, request())
        api.page = { cursor -> Response.success(UserCardsResponse(true, data =
            if (cursor == null) UserRechargesPage(emptyList(), "next") else UserRechargesPage(listOf(item)))) }
        assertEquals(listOf(item), repo.list(session))
    }

    @Test fun cursor_repetido_no_causa_bucle() = runBlocking {
        val api = FakeRechargesApi()
        api.page = { Response.success(UserCardsResponse(true, data = UserRechargesPage(emptyList(), "same"))) }
        try { UserRechargesRepository(api, { 1000 }).list(session); fail() }
        catch (e: CardsException) { assertEquals(502, e.status) }
    }

    @Test fun reintento_incierto_conserva_uuid_monto_y_tarjeta() {
        val api = FakeRechargesApi(); api.failCreate = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val vm = UserRechargesViewModel(SavedStateHandle(), UserRechargesRepository(api, { 1000 }), scope)
            vm.bindSession(session)
            vm.submit("8754785478", 2050, "Yape") { fail() }
            val pending = vm.pending
            assertNotNull(pending); assertNull(vm.receipt); assertNotNull(vm.saveError)
            api.failCreate = false
            var confirmed = false
            vm.submit("11111111", 500, "Plin") { confirmed = true }
            assertTrue(confirmed)
            assertEquals(api.requests[0], api.requests[1])
            assertEquals(pending!!.idOperacion, vm.receipt!!.id)
            assertNull(vm.pending)
            assertEquals(1, vm.items.size)
        } finally { scope.cancel() }
    }

    @Test fun intento_se_recupera_al_recrear_y_no_pasa_a_otro_usuario() {
        val api = FakeRechargesApi(); api.failCreate = true
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val state = SavedStateHandle()
        try {
            val repo = UserRechargesRepository(api, { 1000 })
            val first = UserRechargesViewModel(state, repo, scope)
            first.bindSession(session); first.submit("8754785478", 500, "Plin") {}
            val pending = first.pending
            val recreated = UserRechargesViewModel(state, repo, scope)
            recreated.bindSession(session)
            assertEquals(pending, recreated.pending)
            recreated.bindSession(UserSession(UsuarioRegistrado(id = "B"), "token-B", 5000))
            assertNull(recreated.pending); assertNull(recreated.receipt); assertTrue(recreated.items.isEmpty())
            recreated.bindSession(session)
            assertEquals(pending, recreated.pending)
        } finally { scope.cancel() }
    }

    @Test fun fabrica_android_puede_crear_vm_con_saved_state() {
        assertNotNull(UserRechargesViewModel::class.java.getConstructor(SavedStateHandle::class.java))
    }
}
