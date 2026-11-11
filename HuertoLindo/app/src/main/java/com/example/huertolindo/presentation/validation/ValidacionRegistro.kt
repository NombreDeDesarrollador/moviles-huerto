package com.example.huertolindo.presentation.validation

import android.util.Patterns

data class ResultadoValidacionRegistro(
    val correoNormalizado: String,
    val errorNombre: String?,
    val errorApellido: String?,
    val errorCorreo: String?,
    val errorPassword: String?,
    val errorConfirmarPassword: String?,
    val esValido: Boolean
)

object ValidacionRegistro {
    fun validar(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        confirmarPassword: String,
        mostrarErrores: Boolean
    ): ResultadoValidacionRegistro {
        val correoNormalizado = correo.trim()
        val nombreValido = nombre.count { it.isLetter() } >= 2
        val apellidoValido = apellido.count { it.isLetter() } >= 2
        val correoValido = Patterns.EMAIL_ADDRESS.matcher(correoNormalizado).matches()
        val passwordValido = password.isNotBlank()
        val confirmacionValida = confirmarPassword.isNotBlank() && confirmarPassword == password

        return ResultadoValidacionRegistro(
            correoNormalizado = correoNormalizado,
            errorNombre = if (mostrarErrores && !nombreValido) {
                "El nombre debe tener al menos 2 letras."
            } else {
                null
            },
            errorApellido = if (mostrarErrores && !apellidoValido) {
                "El apellido debe tener al menos 2 letras."
            } else {
                null
            },
            errorCorreo = when {
                !mostrarErrores -> null
                correoNormalizado.isEmpty() -> "El correo electrónico es obligatorio."
                !correoValido -> "Ingresa un correo electrónico válido."
                else -> null
            },
            errorPassword = if (mostrarErrores && !passwordValido) {
                "La contraseña es obligatoria."
            } else {
                null
            },
            errorConfirmarPassword = when {
                !mostrarErrores -> null
                confirmarPassword.isBlank() -> "Confirma tu contraseña."
                confirmarPassword != password -> "Las contraseñas no coinciden."
                else -> null
            },
            esValido = nombreValido && apellidoValido && correoValido &&
                passwordValido && confirmacionValida
        )
    }
}
