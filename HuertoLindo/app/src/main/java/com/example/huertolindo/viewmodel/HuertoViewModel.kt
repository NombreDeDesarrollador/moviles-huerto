package com.example.huertolindo.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Base64
import com.example.huertolindo.data.model.*
import com.example.huertolindo.data.repository.HuertoRepository
import com.example.huertolindo.data.repository.HuertoSnapshot
import com.example.huertolindo.data.security.PermisosHuerto
import com.example.huertolindo.data.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

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

class HuertoViewModel(application: Application) : AndroidViewModel(application) {
    private val huertoRepository = HuertoRepository.create(application)

    private val _usuarioActual = MutableStateFlow<Usuario?>(null)
    val usuarioActual: Usuario? get() = _usuarioActual.value
    val usuarioActualState = _usuarioActual.asStateFlow()

    private val _fotoPerfilUri = MutableStateFlow<Uri?>(null)
    val fotoPerfilUri: Uri? get() = _fotoPerfilUri.value
    val fotoPerfilUriState = _fotoPerfilUri.asStateFlow()

    private val _listaSectores = MutableStateFlow<List<Sector>>(emptyList())
    val listaSectores = _listaSectores.asStateFlow()

    private val _listaCultivos = MutableStateFlow<List<Cultivo>>(emptyList())
    val listaCultivos = _listaCultivos.asStateFlow()

    private val _listaTareas = MutableStateFlow<List<Tarea>>(emptyList())
    private val listaTareas = _listaTareas.asStateFlow()

    private val _historialActividades = MutableStateFlow<List<ActividadTarea>>(emptyList())
    val historialActividades = _historialActividades.asStateFlow()

    private val _evidenciasTareas = MutableStateFlow<List<Evidencia>>(emptyList())
    val evidenciasTareas = _evidenciasTareas.asStateFlow()

    private val _validacionesTareas = MutableStateFlow<List<Validacion>>(emptyList())
    val validacionesTareas = _validacionesTareas.asStateFlow()

    private val _comentariosEntrega = MutableStateFlow<Map<String, String>>(emptyMap())
    val comentariosEntrega = _comentariosEntrega.asStateFlow()

    private val _reportesProblemas = MutableStateFlow<List<ReporteProblema>>(emptyList())
    val reportesProblemas = _reportesProblemas.asStateFlow()

    private val _cargandoReportesProblemas = MutableStateFlow(false)
    val cargandoReportesProblemasState = _cargandoReportesProblemas.asStateFlow()
    val cargandoReportesProblemas: Boolean get() = _cargandoReportesProblemas.value

    private val _errorReportesProblemas = MutableStateFlow<String?>(null)
    val errorReportesProblemasState = _errorReportesProblemas.asStateFlow()
    val errorReportesProblemas: String? get() = _errorReportesProblemas.value

    val esAdministrador: Boolean
        get() = PermisosHuerto.esAdministrador(usuarioActual?.perfil)

    val tareasVisibles: List<Tarea>
        get() = PermisosHuerto.tareasVisibles(
            usuarioActual?.perfil,
            usuarioActual?.id,
            listaTareas.value
        )

    val tareasAtrasadas: List<Tarea>
        get() {
            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(java.util.Date())
            return PermisosHuerto.tareasAtrasadas(tareasVisibles, hoy)
        }

    val historialVisible: List<ActividadTarea>
        get() {
            if (esAdministrador) return historialActividades.value
            val usuarioId = usuarioActual?.id ?: return emptyList()
            return historialActividades.value.filter { it.usuarioRelacionadoId == usuarioId }
        }

    private val _usuariosDisponibles = MutableStateFlow<List<Usuario>>(emptyList())
    val usuariosDisponibles = _usuariosDisponibles.asStateFlow()

    private val _errorCargaUsuarios = MutableStateFlow<String?>(null)
    val errorCargaUsuariosState = _errorCargaUsuarios.asStateFlow()
    val errorCargaUsuarios: String? get() = _errorCargaUsuarios.value

    private val _cargandoDatos = MutableStateFlow(true)
    val cargandoDatosState = _cargandoDatos.asStateFlow()
    val cargandoDatos: Boolean get() = _cargandoDatos.value

    private val _errorPersistencia = MutableStateFlow<String?>(null)
    val errorPersistenciaState = _errorPersistencia.asStateFlow()
    val errorPersistencia: String? get() = _errorPersistencia.value

    val uiState: StateFlow<HuertoUiState> = merge(
        _usuarioActual.map { Unit },
        _fotoPerfilUri.map { Unit },
        _listaSectores.map { Unit },
        _listaCultivos.map { Unit },
        _listaTareas.map { Unit },
        _historialActividades.map { Unit },
        _evidenciasTareas.map { Unit },
        _validacionesTareas.map { Unit },
        _comentariosEntrega.map { Unit },
        _reportesProblemas.map { Unit },
        _cargandoReportesProblemas.map { Unit },
        _errorReportesProblemas.map { Unit },
        _usuariosDisponibles.map { Unit },
        _errorCargaUsuarios.map { Unit },
        _cargandoDatos.map { Unit },
        _errorPersistencia.map { Unit }
    ).scan(crearUiState()) { _, _ -> crearUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, crearUiState())

    private fun crearUiState() = HuertoUiState(
        usuarioActual = usuarioActual,
        fotoPerfilUri = fotoPerfilUri,
        sectores = listaSectores.value,
        cultivos = listaCultivos.value,
        tareas = listaTareas.value,
        historial = historialActividades.value,
        evidencias = evidenciasTareas.value,
        validaciones = validacionesTareas.value,
        comentariosEntrega = comentariosEntrega.value,
        reportesProblemas = reportesProblemas.value,
        cargandoReportesProblemas = cargandoReportesProblemas,
        errorReportesProblemas = errorReportesProblemas,
        usuariosDisponibles = usuariosDisponibles.value,
        errorCargaUsuarios = errorCargaUsuarios,
        cargandoDatos = cargandoDatos,
        errorPersistencia = errorPersistencia
    )

    init {
        cargarDatosPersistentes()
    }

    private fun cargarDatosPersistentes() {
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    huertoRepository.inicializar()
                    huertoRepository.cargarSnapshot()
                }
                aplicarSnapshot(snapshot)
                _usuariosDisponibles.value = withContext(Dispatchers.IO) {
                    huertoRepository.obtenerUsuarios()
                }
                try {
                    withContext(Dispatchers.IO) {
                        huertoRepository.migrarEvidenciasLegacy(snapshot.evidencias)
                        huertoRepository.migrarHistorialLegacy(snapshot.actividades)
                    }
                    aplicarSnapshot(withContext(Dispatchers.IO) { huertoRepository.cargarSnapshot() })
                } catch (exception: SQLiteException) {
                    android.util.Log.e("HuertoViewModel", "No se pudieron migrar datos locales anteriores.", exception)
                    _errorPersistencia.value = "No se pudieron migrar algunos datos locales anteriores."
                } catch (exception: JSONException) {
                    android.util.Log.e("HuertoViewModel", "El formato de datos locales anteriores es inválido.", exception)
                    _errorPersistencia.value = "No se pudieron leer algunos datos locales anteriores."
                }
                _cargandoDatos.value = false
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudieron cargar los datos locales.", exception)
                _errorPersistencia.value = "No se pudieron cargar los datos guardados."
                _cargandoDatos.value = false
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "La base de datos del huerto no está lista.", exception)
                _errorPersistencia.value = "No se pudo inicializar la base de datos del huerto."
                _cargandoDatos.value = false
            }
        }
    }

    private fun aplicarSnapshot(snapshot: HuertoSnapshot) {
        _listaSectores.value = snapshot.sectores
        _listaCultivos.value = snapshot.cultivos
        _listaTareas.value = snapshot.tareas
        _evidenciasTareas.value = snapshot.evidencias.filter { File(it.rutaArchivo).isFile }
        _validacionesTareas.value = snapshot.validaciones
        _comentariosEntrega.value = evidenciasTareas.value
            .filter { !it.tareaId.isNullOrBlank() && it.descripcion.isNotBlank() }
            .groupBy { it.tareaId }
            .mapNotNull { (tareaId, evidencias) ->
                tareaId?.let { it to evidencias.first().descripcion }
            }
            .toMap()
        _reportesProblemas.value = snapshot.reportesProblemas
        _historialActividades.value = snapshot.actividades
    }

    private fun refrescarSnapshot() {
        viewModelScope.launch {
            try {
                aplicarSnapshot(withContext(Dispatchers.IO) { huertoRepository.cargarSnapshot() })
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo actualizar la vista desde Room.", exception)
                _errorPersistencia.value = "No se pudieron actualizar los datos guardados."
            }
        }
    }

    fun entregarTarea(
        tareaId: String,
        comentario: String,
        imagenUri: Uri?,
        onResult: (String?) -> Unit
    ) {
        val usuario = usuarioActual
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId }
        if (usuario == null || tarea == null ||
            usuario.perfil != PermisosHuerto.ESTUDIANTE ||
            tarea.asignadoA != usuario.id ||
            tarea.estado != "En desarrollo"
        ) {
            onResult("No tienes permiso para entregar esta tarea.")
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
            val evidencia = try {
                withContext(Dispatchers.IO) {
                    huertoRepository.guardarEvidenciaLocal(tareaId, comentario.trim(), imagenUri)
                }
            } catch (exception: IOException) {
                onResult("No se pudo guardar la fotografía. Inténtalo nuevamente.")
                return@launch
            } catch (exception: SecurityException) {
                onResult("No se pudo acceder a la fotografía seleccionada.")
                return@launch
            }

            _evidenciasTareas.value = evidenciasTareas.value + evidencia
            _comentariosEntrega.value = comentariosEntrega.value + (tareaId to comentario.trim())
            val tareaEntregada = tarea.copy(estado = "Por validar")
            val actividad = registrarActividad(
                tareaEntregada,
                "Entregó la tarea con comentario y evidencia",
                "Por validar"
            )
            try {
                withContext(Dispatchers.IO) {
                    huertoRepository.entregarTarea(
                        tareaEntregada,
                        evidencia,
                        usuario,
                        actividad ?: throw IllegalStateException("No se pudo registrar la entrega.")
                    )
                }
                actualizarTarea(tareaEntregada)
            } catch (exception: SQLiteException) {
                withContext(Dispatchers.IO) {
                    huertoRepository.eliminarArchivoEvidencia(evidencia.rutaArchivo)
                }
                _evidenciasTareas.value = evidenciasTareas.value.filterNot { it.id == evidencia.id }
                _comentariosEntrega.value = comentariosEntrega.value - tareaId
                actividad?.let { evento ->
                    _historialActividades.value = historialActividades.value.filterNot { it.id == evento.id }
                }
                android.util.Log.e("HuertoViewModel", "No se pudo guardar la entrega en Room.", exception)
                _errorPersistencia.value = "No se pudo guardar la entrega en la base de datos."
                onResult(errorPersistencia)
                return@launch
            }
            onResult(null)
        }
    }

    fun evidenciasDeTarea(tareaId: String): List<Evidencia> =
        evidenciasTareas.value.filter { it.tareaId == tareaId }

    fun evidenciasDeCultivo(cultivoId: String): List<Evidencia> =
        evidenciasTareas.value.filter { it.cultivoId == cultivoId }

    fun evidenciasDeReporte(reporteId: String): List<Evidencia> =
        evidenciasTareas.value.filter { it.reporteId == reporteId }

    fun agregarEvidencia(
        tipoAsociacion: TipoAsociacionEvidencia,
        entidadId: String,
        imagenUri: Uri,
        onResult: (String?) -> Unit
    ) {
        val usuario = usuarioActual
        if (usuario == null) {
            onResult("Inicia sesión para guardar fotografías.")
            return
        }
        val destinoDisponible = when (tipoAsociacion) {
            TipoAsociacionEvidencia.TAREA -> {
                val tarea = listaTareas.value.firstOrNull { it.id == entidadId }
                tarea != null && (
                    esAdministrador ||
                        (tarea.asignadoA == usuario.id &&
                            tarea.estado in setOf("En desarrollo", "Rechazada"))
                    )
            }
            TipoAsociacionEvidencia.CULTIVO ->
                listaCultivos.value.any { it.id == entidadId }
            TipoAsociacionEvidencia.REPORTE -> {
                val reporte = reportesProblemas.value.firstOrNull { it.id == entidadId }
                reporte != null && (esAdministrador || reporte.usuarioId == usuario.id)
            }
        }
        if (!destinoDisponible) {
            onResult("No tienes permiso para adjuntar fotografías a este registro.")
            return
        }

        viewModelScope.launch {
            try {
                val evidencia = withContext(Dispatchers.IO) {
                    huertoRepository.guardarEvidenciaAsociada(
                        tipoAsociacion,
                        entidadId,
                        imagenUri
                    )
                }
                _evidenciasTareas.value = evidenciasTareas.value + evidencia
                onResult(null)
            } catch (exception: IOException) {
                android.util.Log.e("HuertoViewModel", "No se pudo guardar la fotografía.", exception)
                onResult("No se pudo guardar la fotografía. Inténtalo nuevamente.")
            } catch (exception: SecurityException) {
                android.util.Log.e("HuertoViewModel", "No se pudo leer la fotografía seleccionada.", exception)
                onResult("No se pudo acceder a la fotografía seleccionada.")
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo asociar la fotografía al registro.", exception)
                onResult("No se pudo guardar la fotografía en el registro.")
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "La base local no pudo guardar la fotografía.", exception)
                onResult("No se pudo guardar la fotografía. Inténtalo nuevamente.")
            }
        }
    }

    fun eliminarEvidencia(evidenciaId: String, onResult: (String?) -> Unit) {
        val usuario = usuarioActual
        val evidencia = evidenciasTareas.value.firstOrNull { it.id == evidenciaId }
        if (usuario == null || evidencia == null) {
            onResult("La fotografía ya no está disponible.")
            return
        }
        val puedeEliminar = when {
            evidencia.tareaId != null -> {
                val tarea = listaTareas.value.firstOrNull { it.id == evidencia.tareaId }
                tarea != null && (esAdministrador || tarea.asignadoA == usuario.id)
            }
            evidencia.cultivoId != null ->
                listaCultivos.value.any { it.id == evidencia.cultivoId }
            evidencia.reporteId != null -> {
                val reporte = reportesProblemas.value.firstOrNull { it.id == evidencia.reporteId }
                reporte != null && (esAdministrador || reporte.usuarioId == usuario.id)
            }
            else -> false
        }
        if (!puedeEliminar) {
            onResult("No tienes permiso para quitar esta fotografía.")
            return
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    huertoRepository.eliminarEvidencia(evidencia)
                }
                _evidenciasTareas.value = evidenciasTareas.value.filterNot { it.id == evidenciaId }
                if (evidencia.tareaId != null) {
                    _comentariosEntrega.value = evidenciasTareas.value
                        .filter { it.tareaId == evidencia.tareaId && it.descripcion.isNotBlank() }
                        .groupBy { it.tareaId }
                        .mapNotNull { (tareaId, evidencias) ->
                            tareaId?.let { it to evidencias.first().descripcion }
                        }
                        .toMap()
                }
                onResult(null)
            } catch (exception: IOException) {
                android.util.Log.e("HuertoViewModel", "No se pudo eliminar el archivo de evidencia.", exception)
                onResult("No se pudo quitar la fotografía. Inténtalo nuevamente.")
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo eliminar la evidencia de Room.", exception)
                onResult("No se pudo quitar la fotografía de la base de datos.")
            } catch (exception: SecurityException) {
                android.util.Log.e("HuertoViewModel", "No se pudo acceder al archivo de evidencia.", exception)
                onResult("No se pudo quitar la fotografía por permisos de almacenamiento.")
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "La base local no pudo eliminar la evidencia.", exception)
                onResult("No se pudo quitar la fotografía. Inténtalo nuevamente.")
            }
        }
    }

    fun iniciarTarea(tareaId: String): Boolean {
        val usuario = usuarioActual ?: return false
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeIniciarTarea(usuario.perfil, usuario.id, tarea)) return false

        val actualizada = tarea.copy(estado = "En desarrollo", motivoRechazo = "")
        actualizarTarea(actualizada)
        val actividad = registrarActividad(actualizada, "Inició la tarea", "En desarrollo")
        actividad?.let { guardarTareaConActividad(actualizada, it) }
        return true
    }

    fun validarTarea(tareaId: String): Boolean {
        val administrador = usuarioActual ?: return false
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeValidarTarea(usuarioActual?.perfil, tarea.estado)) return false
        val actualizada = tarea.copy(estado = "Completada")
        actualizarTarea(actualizada)
        val validacion = Validacion(
            id = java.util.UUID.randomUUID().toString(),
            tareaId = tarea.id,
            administradorId = administrador.id,
            resultado = "Validada",
            fechaValidacion = fechaActualConHora()
        )
        val actividad = registrarActividad(actualizada, "Validó la tarea", "Completada")
        actividad?.let {
            persistir {
                huertoRepository.validarTarea(actualizada, validacion, it, administrador)
            }
        }
        return true
    }

    fun rechazarTarea(tareaId: String, motivo: String, onResult: (String?) -> Unit) {
        val administrador = usuarioActual
        if (administrador == null) {
            onResult("Inicia sesión como administrador para rechazar la tarea.")
            return
        }
        if (motivo.isBlank()) {
            onResult("El motivo del rechazo es obligatorio.")
            return
        }
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId }
        if (tarea == null ||
            !PermisosHuerto.puedeRechazarTarea(administrador.perfil, tarea.estado, motivo)
        ) {
            onResult("Solo se puede rechazar una tarea pendiente de validación.")
            return
        }

        val actualizada = tarea.copy(estado = "Rechazada", motivoRechazo = motivo.trim())
        actualizarTarea(actualizada)
        val validacion = Validacion(
            id = java.util.UUID.randomUUID().toString(),
            tareaId = tarea.id,
            administradorId = administrador.id,
            resultado = "Rechazada",
            observaciones = motivo.trim(),
            fechaValidacion = fechaActualConHora()
        )
        val actividad = registrarActividad(
            actualizada,
            "Rechazó la tarea: ${motivo.trim()}",
            "Rechazada"
        )
        if (actividad == null) {
            actualizarTarea(tarea)
            onResult("No se pudo registrar quién rechazó la tarea.")
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    huertoRepository.validarTarea(actualizada, validacion, actividad, administrador)
                }
                _errorPersistencia.value = null
                onResult(null)
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo guardar el rechazo de la tarea.", exception)
                _errorPersistencia.value = "No se pudo guardar el rechazo en la base de datos."
                refrescarSnapshot()
                onResult(_errorPersistencia.value)
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "No se pudo completar el rechazo de la tarea.", exception)
                _errorPersistencia.value = exception.message ?: "No se pudo guardar el rechazo."
                refrescarSnapshot()
                onResult(_errorPersistencia.value)
            }
        }
    }

    fun crearTarea(
        titulo: String,
        descripcion: String,
        sectorId: String,
        usuarioId: String,
        fechaProgramada: String
    ): Boolean {
        if (!esAdministrador ||
            titulo.isBlank() || descripcion.isBlank() ||
            !esFechaProgramadaValida(fechaProgramada) ||
            listaSectores.value.none { it.id == sectorId } ||
            usuariosDisponibles.value.none { it.id == usuarioId && it.perfil == "estudiante" }
        ) return false

        val actor = usuarioActual ?: return false
        val estudiante = usuariosDisponibles.value.firstOrNull { it.id == usuarioId } ?: return false
        val sector = listaSectores.value.firstOrNull { it.id == sectorId } ?: return false
        val tarea = Tarea(
            id = java.util.UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descripcion = descripcion.trim(),
            sectorId = sectorId,
            asignadoA = usuarioId,
            estado = "Pendiente",
            fechaProgramada = fechaProgramada.trim()
        )
        _listaTareas.value = listaTareas.value + tarea
        val actividad = registrarActividad(
            tarea,
            "Creó y asignó la tarea",
            "Pendiente",
            usuarioId
        )
        actividad?.let {
            persistir { huertoRepository.crearTarea(tarea, sector, estudiante, actor, it) }
        }
        return true
    }

    fun editarTarea(
        tareaId: String,
        titulo: String,
        descripcion: String,
        sectorId: String,
        usuarioId: String,
        fechaProgramada: String
    ): Boolean {
        if (!esAdministrador ||
            titulo.isBlank() || descripcion.isBlank() ||
            !esFechaProgramadaValida(fechaProgramada) ||
            listaSectores.value.none { it.id == sectorId } ||
            usuariosDisponibles.value.none { it.id == usuarioId && it.perfil == PermisosHuerto.ESTUDIANTE }
        ) return false

        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (tarea.estado == "Completada") return false

        val tareaActualizada = tarea.copy(
                titulo = titulo.trim(),
                descripcion = descripcion.trim(),
                sectorId = sectorId,
                asignadoA = usuarioId,
                fechaProgramada = fechaProgramada.trim(),
                estado = "Pendiente",
                motivoRechazo = ""
            )
        actualizarTarea(tareaActualizada)
            val actividad = registrarActividad(
                tareaActualizada,
                "Editó la tarea",
                tareaActualizada.estado
            )
            actividad?.let { guardarTareaConActividad(tareaActualizada, it) }
        return true
    }

    fun crearReporteProblema(
        descripcion: String,
        sectorId: String,
        cultivoId: String?,
        prioridad: String,
        onResult: (String?) -> Unit
    ) {
        val usuario = usuarioActual ?: run {
            onResult("Inicia sesión para reportar un problema.")
            return
        }
        val sector = listaSectores.value.firstOrNull { it.id == sectorId }
        val cultivo = cultivoId?.let { id -> listaCultivos.value.firstOrNull { it.id == id } }
        if (descripcion.isBlank() ||
            sector == null ||
            (cultivoId != null && (cultivo == null || cultivo.sectorId != sectorId)) ||
            prioridad !in setOf("Baja", "Media", "Alta")
        ) {
            onResult("Revisa la descripción, el sector, el cultivo y la prioridad.")
            return
        }

        val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(java.util.Date())
        val reporte = ReporteProblema(
            id = java.util.UUID.randomUUID().toString(),
            sectorId = sectorId,
            cultivoId = cultivoId,
            usuarioId = usuario.id,
            descripcion = descripcion.trim(),
            prioridad = prioridad,
            estado = "Pendiente",
            fechaReporte = fecha
        )
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    huertoRepository.insertarReporte(reporte, sector, cultivo, usuario)
                }
                cargarReportesProblema()
                _errorReportesProblemas.value = null
                onResult(null)
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo guardar el reporte del huerto.", exception)
                _errorReportesProblemas.value = "No se pudo guardar el reporte en la base de datos."
                onResult(errorReportesProblemas)
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "El reporte no cumple sus relaciones locales.", exception)
                _errorReportesProblemas.value = exception.message ?: "No se pudo guardar el reporte."
                onResult(errorReportesProblemas)
            }
        }
    }

    fun cargarReportesProblema() {
        val usuario = usuarioActual ?: run {
            _reportesProblemas.value = emptyList()
            return
        }
        _cargandoReportesProblemas.value = true
        viewModelScope.launch {
            try {
                val reportes = withContext(Dispatchers.IO) {
                    huertoRepository.obtenerReportesProblema(
                        if (esAdministrador) null else usuario.id
                    )
                }
                _reportesProblemas.value = reportes
                _errorReportesProblemas.value = null
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudieron consultar los reportes del huerto.", exception)
                _errorReportesProblemas.value = "No se pudieron cargar los reportes de problemas."
            } finally {
                _cargandoReportesProblemas.value = false
            }
        }
    }

    fun actualizarEstadoProblema(
        reporteId: String,
        nuevoEstado: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (!esAdministrador || nuevoEstado !in setOf("Pendiente", "En atención", "Resuelto")) {
            onResult("Solo el administrador puede cambiar a un estado válido.")
            return
        }
        val reporte = reportesProblemas.value.firstOrNull { it.id == reporteId }
        if (reporte == null) {
            onResult("El reporte ya no está disponible.")
            return
        }
        if (reporte.estado == nuevoEstado) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    huertoRepository.actualizarReporte(reporte.copy(estado = nuevoEstado))
                }
                cargarReportesProblema()
                onResult(null)
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo actualizar el estado del reporte.", exception)
                _errorReportesProblemas.value = "No se pudo actualizar el estado del reporte."
                onResult(errorReportesProblemas)
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "El reporte no pudo actualizarse.", exception)
                _errorReportesProblemas.value = exception.message ?: "No se pudo actualizar el reporte."
                onResult(errorReportesProblemas)
            }
        }
    }

    val reportesProblemasVisibles: List<ReporteProblema>
        get() {
            if (esAdministrador) return reportesProblemas.value
            val usuarioId = usuarioActual?.id ?: return emptyList()
            return reportesProblemas.value.filter { it.usuarioId == usuarioId }
        }

    private fun esFechaProgramadaValida(fecha: String): Boolean {
        if (!fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        return formato.parse(fecha, posicion) != null && posicion.index == fecha.length
    }

    fun agregarSector(nombre: String, descripcion: String): Boolean {
        if (!esAdministrador || nombre.isBlank() || descripcion.isBlank()) return false
        val sector = Sector(
            id = java.util.UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            estado = "Disponible"
        )
        _listaSectores.value = listaSectores.value + sector
        persistir { huertoRepository.guardarSector(sector) }
        return true
    }

    fun editarSector(
        sectorId: String,
        nombre: String,
        descripcion: String,
        estado: String
    ): Boolean {
        if (!esAdministrador || nombre.isBlank() || descripcion.isBlank() ||
            estado !in setOf("Disponible", "En uso", "Inactivo")
        ) return false
        val sector = listaSectores.value.firstOrNull { it.id == sectorId } ?: return false
        val actualizado = sector.copy(
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            estado = estado
        )
        _listaSectores.value = listaSectores.value.map {
            if (it.id == sectorId) actualizado else it
        }
        persistir { huertoRepository.actualizarSector(actualizado) }
        return true
    }

    fun eliminarSector(sectorId: String): Boolean {
        if (!esAdministrador ||
            listaCultivos.value.any { it.sectorId == sectorId } ||
            listaTareas.value.any { it.sectorId == sectorId } ||
            reportesProblemas.value.any { it.sectorId == sectorId }
        ) return false
        val sectoresActualizados = listaSectores.value.filterNot { it.id == sectorId }
        if (sectoresActualizados.size == listaSectores.value.size) return false
        val sector = listaSectores.value.first { it.id == sectorId }
        _listaSectores.value = sectoresActualizados
        persistir { huertoRepository.eliminarSector(sector) }
        return true
    }

    fun agregarCultivo(
        nombre: String,
        sectorId: String,
        fechaSiembra: String,
        frecuenciaRiego: String
    ): Boolean {
        if (!esAdministrador || nombre.isBlank() ||
            fechaSiembra.isBlank() || frecuenciaRiego.isBlank() ||
            listaSectores.value.none { it.id == sectorId }
        ) return false

        val cultivo = Cultivo(
            id = java.util.UUID.randomUUID().toString(),
            nombreComun = nombre.trim(),
            sectorId = sectorId,
            fechaSiembra = fechaSiembra.trim(),
            estadoDesarrollo = "En crecimiento",
            frecuenciaRiego = frecuenciaRiego.trim()
        )
        _listaCultivos.value = listaCultivos.value + cultivo
        val sector = listaSectores.value.first { it.id == sectorId }
        persistir { huertoRepository.guardarCultivo(cultivo, sector) }
        return true
    }

    fun editarCultivo(
        cultivoId: String,
        nombre: String,
        fechaSiembra: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String
    ): Boolean {
        if (!esAdministrador ||
            nombre.isBlank() ||
            !esFechaProgramadaValida(fechaSiembra) ||
            estadoDesarrollo !in setOf("Sembrado", "En crecimiento", "Desarrollo", "Listo para cosechar") ||
            frecuenciaRiego.isBlank()
        ) return false
        val cultivo = listaCultivos.value.firstOrNull { it.id == cultivoId } ?: return false
        val actualizado = cultivo.copy(
            nombreComun = nombre.trim(),
            fechaSiembra = fechaSiembra.trim(),
            estadoDesarrollo = estadoDesarrollo,
            frecuenciaRiego = frecuenciaRiego.trim()
        )
        _listaCultivos.value = listaCultivos.value.map {
            if (it.id == cultivoId) actualizado else it
        }
        persistir { huertoRepository.actualizarCultivo(actualizado) }
        return true
    }

    fun eliminarCultivo(cultivoId: String): Boolean {
        if (!esAdministrador || reportesProblemas.value.any { it.cultivoId == cultivoId }) return false
        val cultivosActualizados = listaCultivos.value.filterNot { it.id == cultivoId }
        if (cultivosActualizados.size == listaCultivos.value.size) return false
        val cultivo = listaCultivos.value.first { it.id == cultivoId }
        _listaCultivos.value = cultivosActualizados
        persistir { huertoRepository.eliminarCultivo(cultivo) }
        return true
    }

    fun cargarUsuariosDisponibles() {
        viewModelScope.launch {
            try {
                val usuarios = withContext(Dispatchers.IO) { huertoRepository.obtenerUsuarios() }
                _usuariosDisponibles.value = usuarios
                _errorCargaUsuarios.value = null
            } catch (exception: SQLiteException) {
                _errorCargaUsuarios.value = "No se pudieron cargar los estudiantes desde la base de datos."
                android.util.Log.e("HuertoViewModel", errorCargaUsuarios!!, exception)
            }
        }
    }

    private fun actualizarTarea(tareaActualizada: Tarea) {
        _listaTareas.value = listaTareas.value.map {
            if (it.id == tareaActualizada.id) tareaActualizada else it
        }
    }

    private fun registrarActividad(
        tarea: Tarea,
        accion: String,
        estado: String,
        usuarioRelacionadoId: String = tarea.asignadoA
    ): ActividadTarea? {
        val usuario = usuarioActual ?: return null
        val actividad = ActividadTarea(
            id = java.util.UUID.randomUUID().toString(),
            tareaId = tarea.id,
            tituloTarea = tarea.titulo,
            usuarioRelacionadoId = usuarioRelacionadoId,
            usuarioId = usuario.id,
            nombreUsuario = usuario.nombreCompleto,
            accion = accion,
            estado = estado,
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                .format(java.util.Date())
        )
        _historialActividades.value = (historialActividades.value + actividad)
            .sortedByDescending { it.fecha }
        return actividad
    }

    private fun guardarTareaConActividad(tarea: Tarea, actividad: ActividadTarea) {
        val sector = listaSectores.value.firstOrNull { it.id == tarea.sectorId } ?: run {
            _errorPersistencia.value = "No se encontró el sector asociado a la tarea."
            return
        }
        val actor = usuarioActual ?: return
        val estudiante = usuariosDisponibles.value.firstOrNull { it.id == tarea.asignadoA }
            ?: actor.takeIf { it.id == tarea.asignadoA }
            ?: run {
                _errorPersistencia.value = "No se encontró al responsable de la tarea."
                return
            }
        persistir { huertoRepository.actualizarTarea(tarea, sector, estudiante, actor, actividad) }
    }

    private fun fechaActualConHora(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(java.util.Date())

    private fun persistir(operacion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operacion() }
                _errorPersistencia.value = null
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo persistir un cambio del huerto.", exception)
                _errorPersistencia.value = "No se pudo guardar el cambio en la base de datos."
                refrescarSnapshot()
            } catch (exception: IllegalStateException) {
                android.util.Log.e("HuertoViewModel", "No se pudo completar la operación del huerto.", exception)
                _errorPersistencia.value = exception.message ?: "No se pudo guardar el cambio."
                refrescarSnapshot()
            }
        }
    }

    fun iniciarSesion(
        correo: String,
        password: String,
        onResult: (ResultadoInicioSesion) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    huertoRepository.autenticar(correo, password)
                }?.let { ResultadoInicioSesion.Exito(it) } ?: ResultadoInicioSesion.CredencialesInvalidas
            } catch (exception: SQLiteException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (exception: java.security.GeneralSecurityException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (exception: IllegalArgumentException) {
                android.util.Log.e("HuertoViewModel", "Las credenciales guardadas no tienen un formato válido.", exception)
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            }

            if (resultado is ResultadoInicioSesion.Exito) {
                _usuarioActual.value = resultado.usuario
            }
            onResult(resultado)
        }
    }

    fun registrarUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        onResult: (ResultadoRegistro) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    val salt = PasswordHasher.createSalt()
                    huertoRepository.registrarUsuario(
                        Usuario(
                            id = java.util.UUID.randomUUID().toString(),
                            nombre = nombre.trim(),
                            correo = correo.trim().lowercase(Locale.ROOT),
                            perfil = "estudiante",
                            tallerAsignado = "General",
                            apellido = apellido.trim(),
                            passwordSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                            passwordHash = Base64.encodeToString(
                                PasswordHasher.hash(password, salt),
                                Base64.NO_WRAP
                            )
                        )
                    )
                }
                ResultadoRegistro.Exito
            } catch (exception: SQLiteConstraintException) {
                ResultadoRegistro.CorreoYaRegistrado
            } catch (exception: SQLiteException) {
                ResultadoRegistro.ErrorDeBaseDeDatos
            } catch (exception: java.security.GeneralSecurityException) {
                ResultadoRegistro.ErrorDeBaseDeDatos
            }
            onResult(resultado)
        }
    }

    fun actualizarFotoPerfil(uri: Uri?) {
        _fotoPerfilUri.value = uri
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        _fotoPerfilUri.value = null
        _usuariosDisponibles.value = emptyList()
        _errorCargaUsuarios.value = null
    }

    sealed interface ResultadoInicioSesion {
        data class Exito(val usuario: Usuario) : ResultadoInicioSesion
        data object CredencialesInvalidas : ResultadoInicioSesion
        data object ErrorDeBaseDeDatos : ResultadoInicioSesion
    }

    sealed interface ResultadoRegistro {
        data object Exito : ResultadoRegistro
        data object CorreoYaRegistrado : ResultadoRegistro
        data object ErrorDeBaseDeDatos : ResultadoRegistro
    }
}