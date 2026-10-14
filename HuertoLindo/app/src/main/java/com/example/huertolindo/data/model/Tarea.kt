package com.example.huertolindo.data.model

data class Tarea(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val sectorId: String,
    val asignadoA: String,
    val estado: String
)