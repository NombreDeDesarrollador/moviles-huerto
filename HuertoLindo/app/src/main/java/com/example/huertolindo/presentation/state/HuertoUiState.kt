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
    val usuarioActual: Usuario?,
    val fotoPerfilUri: Uri?,
    val sectores: List<Sector>,
    val cultivos: List<Cultivo>,
    val tareas: List<Tarea>,
    val historial: List<ActividadTarea>,
    val evidencias: List<Evidencia>,
    val validaciones: List<Validacion>,
    val comentariosEntrega: Map<String, String>,
    val reportesProblemas: List<ReporteProblema>,
    val cargandoReportesProblemas: Boolean,
    val errorReportesProblemas: String?,
    val usuariosDisponibles: List<Usuario>,
    val errorCargaUsuarios: String?,
    val cargandoDatos: Boolean,
    val errorPersistencia: String?
)
