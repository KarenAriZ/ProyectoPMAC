package com.karenarias.proyectopmac

import com.karenarias.proyectopmac.data.remote.*
import com.karenarias.proyectopmac.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class RegistroRepositoryTest {
    private val password = "MetroGo-Prueba-2026!"
    private val request get() = registroRequest(" Ana ", " Prueba ", " ANA@EXAMPLE.COM ", password)

    private fun repository(response: Response<RegistroUsuarioResponse>) = RegistroRepository(
        object : UsuarioApiService {
            override suspend fun registrarUsuario(usuario: RegistroUsuarioRequest) = response
        }
    )

    @Test fun validaLimitesDeLongitudDePassword() {
        for (length in listOf(10, 11, 128, 129)) {
            val password = "a".repeat(length)
            val error = validarRegistro(registroRequest("Ana", "Prueba", "ana@example.com", password), password)
            if (length in 11..128) assertNull(error) else assertNotNull(error)
        }
    }
    @Test fun normalizaDatosSinModificarPassword() {
        val data = registroRequest(" Ana ", " Prueba ", " ANA@EXAMPLE.COM ", " $password ")
        assertEquals("Ana", data.nombre)
        assertEquals("Prueba", data.apellido)
        assertEquals("ana@example.com", data.correoElectronico)
        assertEquals(" $password ", data.contrasena)
    }

    @Test fun validaCamposAntesDeEnviar() {
        assertNull(validarRegistro(request, password))
        assertNotNull(validarRegistro(registroRequest("", "Prueba", "ana@example.com", password), password))
        assertNotNull(validarRegistro(registroRequest("Ana", "Prueba", "correo-invalido", password), password))
        assertNotNull(validarRegistro(registroRequest("Ana", "Prueba", "ana@example.com", "123"), "123"))
        assertNotNull(validarRegistro(request, "otra contraseña"))
    }

    @Test fun aceptaSoloConfirmacionConId() = runBlocking {
        val repo = repository(Response.success(201,
            RegistroUsuarioResponse(true, "Creado", UsuarioRegistrado(id = "usr_test"))))
        assertEquals("usr_test", repo.registrar(request).id)
    }

    @Test fun rechazaRespuestaExitosaSinUsuario() = runBlocking {
        val repo = repository(Response.success(RegistroUsuarioResponse(success = true)))
        try {
            repo.registrar(request)
            fail("Una respuesta sin ID no confirma el registro")
        } catch (expected: RegistroException) {
            assertTrue(expected.message!!.contains("No se pudo confirmar"))
        }
    }

    @Test fun informaCorreoDuplicado() = runBlocking {
        val body = ResponseBody.create(MediaType.parse("application/json"), """{"success":false,"message":"Duplicado"}""")
        try {
            repository(Response.error(409, body)).registrar(request)
            fail("Debió informar el correo duplicado")
        } catch (expected: RegistroException) {
            assertTrue(expected.message!!.contains("Ya existe"))
        }
    }

    @Test fun manejaErrorDeServidorQueNoEsJson() = runBlocking {
        val body = ResponseBody.create(MediaType.parse("text/plain"), "Bad gateway")
        try {
            repository(Response.error(502, body)).registrar(request)
            fail("Debió informar el error de servidor")
        } catch (expected: RegistroException) {
            assertTrue(expected.message!!.contains("servidor"))
        }
    }
}


