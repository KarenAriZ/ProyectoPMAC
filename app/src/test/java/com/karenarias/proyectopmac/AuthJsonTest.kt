package com.karenarias.proyectopmac

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.karenarias.proyectopmac.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class AuthJsonTest {
    private val gson = Gson()

    @Test fun registroEnviaNombresExactosDeLaLambda() {
        val json = JsonParser.parseString(gson.toJson(
            RegistroUsuarioRequest("Ana", "Prueba", "ana@example.com", "password-de-prueba")
        )).asJsonObject
        assertEquals(setOf("nombre", "apellido", "correoElectronico", "contrasena"), json.keySet())
        assertEquals("password-de-prueba", json.get("contrasena").asString)
    }

    @Test fun loginEnviaCorreoYPasswordOriginal() {
        val json = JsonParser.parseString(gson.toJson(
            LoginRequest("ana@example.com", " password original ")
        )).asJsonObject
        assertEquals(setOf("correoElectronico", "contrasena"), json.keySet())
        assertEquals(" password original ", json.get("contrasena").asString)
    }

    @Test fun interpretaRespuestaActualDeLogin() {
        val json = """{"success":true,"message":"Inicio de sesión exitoso","data":{"usuario":{"id":"usr_test","nombre":"Ana","apellido":"Prueba","correoElectronico":"ana@example.com"},"accessToken":"token-ficticio","tokenType":"Bearer","expiresIn":3600,"expiresAt":2000000000},"timestamp":"2026-10-15T00:00:00Z"}"""
        val response = gson.fromJson(json, LoginResponse::class.java)
        assertTrue(response.success)
        assertEquals("Ana", response.data!!.usuario!!.nombre)
        assertEquals(2000000000L, response.data!!.expiresAt)
        assertEquals("token-ficticio", response.data!!.accessToken)
        assertFalse(response.toString().contains("token-ficticio"))
        assertFalse(response.data.toString().contains("token-ficticio"))
    }

    @Test fun interpretaRespuestaActualDeRegistro() {
        val response = gson.fromJson("""{"success":true,"message":"Cuenta creada correctamente","data":{"id":"usr_test","nombre":"Ana","apellido":"Prueba","correoElectronico":"ana@example.com"}}""", RegistroUsuarioResponse::class.java)
        assertTrue(response.success)
        assertEquals("usr_test", response.data!!.id)
    }
}
