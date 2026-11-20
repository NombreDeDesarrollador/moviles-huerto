package com.example.huertolindo.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.usecase.GestionCultivosUseCase
import com.example.huertolindo.domain.usecase.GestionReportesUseCase
import com.example.huertolindo.domain.usecase.GestionTareasUseCase
import com.example.huertolindo.presentation.state.ReportesHistorialUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReportesHistorialViewModel(
    application: Application,
    private val huertoRepository: HuertoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val reportesUseCase: GestionReportesUseCase = GestionReportesUseCase(huertoRepository),
    private val cultivosUseCase: GestionCultivosUseCase = GestionCultivosUseCase(huertoRepository),
    private val tareasUseCase: GestionTareasUseCase = GestionTareasUseCase(huertoRepository)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReportesHistorialUiState())
    val uiState: StateFlow<ReportesHistorialUiState> = _uiState.asStateFlow()

    val reportesProblemas: List<ReporteProblema> get() = _uiState.value.reportesProblemas
    val historial: List<ActividadTarea> get() = _uiState.value.historial
    val evidencias: List<Evidencia> get() = _uiState.value.evidencias

    init {
        cargarDatos()
    }

    fun cargarDatos(usuario: Usuario? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val snapshot = withContext(ioDispatcher) {
                    huertoRepository.inicializar()
                    huertoRepository.cargarSnapshot()
                }
                val evidenciasExistentes = snapshot.evidencias.filter { File(it.rutaArchivo).isFile }
                val atrasadas = tareasUseCase.tareasAtrasadas(snapshot.tareas)
                val proximosRiegos = cultivosUseCase.cultivosConRiegoPendiente(snapshot.cultivos)

                val historialFiltrado = if (usuario != null && usuario.perfil != "ADMIN" && usuario.perfil != "administrador") {
                    snapshot.actividades.filter { it.usuarioRelacionadoId == usuario.id }
                } else {
                    snapshot.actividades
                }

                _uiState.update {
                    it.copy(
                        reportesProblemas = snapshot.reportesProblemas,
                        historial = historialFiltrado,
                        evidencias = evidenciasExistentes,
                        alertasTareasAtrasadas = atrasadas,
                        alertasProximosRiegos = proximosRiegos,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error al cargar historial y reportes: ${e.message}")
                }
            }
        }
    }

    fun crearReporteProblema(
        descripcion: String,
        sector: Sector,
        cultivo: Cultivo?,
        tipoProblema: String,
        usuario: Usuario,
        fotografia: Uri,
        onResult: (String?) -> Unit
    ) {
        if (descripcion.isBlank()) {
            onResult("Describe el problema detalladamente.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reporte = withContext(ioDispatcher) {
                    reportesUseCase.crearReporteProblema(
                        descripcion, sector, cultivo, tipoProblema, usuario, fotografia
                    )
                }
                _uiState.update {
                    it.copy(
                        reportesProblemas = it.reportesProblemas + reporte,
                        successMessage = "Reporte registrado correctamente",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "No se pudo registrar el reporte.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun actualizarEstadoProblema(
        reporte: ReporteProblema,
        nuevoEstado: String,
        comentario: String,
        administrador: Usuario,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val actualizado = withContext(ioDispatcher) {
                    reportesUseCase.actualizarEstadoProblema(reporte, nuevoEstado, comentario, administrador)
                }
                _uiState.update {
                    it.copy(
                        reportesProblemas = it.reportesProblemas.map { r -> if (r.id == reporte.id) actualizado else r },
                        successMessage = "Estado de reporte actualizado",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al actualizar estado.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun agregarEvidencia(
        tipoAsociacion: TipoAsociacionEvidencia,
        entidadId: String,
        imagenUri: Uri,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val evidencia = withContext(ioDispatcher) {
                    huertoRepository.guardarEvidenciaAsociada(tipoAsociacion, entidadId, imagenUri.toString())
                }
                _uiState.update {
                    it.copy(
                        evidencias = it.evidencias + evidencia,
                        successMessage = "Fotografía guardada",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al guardar evidencia.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun eliminarEvidencia(
        evidenciaId: String,
        onResult: (String?) -> Unit
    ) {
        val evidencia = evidencias.firstOrNull { it.id == evidenciaId }
        if (evidencia == null) {
            onResult("Evidencia no encontrada.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(ioDispatcher) {
                    huertoRepository.eliminarEvidencia(evidencia)
                }
                _uiState.update {
                    it.copy(
                        evidencias = it.evidencias.filterNot { e -> e.id == evidenciaId },
                        successMessage = "Fotografía eliminada",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al eliminar fotografía.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun limpiarError() { _uiState.update { it.copy(errorMessage = null) } }
    fun limpiarExito() { _uiState.update { it.copy(successMessage = null) } }
}
