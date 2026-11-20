package com.example.huertolindo.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.SensorSimulado
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.usecase.GestionCultivosUseCase
import com.example.huertolindo.domain.usecase.GestionSectoresUseCase
import com.example.huertolindo.presentation.state.SectoresCultivosUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SectoresCultivosViewModel(
    application: Application,
    private val huertoRepository: HuertoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sectoresUseCase: GestionSectoresUseCase = GestionSectoresUseCase(huertoRepository),
    private val cultivosUseCase: GestionCultivosUseCase = GestionCultivosUseCase(huertoRepository)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SectoresCultivosUiState())
    val uiState: StateFlow<SectoresCultivosUiState> = _uiState.asStateFlow()

    val sectores: List<Sector> get() = _uiState.value.sectores
    val cultivos: List<Cultivo> get() = _uiState.value.cultivos
    val sensores: List<SensorSimulado> get() = _uiState.value.sensores
    val cultivosConRiegoPendiente: List<Cultivo> get() = cultivosUseCase.cultivosConRiegoPendiente(cultivos)

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val snapshot = withContext(ioDispatcher) {
                    huertoRepository.inicializar()
                    huertoRepository.cargarSnapshot()
                }
                val sensoresGenerados = snapshot.sectores.map { sector ->
                    if (sector.id == "1") {
                        SensorSimulado(sector.id, 62.0, 21.5, 54.0, "Simulación académica / Dato de prueba")
                    } else {
                        SensorSimulado(sector.id, 48.0, 23.0, 49.0, "Simulación académica / Dato de prueba")
                    }
                }
                _uiState.update {
                    it.copy(
                        sectores = snapshot.sectores,
                        cultivos = snapshot.cultivos,
                        sensores = sensoresGenerados,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar sectores y cultivos: ${e.message}"
                    )
                }
            }
        }
    }

    fun agregarSector(
        nombre: String,
        ubicacion: String,
        descripcion: String,
        estado: String,
        inicioMantenimiento: String = "",
        terminoMantenimiento: String = "",
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        if (!sectoresUseCase.estadoValido(estado, inicioMantenimiento, terminoMantenimiento) ||
            nombre.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Datos de sector inválidos.") }
            return false
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val nuevoSector = withContext(ioDispatcher) {
                    sectoresUseCase.agregarSector(
                        nombre, ubicacion, descripcion, estado,
                        inicioMantenimiento, terminoMantenimiento, actorPerfil
                    )
                }
                if (nuevoSector != null) {
                    _uiState.update {
                        it.copy(
                            sectores = it.sectores + nuevoSector,
                            successMessage = "Sector agregado exitosamente",
                            errorMessage = null
                        )
                    }
                    actualizarLecturasSensores()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo guardar el sector: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun editarSector(
        sectorId: String,
        nombre: String,
        ubicacion: String,
        descripcion: String,
        estado: String,
        inicioMantenimiento: String = "",
        terminoMantenimiento: String = "",
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val actualizado = withContext(ioDispatcher) {
                    sectoresUseCase.editarSector(
                        sectorId, nombre, ubicacion, descripcion, estado,
                        inicioMantenimiento, terminoMantenimiento, actorPerfil
                    )
                }
                if (actualizado != null) {
                    _uiState.update {
                        it.copy(
                            sectores = it.sectores.map { s -> if (s.id == sectorId) actualizado else s },
                            successMessage = "Sector actualizado",
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo actualizar el sector: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun eliminarSector(
        sectorId: String,
        tareas: List<Tarea> = emptyList(),
        reportes: List<ReporteProblema> = emptyList(),
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        if (!sectoresUseCase.puedeEliminar(sectorId, cultivos, tareas, reportes)) {
            _uiState.update { it.copy(errorMessage = "No se puede eliminar un sector con cultivos, tareas o reportes.") }
            return false
        }
        val sector = sectores.firstOrNull { it.id == sectorId } ?: return false
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(ioDispatcher) {
                    sectoresUseCase.eliminarSector(sector, actorPerfil)
                }
                _uiState.update {
                    it.copy(
                        sectores = it.sectores.filterNot { s -> s.id == sectorId },
                        successMessage = "Sector eliminado",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "No se pudo eliminar el sector: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun agregarCultivo(
        nombre: String,
        sector: Sector,
        fechaSiembra: String,
        fechaCosechaEstimada: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String,
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val nuevoCultivo = withContext(ioDispatcher) {
                    cultivosUseCase.agregarCultivo(
                        nombre, sector, fechaSiembra, fechaCosechaEstimada,
                        estadoDesarrollo, frecuenciaRiego, actorPerfil
                    )
                }
                if (nuevoCultivo != null) {
                    _uiState.update {
                        it.copy(
                            cultivos = it.cultivos + nuevoCultivo,
                            successMessage = "Cultivo agregado",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Datos de cultivo inválidos.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al guardar el cultivo: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun editarCultivo(
        cultivoActual: Cultivo,
        nombre: String,
        fechaSiembra: String,
        fechaCosechaEstimada: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String,
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val actualizado = withContext(ioDispatcher) {
                    cultivosUseCase.editarCultivo(
                        cultivoActual, nombre, fechaSiembra, fechaCosechaEstimada,
                        estadoDesarrollo, frecuenciaRiego, actorPerfil
                    )
                }
                if (actualizado != null) {
                    _uiState.update {
                        it.copy(
                            cultivos = it.cultivos.map { c -> if (c.id == cultivoActual.id) actualizado else c },
                            successMessage = "Cultivo actualizado",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Datos de cultivo inválidos.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al editar el cultivo: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun eliminarCultivo(
        cultivo: Cultivo,
        tareas: List<Tarea> = emptyList(),
        reportes: List<ReporteProblema> = emptyList(),
        actorPerfil: String? = "ADMIN"
    ): Boolean {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val eliminado = withContext(ioDispatcher) {
                    cultivosUseCase.eliminarCultivo(cultivo, tareas, reportes, actorPerfil)
                }
                if (eliminado) {
                    _uiState.update {
                        it.copy(
                            cultivos = it.cultivos.filterNot { c -> c.id == cultivo.id },
                            successMessage = "Cultivo eliminado",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "No se puede eliminar un cultivo con tareas o reportes.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al eliminar cultivo: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        return true
    }

    fun actualizarLecturasSensores() {
        val nuevos = sectores.map { sector ->
            SensorSimulado(
                sectorId = sector.id,
                humedadSuelo = if (sector.id == "1") 60.0 else 45.0,
                temperaturaAmbiental = 22.0,
                humedadAmbiental = 50.0,
                origen = "Simulación académica / Dato de prueba"
            )
        }
        _uiState.update { it.copy(sensores = nuevos) }
    }

    fun fechaProximoRiego(cultivo: Cultivo): String? = cultivosUseCase.fechaProximoRiego(cultivo)
    fun limpiarError() { _uiState.update { it.copy(errorMessage = null) } }
    fun limpiarExito() { _uiState.update { it.copy(successMessage = null) } }
}
