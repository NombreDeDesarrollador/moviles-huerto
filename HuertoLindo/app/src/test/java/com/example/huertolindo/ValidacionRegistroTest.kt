package com.example.huertolindo

import com.example.huertolindo.presentation.validation.ValidacionRegistro
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionRegistroTest {

    @Test
    fun registroValidoRetornaEsValidoTrueYSinErrores() {
        val resultado = ValidacionRegistro.validar(
            nombre = "Ana",
            apellido = "Pérez",
            correo = "ana.perez@huertolindo.cl",
            password = "Password123!",
            confirmarPassword = "Password123!",
            mostrarErrores = true
        )
        assertTrue(resultado.esValido)
        assertNull(resultado.errorNombre)
        assertNull(resultado.errorApellido)
        assertNull(resultado.errorCorreo)
        assertNull(resultado.errorPassword)
        assertNull(resultado.errorConfirmarPassword)
    }

    @Test
    fun nombreOApellidoCortosGeneranErrorSiMostrarErroresTrue() {
        val resultado = ValidacionRegistro.validar(
            nombre = "A",
            apellido = "1",
            correo = "test@huertolindo.cl",
            password = "pass",
            confirmarPassword = "pass",
            mostrarErrores = true
        )
        assertFalse(resultado.esValido)
        assertNotNull(resultado.errorNombre)
        assertNotNull(resultado.errorApellido)
    }

    @Test
    fun correoInvalidoGeneraError() {
        val resultado = ValidacionRegistro.validar(
            nombre = "Juan",
            apellido = "Gómez",
            correo = "correo-invalido",
            password = "pass",
            confirmarPassword = "pass",
            mostrarErrores = true
        )
        assertFalse(resultado.esValido)
        assertNotNull(resultado.errorCorreo)
    }

    @Test
    fun passwordEnBlancoOConfirmacionNoCoincidenteGeneranError() {
        val passEnBlanco = ValidacionRegistro.validar(
            nombre = "Juan",
            apellido = "Gómez",
            correo = "juan@huertolindo.cl",
            password = "",
            confirmarPassword = "",
            mostrarErrores = true
        )
        assertFalse(passEnBlanco.esValido)
        assertNotNull(passEnBlanco.errorPassword)

        val passNoCoincide = ValidacionRegistro.validar(
            nombre = "Juan",
            apellido = "Gómez",
            correo = "juan@huertolindo.cl",
            password = "Password123!",
            confirmarPassword = "PasswordOtra!",
            mostrarErrores = true
        )
        assertFalse(passNoCoincide.esValido)
        assertNotNull(passNoCoincide.errorConfirmarPassword)
    }

    @Test
    fun siMostrarErroresFalseNoRetornaMensajesDeError() {
        val resultado = ValidacionRegistro.validar(
            nombre = "",
            apellido = "",
            correo = "",
            password = "",
            confirmarPassword = "",
            mostrarErrores = false
        )
        assertFalse(resultado.esValido)
        assertNull(resultado.errorNombre)
        assertNull(resultado.errorApellido)
        assertNull(resultado.errorCorreo)
        assertNull(resultado.errorPassword)
        assertNull(resultado.errorConfirmarPassword)
    }
}
