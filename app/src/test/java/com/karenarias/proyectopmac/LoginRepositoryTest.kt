package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class LoginRepositoryTest {
    private val request = loginRequest(" ANA@EXAMPLE.COM ", " password original ")
    private fun repo(response: Response<LoginResponse>) = LoginRepository(object : LoginApiService {
        override suspend fun login(request: LoginRequest) = response
    }, nowSeconds = { 1000L })

    @Test fun normalizaCorreoYConservaPassword() {
        assertEquals("ana@example.com", request.correoElectronico)
        assertEquals(" password original ", request.contrasena)
        assertNull(validarLogin(request))
        assertNotNull(validarLogin(loginRequest("sin-arroba", "password")))
        assertNotNull(validarLogin(loginRequest("ana@example.com", "")))
    }

    @Test fun admiteSesionValidaYNombreDelServidor() = runBlocking {
        val response = LoginResponse(true, data = LoginData(
            UsuarioRegistrado(id = "usr_test", nombre = "Ana", apellido = "Prueba"),
            "token-de-prueba", "Bearer", 3600L, 4600L))
        val session = repo(Response.success(response)).login(request)
        assertEquals("Ana Prueba", session.displayName)
        assertFalse(session.isExpired(4599L))
        assertTrue(session.isExpired(4600L))
    }

    @Test fun rechazaSesionVencidaSinTokenOSinUsuario() = runBlocking {
        val user = UsuarioRegistrado(id = "usr_test")
        val invalid = listOf(
            LoginData(user, "token", "Bearer", 3600L, 1000L),
            LoginData(user, "", "Bearer", 3600L, 4600L),
            LoginData(null, "token", "Bearer", 3600L, 4600L),
            LoginData(user, "token", "Otro", 3600L, 4600L)
        )
        for (data in invalid) {
            try {
                repo(Response.success(LoginResponse(true, data = data))).login(request)
                fail("No debe admitir una sesión inválida")
            } catch (expected: LoginException) {
                assertTrue(expected.message!!.contains("sesión válida"))
            }
        }
    }

    @Test fun rechazaSuccessFalseAunqueIncluyaToken() = runBlocking {
        try {
            repo(Response.success(LoginResponse(false, data = LoginData(
                UsuarioRegistrado(id = "usr_test"), "token", "Bearer", 3600L, 4600L)))).login(request)
            fail("No debe admitir success false")
        } catch (expected: LoginException) {
            assertTrue(expected.message!!.contains("sesión válida"))
        }
    }

    @Test fun maneja401429Y500SinMostrarDetallesInternos() = runBlocking {
        for ((code, message) in listOf(401 to "incorrectos", 429 to "Demasiados", 500 to "más tarde")) {
            val body = ResponseBody.create(MediaType.parse("text/plain"), "detalle privado")
            try {
                repo(Response.error(code, body)).login(request)
                fail("No debe admitir HTTP $code")
            } catch (expected: LoginException) {
                assertTrue(expected.message!!.contains(message))
                assertFalse(expected.message!!.contains("privado"))
            }
        }
    }
}
