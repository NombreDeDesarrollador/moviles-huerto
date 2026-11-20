package com.example.huertolindo.presentation.state

import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Validacion

data class TareasUiState(
    val tareas: List<Tarea> = emptyList(),
    val tareasAtrasadas: List<Tarea> = emptyList(),
    val validaciones: List<Validacion> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    val comentariosEntrega: Map<String, String> = emptyMap(),
    val filtroEstado: String = "",
    val filtroFecha: String = "",
    val filtroResponsable: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
