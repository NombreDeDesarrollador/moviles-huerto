package com.example.huertolindo.data.model

data class SensorSimulado(
    val sectorId: String,
    val humedadSuelo: Double,
    val temperaturaAmbiental: Double,
    val humedadAmbiental: Double,
    val origen: String = "Simulación académica"
)