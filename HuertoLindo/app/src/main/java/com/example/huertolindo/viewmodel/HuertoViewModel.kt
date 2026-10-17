package com.example.huertolindo.viewmodel

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.room.withTransaction
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.UsuarioDatabaseHelper
import com.example.huertolindo.data.model.*
import com.example.huertolindo.data.security.PermisosHuerto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

class HuertoViewModel(application: Application) : AndroidViewModel(application) {
    private val usuarioDatabase = UsuarioDatabaseHelper(application)
    private val huertoDatabase = HuertoDatabaseProvider.getDatabase(application)

    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    var fotoPerfilUri by mutableStateOf<Uri?>(null)
        private set

    var listaSectores = mutableStateOf(listOf<Sector>())
        private set

    var listaCultivos = mutableStateOf(listOf<Cultivo>())
        private set

    private var listaTareas = mutableStateOf(listOf<Tarea>())

    var historialActividades = mutableStateOf(listOf<ActividadTarea>())
        private set

    var evidenciasTareas = mutableStateOf(listOf<Evidencia>())
        private set

    var comentariosEntrega = mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var reportesProblemas = mutableStateOf(listOf<ReporteProblema>())
        private set

    var cargandoReportesProblemas by mutableStateOf(false)
        private set

    var errorReportesProblemas by mutableStateOf<String?>(null)
        private set

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

    var usuariosDisponibles = mutableStateOf(listOf<Usuario>())
        private set

    var errorCargaUsuarios by mutableStateOf<String?>(null)
        private set

    init {
        cargarDatosFicticios()
        cargarEntregasGuardadas()
        cargarHistorial()
    }

    private fun cargarDatosFicticios() {
        listaSectores.value = listOf(
            Sector(id = "1", nombre = "Sector Norte", descripcion = "Zona de hortalizas de hoja", estado = "En uso"),
            Sector(id = "2", nombre = "Sector Sur", descripcion = "Zona de raíces y tubérculos", estado = "Disponible")
        )

        listaCultivos.value = listOf(
            Cultivo("1", "Lechuga", "1", "2026-09-20", "En crecimiento", "Cada 2 días"),
            Cultivo("2", "Espinaca", "1", "2026-09-25", "En crecimiento", "Cada 2 días"),
            Cultivo("3", "Zanahoria", "2", "2026-09-10", "Desarrollo", "Cada 3 días")
        )

        listaTareas.value = listOf(
            Tarea(id = "1", titulo = "Regar cultivos", descripcion = "Aplicar agua en la base de cada planta", sectorId = "1", asignadoA = "student-vicente", estado = "Pendiente", fechaProgramada = "2026-10-17"),
            Tarea(id = "2", titulo = "Revisar zanahorias", descripcion = "Verificar humedad y retirar malezas", sectorId = "2", asignadoA = "student-matias", estado = "Pendiente", fechaProgramada = "2026-10-18")
        )
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
                    guardarEvidenciaLocal(tareaId, comentario.trim(), imagenUri)
                }
            } catch (exception: IOException) {
                onResult("No se pudo guardar la fotografía. Inténtalo nuevamente.")
                return@launch
            } catch (exception: SecurityException) {
                onResult("No se pudo acceder a la fotografía seleccionada.")
                return@launch
            }

            evidenciasTareas.value = evidenciasTareas.value + evidencia
            comentariosEntrega.value = comentariosEntrega.value + (tareaId to comentario.trim())
            guardarEntregas()
            actualizarEstadoTarea(tareaId, "Por validar")
            registrarActividad(tarea, "Entregó la tarea con comentario y evidencia", "Por validar")
            onResult(null)
        }
    }

    fun evidenciasDeTarea(tareaId: String): List<Evidencia> =
        evidenciasTareas.value.filter { it.tareaId == tareaId }

    private fun guardarEvidenciaLocal(
        tareaId: String,
        comentario: String,
        imagenUri: Uri
    ): Evidencia {
        val carpeta = File(getApplication<Application>().filesDir, "evidencias")
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw IOException("No se pudo crear el directorio de evidencias.")
        }
        val archivo = File(carpeta, "${java.util.UUID.randomUUID()}.jpg")
        val input = getApplication<Application>().contentResolver.openInputStream(imagenUri)
            ?: throw IOException("No se pudo abrir la fotografía.")
        var copiaCompleta = false
        try {
            input.use { origen ->
                archivo.outputStream().use { destino -> origen.copyTo(destino) }
            }
            copiaCompleta = true
        } finally {
            if (!copiaCompleta) archivo.delete()
        }
        return Evidencia(
            id = java.util.UUID.randomUUID().toString(),
            tareaId = tareaId,
            tipo = "foto",
            rutaArchivo = archivo.absolutePath,
            descripcion = comentario,
            fechaRegistro = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT)
                .format(java.util.Date())
        )
    }

    private fun cargarEntregasGuardadas() {
        val json = getApplication<Application>()
            .getSharedPreferences("entregas_tareas", Context.MODE_PRIVATE)
            .getString("evidencias", null) ?: return
        try {
            val guardadas = JSONArray(json)
            val evidencias = mutableListOf<Evidencia>()
            val comentarios = mutableMapOf<String, String>()
            for (index in 0 until guardadas.length()) {
                val elemento = guardadas.getJSONObject(index)
                val evidencia = Evidencia(
                    id = elemento.getString("id"),
                    tareaId = elemento.getString("tareaId"),
                    tipo = elemento.getString("tipo"),
                    rutaArchivo = elemento.getString("rutaArchivo"),
                    descripcion = elemento.getString("descripcion"),
                    fechaRegistro = elemento.getString("fechaRegistro")
                )
                if (File(evidencia.rutaArchivo).isFile) {
                    evidencias.add(evidencia)
                    comentarios[evidencia.tareaId] = evidencia.descripcion
                }
            }
            evidenciasTareas.value = evidencias
            comentariosEntrega.value = comentarios
        } catch (exception: JSONException) {
            android.util.Log.e("HuertoViewModel", "No se pudieron leer las evidencias guardadas.", exception)
        }
    }

    private fun guardarEntregas() {
        val json = JSONArray()
        try {
            evidenciasTareas.value.forEach { evidencia ->
                json.put(
                    JSONObject()
                        .put("id", evidencia.id)
                        .put("tareaId", evidencia.tareaId)
                        .put("tipo", evidencia.tipo)
                        .put("rutaArchivo", evidencia.rutaArchivo)
                        .put("descripcion", evidencia.descripcion)
                        .put("fechaRegistro", evidencia.fechaRegistro)
                )
            }
            getApplication<Application>()
                .getSharedPreferences("entregas_tareas", Context.MODE_PRIVATE)
                .edit()
                .putString("evidencias", json.toString())
                .apply()
        } catch (exception: JSONException) {
            android.util.Log.e("HuertoViewModel", "No se pudieron guardar las evidencias.", exception)
        }
    }

    fun iniciarTarea(tareaId: String): Boolean {
        val usuario = usuarioActual ?: return false
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeIniciarTarea(usuario.perfil, usuario.id, tarea)) return false

        actualizarTarea(tarea.copy(estado = "En desarrollo", motivoRechazo = ""))
        registrarActividad(tarea, "Inició la tarea", "En desarrollo")
        return true
    }

    fun validarTarea(tareaId: String): Boolean {
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeValidarTarea(usuarioActual?.perfil, tarea.estado)) return false
        actualizarEstadoTarea(tareaId, "Completada")
        registrarActividad(tarea, "Validó la tarea", "Completada")
        return true
    }

    fun rechazarTarea(tareaId: String, motivo: String): Boolean {
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeRechazarTarea(usuarioActual?.perfil, tarea.estado, motivo)) return false

        actualizarTarea(
            tarea.copy(estado = "Rechazada", motivoRechazo = motivo.trim())
        )
        registrarActividad(tarea, "Rechazó la tarea: ${motivo.trim()}", "Rechazada")
        return true
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

        val tarea = Tarea(
            id = java.util.UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descripcion = descripcion.trim(),
            sectorId = sectorId,
            asignadoA = usuarioId,
            estado = "Pendiente",
            fechaProgramada = fechaProgramada.trim()
        )
        listaTareas.value = listaTareas.value + tarea
        registrarActividad(tarea, "Creó y asignó la tarea", "Pendiente", usuarioId)
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
        registrarActividad(tareaActualizada, "Editó la tarea", tareaActualizada.estado)
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
                    huertoDatabase.withTransaction {
                        huertoDatabase.sectorDao().asegurar(sector)
                        cultivo?.let { huertoDatabase.cultivoDao().asegurar(it) }
                        huertoDatabase.usuarioDao().asegurar(usuario)
                        huertoDatabase.reporteProblemaDao().insertar(reporte)
                    }
                }
                cargarReportesProblema()
                errorReportesProblemas = null
                onResult(null)
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo guardar el reporte del huerto.", exception)
                errorReportesProblemas = "No se pudo guardar el reporte en la base de datos."
                onResult(errorReportesProblemas)
            }
        }
    }

    fun cargarReportesProblema() {
        val usuario = usuarioActual ?: run {
            reportesProblemas.value = emptyList()
            return
        }
        cargandoReportesProblemas = true
        viewModelScope.launch {
            try {
                val reportes = withContext(Dispatchers.IO) {
                    if (esAdministrador) {
                        huertoDatabase.reporteProblemaDao().obtenerTodos()
                    } else {
                        huertoDatabase.reporteProblemaDao().obtenerPorUsuario(usuario.id)
                    }
                }
                reportesProblemas.value = reportes
                errorReportesProblemas = null
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudieron consultar los reportes del huerto.", exception)
                errorReportesProblemas = "No se pudieron cargar los reportes de problemas."
            } finally {
                cargandoReportesProblemas = false
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
                    huertoDatabase.reporteProblemaDao().actualizar(
                        reporte.copy(estado = nuevoEstado)
                    )
                }
                cargarReportesProblema()
                onResult(null)
            } catch (exception: SQLiteException) {
                android.util.Log.e("HuertoViewModel", "No se pudo actualizar el estado del reporte.", exception)
                errorReportesProblemas = "No se pudo actualizar el estado del reporte."
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
        listaSectores.value = listaSectores.value + Sector(
            id = java.util.UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            estado = "Disponible"
        )
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
        listaSectores.value = listaSectores.value.map {
            if (it.id == sectorId) {
                sector.copy(nombre = nombre.trim(), descripcion = descripcion.trim(), estado = estado)
            } else {
                it
            }
        }
        return true
    }

    fun eliminarSector(sectorId: String): Boolean {
        if (!esAdministrador ||
            listaCultivos.value.any { it.sectorId == sectorId } ||
            listaTareas.value.any { it.sectorId == sectorId }
        ) return false
        val sectoresActualizados = listaSectores.value.filterNot { it.id == sectorId }
        if (sectoresActualizados.size == listaSectores.value.size) return false
        listaSectores.value = sectoresActualizados
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

        listaCultivos.value = listaCultivos.value + Cultivo(
            id = java.util.UUID.randomUUID().toString(),
            nombreComun = nombre.trim(),
            sectorId = sectorId,
            fechaSiembra = fechaSiembra.trim(),
            estadoDesarrollo = "En crecimiento",
            frecuenciaRiego = frecuenciaRiego.trim()
        )
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
        listaCultivos.value = listaCultivos.value.map {
            if (it.id == cultivoId) {
                cultivo.copy(
                    nombreComun = nombre.trim(),
                    fechaSiembra = fechaSiembra.trim(),
                    estadoDesarrollo = estadoDesarrollo,
                    frecuenciaRiego = frecuenciaRiego.trim()
                )
            } else {
                it
            }
        }
        return true
    }

    fun eliminarCultivo(cultivoId: String): Boolean {
        if (!esAdministrador) return false
        val cultivosActualizados = listaCultivos.value.filterNot { it.id == cultivoId }
        if (cultivosActualizados.size == listaCultivos.value.size) return false
        listaCultivos.value = cultivosActualizados
        return true
    }

    fun cargarUsuariosDisponibles() {
        if (!esAdministrador) return
        viewModelScope.launch {
            try {
                usuariosDisponibles.value = withContext(Dispatchers.IO) {
                    usuarioDatabase.obtenerUsuarios()
                }
                errorCargaUsuarios = null
            } catch (exception: SQLiteException) {
                errorCargaUsuarios = "No se pudieron cargar los estudiantes desde la base de datos."
            }
        }
    }

    private fun actualizarEstadoTarea(tareaId: String, estado: String) {
        listaTareas.value = listaTareas.value.map { tarea ->
            if (tarea.id == tareaId) tarea.copy(estado = estado) else tarea
        }
    }

    private fun actualizarTarea(tareaActualizada: Tarea) {
        listaTareas.value = listaTareas.value.map {
            if (it.id == tareaActualizada.id) tareaActualizada else it
        }
    }

    private fun registrarActividad(
        tarea: Tarea,
        accion: String,
        estado: String,
        usuarioRelacionadoId: String = tarea.asignadoA
    ) {
        val usuario = usuarioActual ?: return
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
        historialActividades.value = (historialActividades.value + actividad)
            .sortedByDescending { it.fecha }
        guardarHistorial()
    }

    private fun cargarHistorial() {
        val json = getApplication<Application>()
            .getSharedPreferences("historial_tareas", Context.MODE_PRIVATE)
            .getString("actividades", null) ?: return
        try {
            val elementos = JSONArray(json)
            historialActividades.value = buildList {
                for (index in 0 until elementos.length()) {
                    val elemento = elementos.getJSONObject(index)
                    add(
                        ActividadTarea(
                            id = elemento.getString("id"),
                            tareaId = elemento.getString("tareaId"),
                            tituloTarea = elemento.getString("tituloTarea"),
                            usuarioRelacionadoId = elemento.getString("usuarioRelacionadoId"),
                            usuarioId = elemento.getString("usuarioId"),
                            nombreUsuario = elemento.getString("nombreUsuario"),
                            accion = elemento.getString("accion"),
                            estado = elemento.getString("estado"),
                            fecha = elemento.getString("fecha")
                        )
                    )
                }
            }.sortedByDescending { it.fecha }
        } catch (exception: JSONException) {
            android.util.Log.e(
                "HuertoViewModel",
                "No se pudo leer el historial de actividades.",
                exception
            )
        }
    }

    private fun guardarHistorial() {
        try {
            val elementos = JSONArray()
            historialActividades.value.forEach { actividad ->
                elementos.put(
                    JSONObject()
                        .put("id", actividad.id)
                        .put("tareaId", actividad.tareaId)
                        .put("tituloTarea", actividad.tituloTarea)
                        .put("usuarioRelacionadoId", actividad.usuarioRelacionadoId)
                        .put("usuarioId", actividad.usuarioId)
                        .put("nombreUsuario", actividad.nombreUsuario)
                        .put("accion", actividad.accion)
                        .put("estado", actividad.estado)
                        .put("fecha", actividad.fecha)
                )
            }
            getApplication<Application>()
                .getSharedPreferences("historial_tareas", Context.MODE_PRIVATE)
                .edit()
                .putString("actividades", elementos.toString())
                .apply()
        } catch (exception: JSONException) {
            android.util.Log.e(
                "HuertoViewModel",
                "No se pudo guardar el historial de actividades.",
                exception
            )
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
                    usuarioDatabase.autenticar(correo, password)
                }?.let { ResultadoInicioSesion.Exito(it) } ?: ResultadoInicioSesion.CredencialesInvalidas
            } catch (exception: SQLiteException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (exception: java.security.GeneralSecurityException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            }

            if (resultado is ResultadoInicioSesion.Exito) {
                usuarioActual = resultado.usuario
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
                    usuarioDatabase.registrar(nombre, apellido, correo, password)
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
        fotoPerfilUri = uri
    }

    fun cerrarSesion() {
        usuarioActual = null
        fotoPerfilUri = null
        usuariosDisponibles.value = emptyList()
        errorCargaUsuarios = null
    }

    override fun onCleared() {
        usuarioDatabase.close()
        super.onCleared()
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