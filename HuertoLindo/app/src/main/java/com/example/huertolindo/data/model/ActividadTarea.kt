package com.example.huertolindo.data.model

data class ActividadTarea(
    val id: String,
    val tareaId: String,
    val tituloTarea: String,
    val usuarioRelacionadoId: String,
    val usuarioId: String,
    val nombreUsuario: String,
    val accion: String,
    val estado: String,
    val fecha: String
)
