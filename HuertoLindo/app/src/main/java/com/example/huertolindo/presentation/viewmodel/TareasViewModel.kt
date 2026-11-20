package com.example.huertolindo.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.usecase.GestionTareasUseCase
import com.example.huertolindo.presentation.state.TareasUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TareasViewModel(
    application: Application,
    private val huertoRepository: HuertoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val tareasUseCase: GestionTareasUseCase = GestionTareasUseCase(huertoRepository)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TareasUiState())
    val uiState: StateFlow<TareasUiState> = _uiState.asStateFlow()

    val tareas: List<Tarea> get() = _uiState.value.tareas
    val validaciones: List<Validacion> get() = _uiState.value.validaciones
    val evidencias: List<Evidencia> get() = _uiState.value.evidencias

    init {
        cargarTareas()
    }

    fun cargarTareas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val snapshot = withContext(ioDispatcher) {
                    huertoRepository.inicializar()
                    huertoRepository.cargarSnapshot()
                }
                val atrasadas = tareasUseCase.tareasAtrasadas(snapshot.tareas)
                val comentarios = snapshot.evidencias
                    .filter { !it.tareaId.isNullOrBlank() && it.descripcion.isNotBlank() }
                    .groupBy { it.tareaId!! }
                    .mapValues { it.value.first().descripcion }

                _uiState.update {
                    it.copy(
                        tareas = snapshot.tareas,
                        tareasAtrasadas = atrasadas,
                        validaciones = snapshot.validaciones,
                        evidencias = snapshot.evidencias,
                        comentariosEntrega = comentarios,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Error al cargar tareas: ${e.message}")
                }
            }
        }
    }

    fun tareasVisibles(usuario: Usuario?): List<Tarea> {
        return tareasUseCase.tareasVisibles(usuario?.perfil, usuario?.id, tareas)
    }

    fun tareasAtrasadas(usuario: Usuario?): List<Tarea> {
        return tareasUseCase.tareasAtrasadas(tareasVisibles(usuario))
    }

    fun crearTarea(
        titulo: String,
        descripcion: String,
        sector: Sector,
        cultivoId: String?,
        estudiantes: List<Usuario>,
        actor: Usuario,
        fechaProgramada: String,
        tipo: String
    ): Boolean {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val creadas = withContext(ioDispatcher) {
                    tareasUseCase.crearTareas(
                        titulo, descripcion, sector, cultivoId,
                        estudiantes, actor, fechaProgramada, tipo
                    )
                }
                if (creadas.isNotEmpty()) {
                    val nuevasTareas = creadas.map { it.first }
                    _uiState.update {
                        it.copy(
                            tareas = it.tareas + nuevasTareas,
                            successMessage = "Tarea asignada exitosamente",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Completa todos los campos obligatorios.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al crear tarea: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun iniciarTarea(
        tareaId: String,
        usuario: Usuario,
        onResult: (String?) -> Unit = {}
    ) {
        val tarea = tareas.firstOrNull { it.id == tareaId }
        if (tarea == null) {
            onResult("Tarea no encontrada.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resultado = withContext(ioDispatcher) {
                    tareasUseCase.iniciarTarea(tarea, usuario)
                }
                if (resultado != null) {
                    _uiState.update {
                        it.copy(
                            tareas = it.tareas.map { t -> if (t.id == tareaId) resultado.first else t },
                            successMessage = "Tarea iniciada",
                            errorMessage = null
                        )
                    }
                    onResult(null)
                } else {
                    onResult("No tienes permisos para iniciar esta tarea.")
                }
            } catch (e: Exception) {
                onResult(e.message ?: "Error al iniciar tarea.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun entregarTarea(
        tareaId: String,
        comentario: String,
        imagenUri: Uri?,
        estudiante: Usuario,
        onResult: (String?) -> Unit
    ) {
        val tarea = tareas.firstOrNull { it.id == tareaId }
        if (tarea == null) {
            onResult("Tarea no encontrada.")
            return
        }
        if (comentario.isBlank()) {
            onResult("Escribe un comentario sobre el trabajo realizado.")
            return
        }
        if (imagenUri == null) {
            onResult("Adjunta una fotografía como evidencia.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resultado = withContext(ioDispatcher) {
                    tareasUseCase.entregarTarea(tarea, comentario, imagenUri, estudiante)
                }
                _uiState.update {
                    it.copy(
                        tareas = it.tareas.map { t -> if (t.id == tareaId) resultado.first else t },
                        evidencias = it.evidencias + resultado.second,
                        comentariosEntrega = it.comentariosEntrega + (tareaId to comentario.trim()),
                        successMessage = "Tarea entregada correctamente",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al entregar tarea.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun validarTarea(
        tareaId: String,
        administrador: Usuario,
        onResult: (String?) -> Unit
    ) {
        val tarea = tareas.firstOrNull { it.id == tareaId }
        if (tarea == null) {
            onResult("Tarea no encontrada.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resultado = withContext(ioDispatcher) {
                    tareasUseCase.validarTarea(tarea, administrador)
                }
                _uiState.update {
                    it.copy(
                        tareas = it.tareas.map { t -> if (t.id == tareaId) resultado.first else t },
                        validaciones = it.validaciones + resultado.second,
                        successMessage = "Tarea validada",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al validar tarea.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun rechazarTarea(
        tareaId: String,
        motivo: String,
        administrador: Usuario,
        onResult: (String?) -> Unit
    ) {
        val tarea = tareas.firstOrNull { it.id == tareaId }
        if (tarea == null) {
            onResult("Tarea no encontrada.")
            return
        }
        if (motivo.isBlank()) {
            onResult("Indica el motivo del rechazo.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resultado = withContext(ioDispatcher) {
                    tareasUseCase.rechazarTarea(tarea, motivo, administrador)
                }
                _uiState.update {
                    it.copy(
                        tareas = it.tareas.map { t -> if (t.id == tareaId) resultado.first else t },
                        validaciones = it.validaciones + resultado.second,
                        successMessage = "Tarea rechazada con feedback",
                        errorMessage = null
                    )
                }
                onResult(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
                onResult(e.message ?: "Error al rechazar tarea.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun evidenciasDeTarea(tareaId: String): List<Evidencia> = evidencias.filter { it.tareaId == tareaId }

    fun actualizarFiltros(estado: String = "", fecha: String = "", responsable: String = "") {
        _uiState.update { it.copy(filtroEstado = estado, filtroFecha = fecha, filtroResponsable = responsable) }
    }

    fun limpiarError() { _uiState.update { it.copy(errorMessage = null) } }
    fun limpiarExito() { _uiState.update { it.copy(successMessage = null) } }
}
