package com.example.huertolindo.domain.model


data class Validacion(
    val id: String,
    val tareaId: String,
    val administradorId: String,
    val resultado: String,
    val observaciones: String = "",
    val fechaValidacion: String = ""
)