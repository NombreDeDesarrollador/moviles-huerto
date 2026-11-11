package com.example.huertolindo.domain.model


data class ReporteProblema(
    val id: String,
    val sectorId: String,
    val cultivoId: String? = null,
    val usuarioId: String,
    val descripcion: String,
    val prioridad: String,
    val estado: String,
    val fechaReporte: String = "",
    val tipoProblema: String = "Otro",
    val comentarioSeguimiento: String = "",
    val fechaResolucion: String = ""
)