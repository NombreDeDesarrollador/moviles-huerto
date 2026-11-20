package com.example.huertolindo.domain.model

object EstadoCultivo {
    const val PLANIFICADO = "Planificado"
    const val SEMBRADO = "Sembrado"
    const val EN_CRECIMIENTO = "En crecimiento"
    const val DESARROLLO = "Desarrollo"
    const val LISTO_COSECHA = "Listo para cosechar"
    const val COSECHADO = "Cosechado"
    const val CON_PROBLEMAS = "Con problemas"
    const val FINALIZADO = "Finalizado"

    val opciones = listOf(
        PLANIFICADO,
        SEMBRADO,
        EN_CRECIMIENTO,
        DESARROLLO,
        LISTO_COSECHA,
        COSECHADO,
        CON_PROBLEMAS,
        FINALIZADO
    )

    fun esValido(estado: String): Boolean = estado in opciones
}
