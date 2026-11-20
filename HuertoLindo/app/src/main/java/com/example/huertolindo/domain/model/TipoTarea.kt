package com.example.huertolindo.domain.model

object TipoTarea {
    const val RIEGO = "Riego"
    const val SIEMBRA = "Siembra"
    const val MANTENIMIENTO = "Mantenimiento"
    const val OBSERVACION = "Observación"
    const val PODA = "Poda"
    const val COSECHA = "Cosecha"

    val opciones = listOf(RIEGO, SIEMBRA, MANTENIMIENTO, OBSERVACION, PODA, COSECHA)

    fun esValido(tipo: String): Boolean = tipo in opciones
}
