package com.karenarias.proyectopmac

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class FakeUserCardsApi : UserCardsApiService {
    val calls = mutableListOf<String>()
    val headers = mutableListOf<String>()
    val card = UserCard("8754785478", "Trabajo", "General", "ACTIVA", 0L, true, "ACTIVA")
    var listResponse: suspend (String, String?) -> Response<UserCardsResponse<UserCardsPage>> = { _, _ ->
        Response.success(UserCardsResponse(true, data = UserCardsPage(emptyList())))
    }
    var addResponse: Response<UserCardsResponse<UserCard>> = Response.success(201, UserCardsResponse(true, data = card))
    override suspend fun list(authorization: String, includeInactive: Boolean, nextKey: String?): Response<UserCardsResponse<UserCardsPage>> {
        calls += "list:${nextKey.orEmpty()}"
        headers += authorization
        assertTrue(includeInactive)
        return listResponse(authorization, nextKey)
    }
    override suspend fun add(authorization: String, card: UserCardRequest): Response<UserCardsResponse<UserCard>> {
        calls += "add"; headers += authorization
        return addResponse
    }
    override suspend fun changeState(authorization: String, number: String, state: UserCardStateRequest): Response<UserCardsResponse<UserCard>> {
        calls += "state:${state.estado}"; headers += authorization
        return Response.success(UserCardsResponse(true, data = card.copy(numeroTarjeta = number, estado = state.estado)))
    }
}

class UserCardsRepositoryTest {
    private val api = FakeUserCardsApi()
    private val repo = UserCardsRepository(api, { 1000L })
    private val session = UserSession(UsuarioRegistrado(id = "usr_a"), "private-test-token", 5000L)

    @Test fun cuentaNuevaDevuelveListaVaciaYEnviaBearer() = runBlocking {
        assertTrue(repo.list(session).isEmpty())
        assertEquals(listOf("Bearer private-test-token"), api.headers)
    }

    @Test fun incluyePaginasVaciasIntermediasSinPerderTarjetas() = runBlocking {
        api.listResponse = { _, cursor ->
            Response.success(UserCardsResponse(true, data = if (cursor == null) UserCardsPage(emptyList(), "next") else UserCardsPage(listOf(api.card))))
        }
        assertEquals(listOf(api.card), repo.list(session))
        assertEquals(listOf("list:", "list:next"), api.calls)
    }

    @Test fun detienePaginacionCircular() = runBlocking {
        api.listResponse = { _, _ -> Response.success(UserCardsResponse(true, data = UserCardsPage(emptyList(), "same"))) }
        try { repo.list(session); fail("No debe consultar indefinidamente") }
        catch (error: CardsException) { assertEquals(502, error.status) }
    }

    @Test fun noConsultaConSesionVencida() = runBlocking {
        try { repo.list(UserSession(session.user, session.accessToken, 1000L)); fail("Debió rechazar sesión") }
        catch (error: CardsException) { assertEquals(401, error.status) }
        assertTrue(api.calls.isEmpty())
    }

    @Test fun creaTarjetaSoloConConfirmacionDelServidor() = runBlocking {
        assertEquals(api.card, repo.add(session, api.card.numeroTarjeta, " Trabajo ", "General"))
        api.addResponse = Response.success(UserCardsResponse(false, data = api.card))
        try { repo.add(session, api.card.numeroTarjeta, "Trabajo", "General"); fail("No debe aceptar success false") }
        catch (error: CardsException) { assertTrue(error.message!!.contains("no confirmó")) }
    }

    @Test fun rechazaTarjetaDistintaALaSolicitada() = runBlocking {
        api.addResponse = Response.success(UserCardsResponse(true, data = api.card.copy(numeroTarjeta = "1234567890")))
        try { repo.add(session, api.card.numeroTarjeta, "Trabajo", "General"); fail("Número incorrecto") }
        catch (error: CardsException) { assertEquals(502, error.status) }
    }

    @Test fun identifica401YNoConfundeErrorConCuentaVacia() = runBlocking {
        api.listResponse = { _, _ -> Response.error(401, ResponseBody.create(MediaType.parse("application/json"), "{}")) }
        try { repo.list(session); fail("Debe informar sesión inválida") }
        catch (error: CardsException) { assertEquals(401, error.status) }
    }

    @Test fun rechazaRespuestaSinLista() = runBlocking {
        api.listResponse = { _, _ -> Response.success(UserCardsResponse(true, data = UserCardsPage(null))) }
        try { repo.list(session); fail("Ausencia de items no es cuenta vacía") }
        catch (error: CardsException) { assertEquals(502, error.status) }
    }

    @Test fun desactivaYReactivaMediantePatch() = runBlocking {
        assertFalse(repo.changeState(session, api.card.numeroTarjeta, false).isActive)
        assertTrue(repo.changeState(session, api.card.numeroTarjeta, true).isActive)
        assertEquals(listOf("state:INACTIVA", "state:ACTIVA"), api.calls)
    }

    @Test fun payloadNoEnviaUsuarioNiSaldoElegidoPorCliente() {
        val json = JsonParser.parseString(Gson().toJson(UserCardRequest("8754785478", "Trabajo", "General"))).asJsonObject
        assertEquals(setOf("numeroTarjeta", "alias", "tipo"), json.keySet())
    }

    @Test fun interpretaRespuestaExactaDeLambda() {
        val json = """{"numeroTarjeta":"8754785478","alias":"Trabajo","tipo":"Estudiante","estado":"ACTIVA","saldoCentimos":0,"saldoSimulado":true,"estadoTarjeta":"ACTIVA"}"""
        val card = Gson().fromJson(json, UserCard::class.java)
        assertTrue(card.isActive)
        assertTrue(card.saldoSimulado)
        assertEquals(0L, card.saldoCentimos)
    }
}
