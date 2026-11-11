package com.example.huertolindo.domain.model

object EstadoCultivo {
    val opciones = listOf(
        "Sembrado",
        "Planificado",
        "En crecimiento",
        "Desarrollo",
        "Listo para cosechar",
        "Cosechado"
    )

    fun esValido(estado: String): Boolean = estado in opciones
}
