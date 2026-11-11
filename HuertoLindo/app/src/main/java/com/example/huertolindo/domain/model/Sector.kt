package com.example.huertolindo.domain.model


data class Sector(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val estado: String,
    val ubicacion: String = "",
    val fechaInicioMantenimiento: String = "",
    val fechaTerminoMantenimiento: String = ""
)