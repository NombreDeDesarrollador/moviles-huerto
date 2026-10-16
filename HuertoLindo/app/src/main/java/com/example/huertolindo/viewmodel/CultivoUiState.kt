package com.example.huertolindo.viewmodel

data class CultivoUiState(
    val nombre: String = "",
    val sector: String = "",
    val fechaSiembra: String = "",
    val estado: String = "",
    val errorNombre: Boolean = false,
    val errorSector: Boolean = false
)