package com.example.huertolindo.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Base64
import androidx.room.withTransaction
import com.example.huertolindo.data.local.HuertoDatabase
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.data.local.mapper.toDomain
import com.example.huertolindo.data.local.mapper.toEntity
import com.example.huertolindo.data.remote.HuertoApiService
import com.example.huertolindo.data.remote.LoginRequestDto
import com.example.huertolindo.data.remote.BackendRegistroRequestDto
import com.example.huertolindo.data.remote.BackendUsuarioRequestDto
import com.example.huertolindo.data.remote.BackendActividadRequestDto
import com.example.huertolindo.data.remote.BackendCultivoDto
import com.example.huertolindo.data.remote.BackendSectorDto
import com.example.huertolindo.data.remote.BackendTareaDto
import com.example.huertolindo.data.remote.toDomain as backendCultivoToDomain
import com.example.huertolindo.data.remote.toDomain as backendSectorToDomain
import com.example.huertolindo.data.remote.toDomain as backendUsuarioToDomain
import com.example.huertolindo.data.remote.toDomain as backendTareaToDomain
import com.example.huertolindo.data.remote.toBackendRequest as sectorToBackendRequest
import com.example.huertolindo.data.remote.toBackendRequest as cultivoToBackendRequest
import com.example.huertolindo.data.remote.toBackendRequest as tareaToBackendRequest
import com.example.huertolindo.data.remote.toBackendRequest as validacionToBackendRequest
import com.example.huertolindo.data.remote.toBackendRequest as reporteToBackendRequest
import com.example.huertolindo.data.remote.toCultivoEstadoBackend
import com.example.huertolindo.data.remote.toDomain
import com.example.huertolindo.data.remote.toDto
import com.example.huertolindo.data.remote.toTareaEstadoBackend
import com.example.huertolindo.data.remote.toDomain as backendActividadToDomain
import com.example.huertolindo.data.remote.toDomain as backendValidacionToDomain
import com.example.huertolindo.data.remote.toDomain as backendEvidenciaToDomain
import com.example.huertolindo.data.remote.toDomain as backendReporteToDomain
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.TipoAsociacionEvidencia
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Validacion
import com.example.huertolindo.domain.exception.CorreoYaRegistradoException
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PermisosHuerto
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApiHuertoRepository(
    context: Context,
    private val apiService: HuertoApiService? = null
) : HuertoRepository {
    private val context = context.applicationContext
    private val database: HuertoDatabase = HuertoDatabaseProvider.getDatabase(context)
    private val migrationMutex = Mutex()
    @Volatile
    private var migrationWarning: String? = null

    override suspend fun inicializar() {
        val usuario = SessionManager(context).obtenerUsuarioSesion()
        if (PermisosHuerto.esAdministrador(usuario?.perfil)) intentarMigracionRoom()
    }

    private suspend fun intentarMigracionRoom() {
        try {
            migrarDatosRoomSiNecesario()
            migrationWarning = null
        } catch (exception: IOException) {
            migrationWarning = exception.message
                ?: "La migración local quedó pendiente; los datos anteriores se conservaron."
            android.util.Log.w("ApiHuertoRepository", migrationWarning, exception)
        } catch (exception: IllegalStateException) {
            migrationWarning = exception.message
                ?: "El backend no confirmó la migración local; los datos anteriores se conservaron."
            android.util.Log.w("ApiHuertoRepository", migrationWarning, exception)
        } catch (exception: SQLiteException) {
            migrationWarning = "No se pudo leer la base Room; los datos locales se conservaron."
            android.util.Log.e("ApiHuertoRepository", migrationWarning, exception)
        }
    }

    private suspend fun migrarDatosRoomSiNecesario() = migrationMutex.withLock {
        if (database.migracionDao().estaCompletada() == true) return@withLock
        val api = apiService ?: throw IOException("El backend no está configurado para migrar los datos locales.")

        val usuarios = database.usuarioDao().obtenerTodos().filterNot(::esUsuarioDemo)
        val sectores = database.sectorDao().obtenerTodos().map { it.toDomain() }.filterNot(::esSectorDemo)
        val cultivos = database.cultivoDao().obtenerTodos().map { it.toDomain() }.filterNot(::esCultivoDemo)
        val tareas = database.tareaDao().obtenerTodos().map { it.toDomain() }.filterNot(::esTareaDemo)
        val reportes = database.reporteProblemaDao().obtenerTodos()
        val evidencias = database.evidenciaDao().obtenerTodos()
        val validaciones = database.validacionDao().obtenerTodos()
        val actividades = database.actividadTareaDao().obtenerTodos()

        if (reportes.isNotEmpty() || evidencias.isNotEmpty() ||
            validaciones.isNotEmpty() || actividades.isNotEmpty()
        ) {
            throw IOException(
                "La migración se detuvo sin borrar datos: el backend actual no permite importar " +
                    "reportes, evidencias, validaciones ni historial de actividades."
            )
        }
        if (sectores.any {
                it.estado != "En uso" ||
                    it.fechaInicioMantenimiento.isNotBlank() ||
                    it.fechaTerminoMantenimiento.isNotBlank()
            } || cultivos.any {
                it.frecuenciaRiego != "7" ||
                    it.fechaCosechaEstimada.isNotBlank() ||
                    it.fechaCambioEstado.isNotBlank()
            } || tareas.any { it.motivoRechazo.isNotBlank() } ||
            usuarios.any { it.tallerAsignado != "General" }
        ) {
            throw IOException(
                "La migración se detuvo porque algunos campos locales no existen en el contrato " +
                    "actual del backend; los registros Room se conservaron."
            )
        }
        if (cultivos.any {
                it.estadoDesarrollo !in setOf(
                    "Sembrado", "En crecimiento", "Desarrollo",
                    "Listo para cosechar", "Cosechado", "Con problemas"
                )
            } || tareas.any { it.tipo !in setOf("Riego", "Poda", "Cosecha", "Mantenimiento") }
        ) {
            throw IOException(
                "La migración se detuvo porque el backend no conserva el tipo de algunas tareas; " +
                    "los registros Room se conservaron."
            )
        }
        val usuariosLocalesIds = usuarios.mapTo(mutableSetOf()) { it.id }
        val cultivosLocalesPorId = cultivos.associateBy { it.id }
        if (tareas.any { tarea ->
                val cultivo = tarea.cultivoId?.let(cultivosLocalesPorId::get)
                tarea.asignadoA !in usuariosLocalesIds ||
                    cultivo == null ||
                    cultivo.sectorId != tarea.sectorId
            }
        ) {
            throw IOException(
                "La migración se detuvo porque hay tareas con relaciones locales incompletas; " +
                    "los registros Room se conservaron."
            )
        }

        if (usuarios.isEmpty() && sectores.isEmpty() && cultivos.isEmpty() && tareas.isEmpty()) {
            limpiarDatosRoomMigrados()
            return@withLock
        }

        val usuariosRemotos = api.obtenerUsuariosBackend().requireBody("confirmar usuarios")
        val sectoresRemotos = api.obtenerSectoresBackend().requireBody("confirmar sectores").toMutableList()
        val cultivosRemotos = api.obtenerCultivosBackend().requireBody("confirmar cultivos").toMutableList()
        val tareasRemotas = api.obtenerTareasBackend().requireBody("confirmar tareas").toMutableList()
        val usuariosPorId = usuarios.associate { local ->
            val remoto = usuariosRemotos.firstOrNull {
                it.correo.trim().equals(local.correo.trim(), ignoreCase = true)
            } ?: throw IOException(
                "No se puede migrar la cuenta ${local.correo}: el servidor no la contiene y Room " +
                    "solo conserva su hash de contraseña."
            )
            if (remoto.nombre != local.nombreCompleto ||
                PermisosHuerto.esAdministrador(remoto.rol) !=
                PermisosHuerto.esAdministrador(local.perfil)
            ) {
                throw IOException(
                    "La cuenta local ${local.correo} no coincide con la cuenta del backend; " +
                        "no se sobrescribió ninguno de los datos."
                )
            }
            local.id to remoto.id.toString()
        }

        val sectoresPorId = mutableMapOf<String, String>()
        sectores.forEach { sector ->
            val idRemoto = sector.id.toLongOrNull()
            val conflictoId = sectoresRemotos.firstOrNull { it.id == idRemoto }
            if (conflictoId != null &&
                (conflictoId.nombre != sector.nombre ||
                    conflictoId.descripcion.orEmpty() != sector.descripcion ||
                    conflictoId.ubicacion.orEmpty() != sector.ubicacion)
            ) {
                throw IOException("El sector local ${sector.id} tiene un ID ocupado por otro registro remoto.")
            }
            val remoto = sectoresRemotos.firstOrNull {
                it.nombre.trim().equals(sector.nombre.trim(), ignoreCase = true) &&
                    it.descripcion.orEmpty() == sector.descripcion &&
                    it.ubicacion.orEmpty() == sector.ubicacion
            } ?: api.guardarSectorBackend(sector.sectorToBackendRequest())
                .requireBody("migrar sector").also(sectoresRemotos::add)
            sectoresPorId[sector.id] = remoto.id.toString()
        }

        val cultivosPorId = mutableMapOf<String, String>()
        cultivos.forEach { cultivo ->
            val sectorId = sectoresPorId[cultivo.sectorId]
                ?: sectoresRemotos.firstOrNull { it.id.toString() == cultivo.sectorId }?.id?.toString()
                ?: throw IOException("No se pudo asociar el cultivo ${cultivo.nombreComun} a un sector remoto.")
            val idRemoto = cultivo.id.toLongOrNull()
            val conflictoId = cultivosRemotos.firstOrNull { it.id == idRemoto }
            if (conflictoId != null &&
                (conflictoId.nombre != cultivo.nombreComun ||
                    conflictoId.fechaSiembra.orEmpty() != cultivo.fechaSiembra ||
                    conflictoId.sectorId.toString() != sectorId ||
                    conflictoId.estado != cultivo.copy(sectorId = sectorId).cultivoToBackendRequest().estado)
            ) {
                throw IOException("El cultivo local ${cultivo.id} tiene un ID ocupado por otro registro remoto.")
            }
            val remoto = cultivosRemotos.firstOrNull {
                it.sectorId.toString() == sectorId &&
                    it.nombre.trim().equals(cultivo.nombreComun.trim(), ignoreCase = true) &&
                    it.fechaSiembra.orEmpty() == cultivo.fechaSiembra &&
                    it.estado == cultivo.copy(sectorId = sectorId).cultivoToBackendRequest().estado
            } ?: api.guardarCultivoBackend(
                cultivo.copy(sectorId = sectorId).cultivoToBackendRequest()
            ).requireBody("migrar cultivo").also(cultivosRemotos::add)
            cultivosPorId[cultivo.id] = remoto.id.toString()
        }

        tareas.forEach { tarea ->
            val usuarioId = usuariosPorId[tarea.asignadoA]
                ?: throw IOException("No se pudo asociar la tarea ${tarea.titulo} a un usuario remoto.")
            val cultivoId = tarea.cultivoId?.let { cultivosPorId[it] }
                ?: throw IOException("La tarea ${tarea.titulo} no tiene un cultivo remoto asociado.")
            val request = tarea.copy(
                asignadoA = usuarioId,
                cultivoId = cultivoId
            ).tareaToBackendRequest()
            val existente = tareasRemotas.firstOrNull {
                it.titulo == tarea.titulo &&
                    it.descripcion.orEmpty() == tarea.descripcion &&
                    it.responsableId.toString() == usuarioId &&
                    it.cultivoId.toString() == cultivoId &&
                    it.tipo == request.tipo &&
                    it.fechaProgramada == request.fechaProgramada &&
                    (it.estado?.uppercase(Locale.ROOT) ?: if (it.completada) "COMPLETADA" else "PENDIENTE") ==
                    request.estado
            }
            val conflictoId = tarea.id.toLongOrNull()?.let { id ->
                tareasRemotas.firstOrNull { it.id == id }
            }
            if (conflictoId != null &&
                (conflictoId.titulo != tarea.titulo ||
                    conflictoId.descripcion.orEmpty() != tarea.descripcion ||
                    conflictoId.responsableId.toString() != usuarioId ||
                    conflictoId.cultivoId.toString() != cultivoId ||
                    conflictoId.tipo != request.tipo ||
                    conflictoId.fechaProgramada != request.fechaProgramada ||
                    (conflictoId.estado?.uppercase(Locale.ROOT)
                        ?: if (conflictoId.completada) "COMPLETADA" else "PENDIENTE") != request.estado)
            ) {
                throw IOException("La tarea local ${tarea.id} tiene un ID ocupado por otro registro remoto.")
            }
            if (existente == null) {
                val creada = api.guardarTareaBackend(
                    request
                ).requireBody("migrar tarea")
                tareasRemotas.add(creada)
            }
        }

        val sectoresConfirmados = api.obtenerSectoresBackend().requireBody("verificar sectores")
        val cultivosConfirmados = api.obtenerCultivosBackend().requireBody("verificar cultivos")
        val tareasConfirmadas = api.obtenerTareasBackend().requireBody("verificar tareas")
        val todoConfirmado = sectores.all { local ->
            sectoresConfirmados.any {
                it.nombre.trim().equals(local.nombre.trim(), ignoreCase = true) &&
                    it.descripcion.orEmpty() == local.descripcion &&
                    it.ubicacion.orEmpty() == local.ubicacion
            }
        } && cultivos.all { local ->
            val sectorId = sectoresPorId[local.sectorId]
            cultivosConfirmados.any {
                it.nombre.trim().equals(local.nombreComun.trim(), ignoreCase = true) &&
                    it.sectorId.toString() == sectorId &&
                    it.fechaSiembra.orEmpty() == local.fechaSiembra &&
                    it.estado == local.copy(sectorId = sectorId.orEmpty()).cultivoToBackendRequest().estado
            }
        } && tareas.all { local ->
            val userId = usuariosPorId[local.asignadoA]
            val cropId = local.cultivoId?.let { cultivosPorId[it] }
            tareasConfirmadas.any {
                it.titulo == local.titulo &&
                    it.descripcion.orEmpty() == local.descripcion &&
                    it.responsableId.toString() == userId &&
                    it.cultivoId.toString() == cropId &&
                    it.tipo == local.copy(
                        asignadoA = userId.orEmpty(),
                        cultivoId = cropId
                    ).tareaToBackendRequest().tipo &&
                    it.fechaProgramada == local.fechaProgramada &&
                    (it.estado?.uppercase(Locale.ROOT) ?: if (it.completada) "COMPLETADA" else "PENDIENTE") ==
                    local.toTareaEstadoBackend()
            }
        }
        if (!todoConfirmado) {
            throw IOException("El backend no confirmó todos los registros locales; Room se conserva para reintentar.")
        }
        limpiarDatosRoomMigrados()
    }

    private suspend fun limpiarDatosRoomMigrados() {
        database.withTransaction {
            database.evidenciaDao().eliminarTodos()
            database.validacionDao().eliminarTodos()
            database.actividadTareaDao().eliminarTodos()
            database.reporteProblemaDao().eliminarTodos()
            database.tareaDao().eliminarTodos()
            database.cultivoDao().eliminarTodos()
            database.sectorDao().eliminarTodos()
            database.usuarioDao().eliminarTodos()
            database.migracionDao().marcarCompletada()
        }
    }

    private fun esUsuarioDemo(usuario: com.example.huertolindo.data.local.entity.UsuarioEntity): Boolean =
        usuario.id in DEMO_USER_IDS ||
            usuario.correo.endsWith(".test", ignoreCase = true) ||
            usuario.correo.lowercase(Locale.ROOT) in DEMO_EMAILS

    private fun esSectorDemo(sector: Sector): Boolean =
        sector.nombre in DEMO_SECTOR_NAMES && sector.id in setOf("1", "2")

    private fun esCultivoDemo(cultivo: Cultivo): Boolean =
        cultivo.id in setOf("1", "2", "3") &&
            cultivo.nombreComun in DEMO_CROP_NAMES

    private fun esTareaDemo(tarea: Tarea): Boolean =
        tarea.id in setOf("1", "2") &&
            tarea.titulo in DEMO_TASK_NAMES &&
            tarea.asignadoA in DEMO_USER_IDS

    private fun <T> Response<T>.requireBody(endpoint: String): T {
        if (!isSuccessful) {
            throw IllegalStateException("El backend rechazó $endpoint (HTTP ${code()}).")
        }
        return body() ?: throw IllegalStateException("El backend no devolvió datos al $endpoint.")
    }

    private fun <T> Response<T>.requireRegistrationBody(endpoint: String): T {
        if (code() == 409) throw CorreoYaRegistradoException()
        return requireBody(endpoint)
    }

    private fun Response<*>.requireSuccessful(endpoint: String) {
        if (!isSuccessful) {
            throw IllegalStateException("El backend rechazó $endpoint (HTTP ${code()}).")
        }
    }

    override suspend fun obtenerUsuarios(): List<Usuario> {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        return api.obtenerUsuariosBackend().requireBody("usuarios")
            .map { it.backendUsuarioToDomain() }
    }

    override suspend fun autenticar(correo: String, password: String): Usuario? {
        val normalizado = correo.trim().lowercase(Locale.ROOT)
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val response = api.login(LoginRequestDto(normalizado, password))
        if (response.code() in 400..499) return null
        val respuesta = response.requireBody("iniciar sesión")
        if (!respuesta.usuario.activo) return null
        if (PermisosHuerto.esAdministrador(respuesta.usuario.rol)) intentarMigracionRoom()
        return respuesta.usuario.backendUsuarioToDomain()
    }

    override suspend fun registrarUsuario(usuario: Usuario, password: String) {
        val api = apiService
            ?: throw IOException("El backend no está configurado para registrar cuentas.")
        val remoteUser = api.registrarEnBackend(
            BackendRegistroRequestDto(
                nombre = usuario.nombreCompleto,
                correo = usuario.correo,
                password = password
            )
        ).requireRegistrationBody("registrar cuenta")
        check(remoteUser.activo) { "El backend no activó la cuenta registrada." }
    }

    override suspend fun registrarAdministrador(
        usuario: Usuario,
        administradorId: String,
        password: String
    ): Boolean {
        if (!PermisosHuerto.esAdministrador(usuario.perfil)) {
            return false
        }

        val api = apiService
            ?: throw IOException("El backend no está configurado para registrar administradores.")
        val remoteUser = api.registrar(
            BackendUsuarioRequestDto(
                nombre = usuario.nombreCompleto,
                correo = usuario.correo,
                password = password,
                rol = "ADMIN",
                activo = true
            )
        ).requireRegistrationBody("registrar administrador")
        check(remoteUser.activo) { "El backend no activó la cuenta registrada." }
        return true
    }

    override suspend fun cargarSnapshot(): HuertoSnapshot = cargarSnapshotRemoto(null)

    override suspend fun cargarSnapshot(usuarioActual: Usuario): HuertoSnapshot =
        cargarSnapshotRemoto(usuarioActual)

    private suspend fun cargarSnapshotRemoto(usuarioActual: Usuario?): HuertoSnapshot {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val sectoresDto = api.obtenerSectoresBackend().requireBody("sectores")
        val cultivosDto = api.obtenerCultivosBackend().requireBody("cultivos")
        val sectores = sectoresDto.map { it.backendSectorToDomain() }
        val cultivos = cultivosDto.map { it.backendCultivoToDomain() }
        val cultivosPorId = cultivos.associateBy { it.id }
        val tareasDto = api.obtenerTareasBackend().requireBody("tareas")
        val tareasRemotas = tareasDto.map { tareaDto ->
            val cultivo = cultivosPorId[tareaDto.cultivoId.toString()]
                ?: throw IllegalStateException(
                    "La tarea ${tareaDto.id} referencia un cultivo que no devolvió la API."
                )
            tareaDto.backendTareaToDomain(cultivo)
        }
        val esAdministrador = PermisosHuerto.esAdministrador(usuarioActual?.perfil)
        val reportes = api.obtenerReportesBackend().requireBody("reportes")
            .filter { esAdministrador || it.usuarioId.toString() == usuarioActual?.id }
            .map { dto ->
                val cultivo = cultivosPorId[dto.cultivoId.toString()]
                    ?: throw IllegalStateException(
                        "El reporte ${dto.id} referencia un cultivo que no devolvió la API."
                    )
                dto.backendReporteToDomain(cultivo)
            }
        val evidenciasRemotas = if (esAdministrador) {
            api.obtenerEvidenciasBackend().requireBody("evidencias")
        } else {
            val porTarea = tareasRemotas.flatMap { tarea ->
                api.obtenerEvidenciasBackend(tarea.id.toLongOrNull())
                    .requireBody("evidencias de la tarea ${tarea.id}")
            }
            val porCultivo = tareasRemotas.mapNotNull { it.cultivoId?.toLongOrNull() }
                .distinct()
                .flatMap { cultivoId ->
                    api.obtenerEvidenciasBackend(cultivoId = cultivoId)
                        .requireBody("evidencias del cultivo $cultivoId")
                }
            val porReporte = reportes.flatMap { reporte ->
                api.obtenerEvidenciasBackend(reporteId = reporte.id.toLongOrNull())
                    .requireBody("evidencias del reporte ${reporte.id}")
            }
            (porTarea + porCultivo + porReporte).distinctBy { it.id }
        }
        val evidencias = evidenciasRemotas.map { it.backendEvidenciaToDomain() }
        val validaciones = if (esAdministrador) {
            val evidenciaPorId = evidencias.associateBy { it.id }
            api.obtenerValidacionesBackend()
                .requireBody("validaciones")
                .map { dto ->
                    val tareaId = dto.tareaId?.toString()
                        ?: dto.evidenciaId?.toString()?.let(evidenciaPorId::get)?.tareaId
                        ?: throw IllegalStateException(
                            "La validación ${dto.id} no tiene una tarea asociada."
                        )
                    dto.backendValidacionToDomain(tareaId)
                }
        } else {
            tareasRemotas.flatMap { tarea ->
                api.obtenerValidacionesBackend(tarea.id.toLongOrNull())
                    .requireBody("validaciones de la tarea ${tarea.id}")
                    .map { dto -> dto.backendValidacionToDomain(tarea.id) }
            }
        }
        val actividades = tareasRemotas.flatMap { tarea ->
            api.obtenerActividadesTarea(tarea.id)
                .requireBody("historial de la tarea ${tarea.id}")
                .map { it.backendActividadToDomain(tarea) }
        }
        return HuertoSnapshot(
            sectores = sectores,
            cultivos = cultivos,
            tareas = tareasRemotas,
            evidencias = evidencias,
            validaciones = validaciones,
            reportesProblemas = reportes,
            actividades = actividades,
            advertencia = migrationWarning
        )
    }

    override suspend fun guardarSector(sector: Sector): Sector {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        validarCamposSectorRemoto(sector)
        return api.guardarSectorBackend(sector.sectorToBackendRequest())
            .requireBody("crear sector")
            .backendSectorToDomain()
            .copy(
                estado = sector.estado,
                fechaInicioMantenimiento = sector.fechaInicioMantenimiento,
                fechaTerminoMantenimiento = sector.fechaTerminoMantenimiento
            )
    }

    override suspend fun actualizarSector(sector: Sector) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        validarCamposSectorRemoto(sector)
        api.actualizarSectorBackend(sector.id, sector.sectorToBackendRequest())
            .requireBody("actualizar sector")
    }

    override suspend fun eliminarSector(sector: Sector) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        api.eliminarSector(sector.id).requireSuccessful("eliminar sector")
    }

    override suspend fun guardarCultivo(cultivo: Cultivo, sector: Sector): Cultivo {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        validarCamposCultivoRemoto(cultivo)
        val request = cultivo.copy(sectorId = sector.id).cultivoToBackendRequest()
        val response = api.guardarCultivoBackend(request)
            .requireBody("crear cultivo").backendCultivoToDomain()
        return response.copy(
            fechaCosechaEstimada = cultivo.fechaCosechaEstimada,
            frecuenciaRiego = cultivo.frecuenciaRiego,
            fechaCambioEstado = cultivo.fechaCambioEstado
        )
    }

    override suspend fun actualizarCultivo(cultivo: Cultivo) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        validarCamposCultivoRemoto(cultivo)
        api.actualizarCultivoBackend(
            cultivo.id,
            cultivo.cultivoToBackendRequest()
        ).requireBody("actualizar cultivo")
    }

    override suspend fun eliminarCultivo(cultivo: Cultivo) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        api.eliminarCultivo(cultivo.id).requireSuccessful("eliminar cultivo")
    }

    private fun validarCamposSectorRemoto(sector: Sector) {
        if (sector.estado != "En uso" ||
            sector.fechaInicioMantenimiento.isNotBlank() ||
            sector.fechaTerminoMantenimiento.isNotBlank()
        ) {
            throw IOException("El backend actual no permite guardar el estado ni las fechas de mantenimiento del sector.")
        }
    }

    private fun validarCamposCultivoRemoto(cultivo: Cultivo) {
        if (cultivo.frecuenciaRiego != "7" ||
            cultivo.fechaCosechaEstimada.isNotBlank() ||
            cultivo.fechaCambioEstado.isNotBlank()
        ) {
            throw IOException("El backend actual no permite guardar la frecuencia de riego ni las fechas auxiliares del cultivo.")
        }
    }

    override suspend fun guardarEvidencia(evidencia: Evidencia) {
        subirEvidenciaRemota(evidencia)
    }

    override suspend fun guardarEvidenciaLocal(
        tareaId: String,
        comentario: String,
        imagenUri: String
    ): Evidencia = copiarEvidenciaLocal(
        imagenUri = Uri.parse(imagenUri),
        descripcion = comentario,
        tareaId = tareaId
    )

    override suspend fun guardarEvidenciaAsociada(
        tipoAsociacion: TipoAsociacionEvidencia,
        entidadId: String,
        imagenUri: String
    ): Evidencia {
        val evidencia = copiarEvidenciaLocal(
            imagenUri = Uri.parse(imagenUri),
            tareaId = if (tipoAsociacion == TipoAsociacionEvidencia.TAREA) entidadId else null,
            cultivoId = if (tipoAsociacion == TipoAsociacionEvidencia.CULTIVO) entidadId else null,
            reporteId = if (tipoAsociacion == TipoAsociacionEvidencia.REPORTE) entidadId else null
        )
        return subirEvidenciaRemota(evidencia)
    }

    private suspend fun subirEvidenciaRemota(evidencia: Evidencia): Evidencia {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val archivo = File(evidencia.rutaArchivo)
        if (!archivo.isFile) throw IOException("No se encontró el archivo de evidencia.")
        val associationCount = listOf(
            evidencia.tareaId,
            evidencia.cultivoId,
            evidencia.reporteId
        ).count { it != null }
        if (associationCount != 1) {
            throw IOException("La evidencia debe estar asociada a una sola tarea, cultivo o reporte.")
        }
        val part = MultipartBody.Part.createFormData(
            "file",
            archivo.name,
            archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        val response = api.subirEvidencia(
            part,
            evidencia.tareaId?.toRequestBody("text/plain".toMediaTypeOrNull()),
            evidencia.cultivoId?.toRequestBody("text/plain".toMediaTypeOrNull()),
            evidencia.reporteId?.toRequestBody("text/plain".toMediaTypeOrNull()),
            evidencia.descripcion.toRequestBody("text/plain".toMediaTypeOrNull()),
        ).requireBody("subir evidencia")
        if (!archivo.delete()) throw IOException("La evidencia se subió, pero no se pudo limpiar el archivo temporal.")
        return response.backendEvidenciaToDomain()
    }

    private fun copiarEvidenciaLocal(
        imagenUri: Uri,
        descripcion: String = "",
        tareaId: String? = null,
        cultivoId: String? = null,
        reporteId: String? = null
    ): Evidencia {
        val bitmap = decodificarFotografia(imagenUri)
        val carpeta = File(context.filesDir, "evidencias")
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            bitmap.recycle()
            throw IOException("No se pudo crear el directorio de evidencias.")
        }

        val archivo = File(carpeta, "${UUID.randomUUID()}.jpg")
        var copiaCompleta = false
        try {
            archivo.outputStream().use { destino ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, destino)) {
                    throw IOException("No se pudo procesar la fotografía.")
                }
            }
            copiaCompleta = true
        } finally {
            bitmap.recycle()
            if (!copiaCompleta) archivo.delete()
        }
        return Evidencia(
            id = UUID.randomUUID().toString(),
            tareaId = tareaId,
            cultivoId = cultivoId,
            reporteId = reporteId,
            tipo = "foto",
            rutaArchivo = archivo.absolutePath,
            descripcion = descripcion,
            fechaRegistro = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date())
        )
    }

    private fun decodificarFotografia(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val width = info.size.width
                    val height = info.size.height
                    if (width <= 0 || height <= 0) {
                        throw IOException("La fotografía seleccionada no tiene un tamaño válido.")
                    }
                    val mayorLado = maxOf(width, height)
                    if (mayorLado > 1600) {
                        val escala = 1600f / mayorLado
                        decoder.setTargetSize(
                            (width * escala).roundToInt().coerceAtLeast(1),
                            (height * escala).roundToInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (exception: ImageDecoder.DecodeException) {
                throw IOException("No se pudo analizar la fotografía seleccionada.", exception)
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IOException("No se pudo analizar la fotografía seleccionada.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("La fotografía seleccionada no tiene un formato compatible.")
        }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1600) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("No se pudo analizar la fotografía seleccionada.")
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            throw IOException("La fotografía seleccionada no tiene un tamaño válido.")
        }
        return bitmap
    }

    override suspend fun eliminarEvidencia(evidencia: Evidencia) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        api.eliminarEvidencia(evidencia.id).requireSuccessful("eliminar evidencia")
    }

    override fun eliminarArchivoEvidencia(rutaArchivo: String): Boolean = File(rutaArchivo).delete()

    override suspend fun crearTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    ): Pair<Tarea, ActividadTarea> {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val response = api.guardarTareaBackend(tarea.tareaToBackendRequest())
            .requireBody("crear tarea")
        val guardada = response.backendTareaToDomain(
            Cultivo(
                id = response.cultivoId.toString(),
                nombreComun = response.cultivoNombre,
                sectorId = sector.id,
                fechaSiembra = "",
                estadoDesarrollo = "Sembrado",
                frecuenciaRiego = "7"
            )
        )
        val actividadGuardada = actividad.copy(tareaId = guardada.id)
        api.agregarActividadTarea(
            guardada.id,
            BackendActividadRequestDto(actividad.accion)
        ).requireBody("guardar historial de la tarea")
        return guardada to actividadGuardada
    }

    override suspend fun actualizarTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    ) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        if (actor.perfil == PermisosHuerto.ESTUDIANTE) {
            api.actualizarEstadoTarea(
                tarea.id,
                mapOf("estado" to tarea.toTareaEstadoBackend())
            ).requireBody("actualizar estado de tarea")
        } else {
            api.actualizarTareaBackend(tarea.id, tarea.tareaToBackendRequest())
                .requireBody("actualizar tarea")
        }
        api.agregarActividadTarea(
            tarea.id,
            BackendActividadRequestDto(actividad.accion)
        ).requireBody("guardar historial de la tarea")
    }

    override suspend fun entregarTarea(
        tarea: Tarea,
        evidencia: Evidencia,
        estudiante: Usuario,
        actividad: ActividadTarea
    ): Evidencia {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val evidenciaRemota = subirEvidenciaRemota(evidencia)
        api.actualizarEstadoTarea(
            tarea.id,
            mapOf("estado" to tarea.toTareaEstadoBackend())
        ).requireBody("entregar tarea")
        api.agregarActividadTarea(
            tarea.id,
            BackendActividadRequestDto(actividad.accion)
        ).requireBody("guardar historial de la tarea")
        return evidenciaRemota
    }

    override suspend fun validarTarea(
        tarea: Tarea,
        validacion: Validacion,
        actividad: ActividadTarea,
        administrador: Usuario
    ) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        api.guardarValidacionBackend(validacion.validacionToBackendRequest())
            .requireBody("guardar validación")
        api.agregarActividadTarea(
            tarea.id,
            BackendActividadRequestDto(actividad.accion)
        ).requireBody("guardar historial de la tarea")
    }

    override suspend fun insertarReporte(
        reporte: ReporteProblema,
        sector: Sector,
        cultivo: Cultivo?,
        usuario: Usuario,
        fotografia: Uri
    ) {
        val evidencia = copiarEvidenciaLocal(
            fotografia,
            descripcion = "Imagen referencial de ${reporte.tipoProblema}",
            reporteId = reporte.id
        )
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val reporteRemoto = api.guardarReporteBackend(
            reporte.reporteToBackendRequest(cultivo?.id ?: reporte.cultivoId)
        ).requireBody("crear reporte")
        val archivo = File(evidencia.rutaArchivo)
        if (!archivo.isFile) throw IOException("No se encontró el archivo de evidencia del reporte.")
        val part = MultipartBody.Part.createFormData(
            "file",
            archivo.name,
            archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        api.subirEvidencia(
            part,
            null,
            null,
            reporteRemoto.id.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            evidencia.descripcion.toRequestBody("text/plain".toMediaTypeOrNull()),
        ).requireBody("subir evidencia del reporte")
        if (!archivo.delete()) {
            throw IOException("El reporte se subió, pero no se pudo limpiar el archivo temporal.")
        }
    }

    override suspend fun obtenerReportesProblema(usuarioId: String?): List<ReporteProblema> {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        val cultivos = api.obtenerCultivosBackend().requireBody("cultivos")
            .map { it.backendCultivoToDomain() }
            .associateBy { it.id }
        val reportes = api.obtenerReportesBackend().requireBody("reportes").map { reporte ->
            val cultivo = cultivos[reporte.cultivoId.toString()]
                ?: throw IllegalStateException(
                    "El reporte ${reporte.id} referencia un cultivo que no devolvió la API."
                )
            reporte.backendReporteToDomain(cultivo)
        }
        return if (usuarioId == null) reportes else reportes.filter { it.usuarioId == usuarioId }
    }

    override suspend fun actualizarReporte(reporte: ReporteProblema) {
        val api = apiService ?: throw IOException("El backend no está configurado.")
        api.actualizarReporteBackend(
            reporte.id,
            reporte.reporteToBackendRequest()
        ).requireBody("actualizar reporte")
    }

    private companion object {
        val DEMO_USER_IDS = setOf(
            "demo-docente", "demo-estudiante"
        )
        val DEMO_EMAILS = setOf(
            "docente@huertolindo.test",
            "estudiante@huertolindo.test"
        )
        val DEMO_SECTOR_NAMES = setOf("Sector Norte", "Sector Sur")
        val DEMO_CROP_NAMES = setOf("Lechuga", "Espinaca", "Zanahoria")
        val DEMO_TASK_NAMES = setOf("Regar cultivos", "Revisar zanahorias")
    }
}
