package com.example.huertolindo.data.model

object EstadoCultivo {
    val opciones = listOf(
        "Sembrado",
        "En crecimiento",
        "Desarrollo",
        "Listo para cosechar"
    )

    fun esValido(estado: String): Boolean = estado in opciones
}
