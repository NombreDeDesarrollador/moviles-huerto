package com.example.huertolindo.presentation.state

import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Tarea

data class ReportesHistorialUiState(
    val reportesProblemas: List<ReporteProblema> = emptyList(),
    val historial: List<ActividadTarea> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    val alertasTareasAtrasadas: List<Tarea> = emptyList(),
    val alertasProximosRiegos: List<Cultivo> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
