package com.example.huertolindo.domain.model


data class Cultivo(
    val id: String,
    val nombreComun: String,
    val sectorId: String,
    val fechaSiembra: String,
    val estadoDesarrollo: String,
    val frecuenciaRiego: String,
    val fechaCosechaEstimada: String = ""
)