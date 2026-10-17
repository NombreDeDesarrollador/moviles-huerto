package com.example.huertolindo.data.model

object TipoTarea {
    const val RIEGO = "Riego"
    const val SIEMBRA = "Siembra"
    const val MANTENIMIENTO = "Mantenimiento"
    const val OBSERVACION = "Observación"

    val opciones = listOf(RIEGO, SIEMBRA, MANTENIMIENTO, OBSERVACION)

    fun esValido(tipo: String): Boolean = tipo in opciones
}
