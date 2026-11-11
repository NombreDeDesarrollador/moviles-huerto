package com.example.huertolindo.domain.model


data class Tarea(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val sectorId: String,
    val asignadoA: String,
    val estado: String,
    val fechaProgramada: String = "",
    val motivoRechazo: String = "",
    val tipo: String = TipoTarea.MANTENIMIENTO
)