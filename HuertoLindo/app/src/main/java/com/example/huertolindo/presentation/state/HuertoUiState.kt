package com.example.huertolindo.presentation.state

import android.net.Uri
import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Validacion

data class HuertoUiState(
    val usuarioActual: Usuario? = null,
    val fotoPerfilUri: Uri? = null,
    val sectores: List<Sector> = emptyList(),
    val cultivos: List<Cultivo> = emptyList(),
    val tareas: List<Tarea> = emptyList(),
    val historial: List<ActividadTarea> = emptyList(),
    val evidencias: List<Evidencia> = emptyList(),
    val validaciones: List<Validacion> = emptyList(),
    val comentariosEntrega: Map<String, String> = emptyMap(),
    val reportesProblemas: List<ReporteProblema> = emptyList(),
    val cargandoReportesProblemas: Boolean = false,
    val errorReportesProblemas: String? = null,
    val usuariosDisponibles: List<Usuario> = emptyList(),
    val errorCargaUsuarios: String? = null,
    val cargandoDatos: Boolean = false,
    val errorPersistencia: String? = null,
    val isLoading: Boolean = cargandoDatos,
    val errorMessage: String? = errorPersistencia,
    val successMessage: String? = null
)
