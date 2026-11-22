package com.example.huertolindo.data.repository

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.FaceDetector
import android.util.Base64
import androidx.room.withTransaction
import com.example.huertolindo.data.local.HuertoDatabase
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.LegacyUsuarioImporter
import com.example.huertolindo.data.local.entity.UsuarioEntity
import com.example.huertolindo.data.local.mapper.toDomain
import com.example.huertolindo.data.local.mapper.toEntity
import com.example.huertolindo.data.remote.HuertoApiService
import com.example.huertolindo.data.remote.LoginRequestDto
import com.example.huertolindo.data.remote.BackendRegistroRequestDto
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
import com.example.huertolindo.data.remote.toCultivoEstadoBackend
import com.example.huertolindo.data.remote.toDomain
import com.example.huertolindo.data.remote.toDto
import com.example.huertolindo.data.remote.toTareaEstadoBackend
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
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PasswordHasher
import com.example.huertolindo.domain.security.EvidenciaNoSeguraException
import com.example.huertolindo.domain.security.PermisosHuerto
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class RoomHuertoRepository(
    context: Context,
    private val apiService: HuertoApiService? = null
) : HuertoRepository {
    private val context = context.applicationContext
    private val database: HuertoDatabase = HuertoDatabaseProvider.getDatabase(context)
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override suspend fun inicializar() {
        val importer = LegacyUsuarioImporter(context)
        try {
            inicializar(importer.importarUsuarios())
            importer.eliminarContrasenasEnClaro()
        } finally {
            importer.close()
        }
    }

    private fun <T> Response<T>.requireBody(endpoint: String): T {
        if (!isSuccessful) {
            throw IllegalStateException("El backend rechazó $endpoint (HTTP ${code()}).")
        }
        return body() ?: throw IllegalStateException("El backend no devolvió datos al $endpoint.")
    }

    private fun Response<*>.requireSuccessful(endpoint: String) {
        if (!isSuccessful) {
            throw IllegalStateException("El backend rechazó $endpoint (HTTP ${code()}).")
        }
    }

    private suspend fun inicializar(usuarios: List<Usuario>) {
        val cuentasDemo = listOf(
            Triple("demo-docente", "docente@huertolindo.test", "HuertoDocente2026!"),
            Triple("demo-estudiante", "estudiante@huertolindo.test", "HuertoEstudiante2026!")
        ).filter { (id, _, _) -> database.usuarioDao().obtenerPorId(id) == null }
            .map { (id, correo, password) ->
                val perfil = if (id == "demo-docente") {
                    PermisosHuerto.ADMINISTRADOR
                } else {
                    PermisosHuerto.ESTUDIANTE
                }
                val salt = PasswordHasher.createSalt()
                Usuario(
                    id = id,
                    nombre = if (perfil == PermisosHuerto.ADMINISTRADOR) "Docente Demo" else "Estudiante Demo",
                    apellido = "Huerto",
                    correo = correo,
                    perfil = perfil,
                    tallerAsignado = "Demo",
                    passwordSalt = PasswordHasher.encodeBase64(salt),
                    passwordHash = PasswordHasher.encodeBase64(
                        PasswordHasher.hash(password, salt)
                    )
                )
            }
        database.withTransaction {
            usuarios.forEach { usuario ->
                val normalizado = usuario.copy(correo = usuario.correo.trim().lowercase(Locale.ROOT))
                database.usuarioDao().asegurar(normalizado.toEntity())
                if (normalizado.passwordHash.isNotBlank()) {
                    database.usuarioDao().importarCredencialesSiVacias(
                        normalizado.id,
                        normalizado.passwordSalt,
                        normalizado.passwordHash
                    )
                }
            }
            cuentasDemo.forEach { database.usuarioDao().asegurar(it.toEntity()) }

            if (!preferences.getBoolean(DEMO_DATA_INITIALIZED, false)) {
                val snapshot = cargarSnapshotLocal()
                if (snapshot.sectores.isEmpty() &&
                    snapshot.cultivos.isEmpty() &&
                    snapshot.tareas.isEmpty() &&
                    snapshot.evidencias.isEmpty() &&
                    snapshot.validaciones.isEmpty() &&
                    snapshot.reportesProblemas.isEmpty() &&
                    snapshot.actividades.isEmpty()
                ) {
                    sectoresDemo.forEach { database.sectorDao().asegurar(it.toEntity()) }
                    cultivosDemo.forEach { database.cultivoDao().asegurar(it.toEntity()) }
                    tareasDemo.forEach { database.tareaDao().insertar(it.toEntity()) }
                }
            }
        }
        preferences.edit().putBoolean(DEMO_DATA_INITIALIZED, true).apply()
    }

    override suspend fun obtenerUsuarios(): List<Usuario> {
        if (apiService != null) {
            val response = try {
                apiService.obtenerUsuariosBackend()
            } catch (exception: IOException) {
                android.util.Log.w(
                    "RoomHuertoRepository",
                    "El backend no está disponible; se cargarán los usuarios de Room.",
                    exception
                )
                return database.usuarioDao().obtenerTodos().map {
                    it.toDomain().copy(passwordSalt = "", passwordHash = "")
                }
            }
            if (response.code() == 401 || response.code() == 403) {
                android.util.Log.i(
                    "RoomHuertoRepository",
                    "La sesión no permite listar usuarios; se cargarán los usuarios locales."
                )
                return database.usuarioDao().obtenerTodos().map {
                    it.toDomain().copy(passwordSalt = "", passwordHash = "")
                }
            }
            val remoteUsers = response.requireBody("usuarios")
                .map { it.backendUsuarioToDomain() }
            database.withTransaction {
                remoteUsers.forEach { database.usuarioDao().asegurar(it.toEntity()) }
            }
            return remoteUsers
        }
        return database.usuarioDao()
            .obtenerTodos()
            .map { it.toDomain().copy(passwordSalt = "", passwordHash = "") }
    }

    override suspend fun autenticar(correo: String, password: String): Usuario? {
        val normalizado = correo.trim().lowercase(Locale.ROOT)
        try {
            if (apiService != null) {
                val response = apiService.login(LoginRequestDto(normalizado, password))
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    if (!respuesta.usuario.activo) return null
                    val usuarioRemoto = respuesta.usuario.backendUsuarioToDomain()
                    database.usuarioDao().asegurar(usuarioRemoto.toEntity())
                    return usuarioRemoto.copy(passwordSalt = "", passwordHash = "")
                }
                if (response.code() in 400..499) return null
                android.util.Log.w(
                    "RoomHuertoRepository",
                    "El backend no pudo autenticar la cuenta (${response.code()}); se probará Room."
                )
            }
        } catch (exception: Exception) {
            android.util.Log.w("RoomHuertoRepository", "No se pudo autenticar en el backend; se probará Room.", exception)
        }

        val usuario = database.usuarioDao()
            .obtenerPorCorreo(normalizado)
            ?: return null
        if (usuario.passwordSalt.isBlank() || usuario.passwordHash.isBlank()) return null
        val valido = PasswordHasher.verify(
            password,
            PasswordHasher.decodeBase64(usuario.passwordSalt),
            PasswordHasher.decodeBase64(usuario.passwordHash)
        )
        return usuario.takeIf { valido }
            ?.toDomain()
            ?.copy(passwordSalt = "", passwordHash = "")
    }

    override suspend fun registrarUsuario(usuario: Usuario) {
        database.usuarioDao().insertar(usuario.toEntity())
    }

    override suspend fun registrarUsuario(usuario: Usuario, password: String) {
        if (apiService != null) {
            try {
                val response = apiService.registrarEnBackend(
                    BackendRegistroRequestDto(
                        nombre = usuario.nombreCompleto,
                        correo = usuario.correo,
                        password = password
                    )
                )
                if (response.isSuccessful) {
                    val remoteUser = response.body()?.backendUsuarioToDomain()
                        ?: throw IllegalStateException("El backend no devolvió el usuario registrado.")
                    database.usuarioDao().insertar(
                        remoteUser.copy(
                            tallerAsignado = usuario.tallerAsignado,
                            passwordSalt = usuario.passwordSalt,
                            passwordHash = usuario.passwordHash
                        ).toEntity()
                    )
                    return
                }
                if (response.code() in 400..499) {
                    throw IllegalStateException("El backend rechazó el registro (${response.code()}).")
                }
                android.util.Log.w(
                    "RoomHuertoRepository",
                    "El backend no pudo registrar la cuenta (${response.code()}); se guardará en Room."
                )
            } catch (exception: java.io.IOException) {
                android.util.Log.w("RoomHuertoRepository", "Backend no disponible; se guardará el registro en Room.", exception)
            }
        }
        registrarUsuario(usuario)
    }

    override suspend fun registrarAdministrador(usuario: Usuario, administradorId: String): Boolean =
        database.withTransaction {
            val administrador = database.usuarioDao().obtenerPorId(administradorId)
            if (!PermisosHuerto.esAdministrador(administrador?.perfil) ||
                !PermisosHuerto.esAdministrador(usuario.perfil)
            ) {
                return@withTransaction false
            }
            try {
                apiService?.registrar(usuario.toDto())
            } catch (_: Exception) {}
            database.usuarioDao().insertar(usuario.toEntity())
            true
        }

    private suspend fun cargarSnapshotLocal(): HuertoSnapshot = HuertoSnapshot(
        sectores = database.sectorDao().obtenerTodos().map { it.toDomain() },
        cultivos = database.cultivoDao().obtenerTodos().map { it.toDomain() },
        tareas = database.tareaDao().obtenerTodos().map { it.toDomain() },
        evidencias = database.evidenciaDao().obtenerTodos().map { it.toDomain() },
        validaciones = database.validacionDao().obtenerTodos().map { it.toDomain() },
        reportesProblemas = database.reporteProblemaDao().obtenerTodos().map { it.toDomain() },
        actividades = database.actividadTareaDao().obtenerTodos().map { it.toDomain() }
    )

    override suspend fun cargarSnapshot(): HuertoSnapshot = cargarSnapshotRemoto(null)

    override suspend fun cargarSnapshot(usuarioActual: Usuario): HuertoSnapshot =
        cargarSnapshotRemoto(usuarioActual)

    private suspend fun cargarSnapshotRemoto(usuarioActual: Usuario?): HuertoSnapshot {
        if (apiService == null) return cargarSnapshotLocal()

        val sectoresDto: List<BackendSectorDto>
        val cultivosDto: List<BackendCultivoDto>
        try {
            sectoresDto = apiService.obtenerSectoresBackend().requireBody("sectores")
            cultivosDto = apiService.obtenerCultivosBackend().requireBody("cultivos")
        } catch (exception: IOException) {
            android.util.Log.w(
                "RoomHuertoRepository",
                "El backend no está disponible; se cargará el snapshot local de Room.",
                exception
            )
            return cargarSnapshotLocal()
        }

        val sectores = sectoresDto.map { it.backendSectorToDomain() }
        val cultivos = cultivosDto.map { it.backendCultivoToDomain() }
        val cultivosPorId = cultivos.associateBy { it.id }
        var advertencia: String? = null
        val tareasDto = if (usuarioActual == null) {
            null
        } else {
            try {
                apiService.obtenerTareasBackend().requireBody("tareas")
            } catch (exception: IOException) {
                advertencia = "No se pudieron cargar las tareas del backend; los sectores y cultivos sí se sincronizaron."
                android.util.Log.w("RoomHuertoRepository", advertencia, exception)
                null
            } catch (exception: IllegalStateException) {
                val detalle = exception.message
                    ?: "No se pudieron cargar las tareas del backend."
                advertencia = "$detalle Cierra sesión y vuelve a iniciar sesión; si persiste, revisa el backend."
                android.util.Log.e("RoomHuertoRepository", advertencia, exception)
                null
            }
        }
        val tareasRemotas = tareasDto?.map { tareaDto ->
            val cultivo = cultivosPorId[tareaDto.cultivoId.toString()]
                ?: throw IllegalStateException(
                    "La tarea ${tareaDto.id} referencia un cultivo que no devolvió la API."
                )
            tareaDto.backendTareaToDomain(cultivo)
        } ?: emptyList()

        database.withTransaction {
            sectores.forEach { database.sectorDao().insertar(it.toEntity()) }
            cultivos.forEach { database.cultivoDao().insertar(it.toEntity()) }
            tareasDto.orEmpty().zip(tareasRemotas).forEach { (tareaDto, tarea) ->
                if (database.usuarioDao().obtenerPorId(tarea.asignadoA) == null) {
                    database.usuarioDao().asegurar(
                        UsuarioEntity(
                            id = tarea.asignadoA,
                            nombre = tareaDto.responsableNombre,
                            correo = "${tarea.asignadoA}@backend.huertolindo",
                            perfil = PermisosHuerto.ESTUDIANTE,
                            tallerAsignado = "General"
                        )
                    )
                }
                database.tareaDao().insertar(tarea.toEntity())
            }
        }

        val localSnapshot = cargarSnapshotLocal()
        return localSnapshot.copy(
            sectores = sectores,
            cultivos = cultivos,
            tareas = tareasDto?.let { tareasRemotas } ?: localSnapshot.tareas,
            advertencia = advertencia
        )
    }

    override suspend fun guardarSector(sector: Sector): Sector {
        val guardado = if (apiService != null) {
            apiService.guardarSectorBackend(sector.sectorToBackendRequest())
                .requireBody("crear sector")
                .backendSectorToDomain()
                .copy(
                    estado = sector.estado,
                    fechaInicioMantenimiento = sector.fechaInicioMantenimiento,
                    fechaTerminoMantenimiento = sector.fechaTerminoMantenimiento
                )
        } else {
            sector
        }
        database.sectorDao().insertar(guardado.toEntity())
        return guardado
    }

    override suspend fun actualizarSector(sector: Sector) {
        val actualizado = if (apiService != null) {
            apiService.actualizarSectorBackend(sector.id, sector.sectorToBackendRequest())
                .requireBody("actualizar sector")
                .backendSectorToDomain()
                .copy(
                    estado = sector.estado,
                    fechaInicioMantenimiento = sector.fechaInicioMantenimiento,
                    fechaTerminoMantenimiento = sector.fechaTerminoMantenimiento
                )
        } else {
            sector
        }
        database.sectorDao().actualizar(actualizado.toEntity())
    }

    override suspend fun eliminarSector(sector: Sector) {
        apiService?.eliminarSector(sector.id)?.requireSuccessful("eliminar sector")
        database.sectorDao().eliminar(sector.toEntity())
    }

    override suspend fun guardarCultivo(cultivo: Cultivo, sector: Sector): Cultivo {
        val guardado = if (apiService != null) {
            val response = apiService.guardarCultivoBackend(cultivo.cultivoToBackendRequest())
                .requireBody("crear cultivo")
                .backendCultivoToDomain()
            response.copy(
                fechaCosechaEstimada = cultivo.fechaCosechaEstimada,
                frecuenciaRiego = cultivo.frecuenciaRiego,
                fechaCambioEstado = cultivo.fechaCambioEstado
            )
        } else {
            cultivo
        }
        database.withTransaction {
            database.sectorDao().asegurar(sector.toEntity())
            database.cultivoDao().insertar(guardado.toEntity())
        }
        return guardado
    }

    override suspend fun actualizarCultivo(cultivo: Cultivo) {
        val actualizado = if (apiService != null) {
            val response = apiService.actualizarCultivoBackend(
                cultivo.id,
                cultivo.cultivoToBackendRequest()
            ).requireBody("actualizar cultivo").backendCultivoToDomain()
            response.copy(
                fechaCosechaEstimada = cultivo.fechaCosechaEstimada,
                frecuenciaRiego = cultivo.frecuenciaRiego,
                fechaCambioEstado = cultivo.fechaCambioEstado
            )
        } else {
            cultivo
        }
        database.cultivoDao().actualizar(actualizado.toEntity())
    }

    override suspend fun eliminarCultivo(cultivo: Cultivo) {
        apiService?.eliminarCultivo(cultivo.id)?.requireSuccessful("eliminar cultivo")
        database.cultivoDao().eliminar(cultivo.toEntity())
    }

    override suspend fun guardarEvidencia(evidencia: Evidencia) {
        try {
            // API upload if needed
        } catch (_: Exception) {}
        database.evidenciaDao().insertar(evidencia.toEntity())
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
        try {
            if (apiService != null) {
                val archivo = File(evidencia.rutaArchivo)
                if (archivo.exists()) {
                    val reqFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", archivo.name, reqFile)
                    val tipoStr = when (tipoAsociacion) {
                        TipoAsociacionEvidencia.TAREA -> "TAREA"
                        TipoAsociacionEvidencia.CULTIVO -> "CULTIVO"
                        TipoAsociacionEvidencia.REPORTE -> "REPORTE"
                    }
                    val tareaPart = if (tipoAsociacion == TipoAsociacionEvidencia.TAREA) entidadId.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                    val comPart = evidencia.descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                    val tipoPart = tipoStr.toRequestBody("text/plain".toMediaTypeOrNull())
                    val entPart = entidadId.toRequestBody("text/plain".toMediaTypeOrNull())
                    apiService.subirEvidencia(part, tareaPart, comPart, tipoPart, entPart)
                }
            }
        } catch (_: Exception) {}
        var guardada = false
        try {
            database.evidenciaDao().insertar(evidencia.toEntity())
            guardada = true
            return evidencia
        } finally {
            if (!guardada) eliminarArchivoEvidencia(evidencia.rutaArchivo)
        }
    }

    private fun copiarEvidenciaLocal(
        imagenUri: Uri,
        descripcion: String = "",
        tareaId: String? = null,
        cultivoId: String? = null,
        reporteId: String? = null
    ): Evidencia {
        val bitmap = decodificarFotografiaSinRostros(imagenUri)
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

    private fun decodificarFotografiaSinRostros(uri: Uri): Bitmap {
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
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("No se pudo analizar la fotografía seleccionada.")
        val ancho = bitmap.width - bitmap.width % 2
        val alto = bitmap.height - bitmap.height % 2
        if (ancho <= 0 || alto <= 0) {
            bitmap.recycle()
            throw IOException("La fotografía seleccionada no tiene un tamaño válido.")
        }
        val compatible = if (ancho == bitmap.width && alto == bitmap.height) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, ancho, alto).also { bitmap.recycle() }
        }
        var fotografiaValida = false
        try {
            if (FaceDetector(ancho, alto, 1)
                    .findFaces(compatible, arrayOfNulls(1)) > 0
            ) {
                throw EvidenciaNoSeguraException(
                    "No se admiten fotografías con rostros detectados. Usa imágenes de plantas o herramientas."
                )
            }
            fotografiaValida = true
            return compatible
        } finally {
            if (!fotografiaValida) compatible.recycle()
        }
    }

    override suspend fun eliminarEvidencia(evidencia: Evidencia) {
        val archivo = File(evidencia.rutaArchivo)
        if (archivo.exists() && !archivo.delete()) {
            throw IOException("No se pudo eliminar el archivo de evidencia.")
        }
        database.evidenciaDao().eliminar(evidencia.toEntity())
    }

    override fun eliminarArchivoEvidencia(rutaArchivo: String): Boolean = File(rutaArchivo).delete()

    override suspend fun migrarEvidenciasLegacy(evidenciasExistentes: List<Evidencia>) {
        if (evidenciasExistentes.isNotEmpty()) return
        val json = preferences.getString("evidencias", null) ?: return
        val elementos = JSONArray(json)
        val tareas = database.tareaDao().obtenerTodos().map { it.id }.toSet()
        for (index in 0 until elementos.length()) {
            val elemento = elementos.getJSONObject(index)
            val evidencia = Evidencia(
                id = elemento.getString("id"),
                tareaId = elemento.getString("tareaId"),
                tipo = elemento.getString("tipo"),
                rutaArchivo = elemento.getString("rutaArchivo"),
                descripcion = elemento.getString("descripcion"),
                fechaRegistro = elemento.getString("fechaRegistro")
            )
            if (File(evidencia.rutaArchivo).isFile && evidencia.tareaId in tareas) {
                database.evidenciaDao().insertar(evidencia.toEntity())
            }
        }
        preferences.edit().remove("evidencias").apply()
    }

    override suspend fun migrarHistorialLegacy(actividadesExistentes: List<ActividadTarea>) {
        if (actividadesExistentes.isNotEmpty()) return
        val json = context.getSharedPreferences("historial_tareas", Context.MODE_PRIVATE)
            .getString("actividades", null) ?: return
        val elementos = JSONArray(json)
        val usuarios = database.usuarioDao().obtenerTodos().map { it.toDomain() }.associateBy { it.id }
        for (index in 0 until elementos.length()) {
            val item = elementos.getJSONObject(index)
            val actividad = ActividadTarea(
                id = item.getString("id"),
                tareaId = item.getString("tareaId"),
                tituloTarea = item.getString("tituloTarea"),
                usuarioRelacionadoId = item.getString("usuarioRelacionadoId"),
                usuarioId = item.getString("usuarioId"),
                nombreUsuario = item.getString("nombreUsuario"),
                accion = item.getString("accion"),
                estado = item.getString("estado"),
                fecha = item.getString("fecha")
            )
            usuarios[actividad.usuarioId]?.let { actor -> migrarActividad(actividad, actor) }
        }
        context.getSharedPreferences("historial_tareas", Context.MODE_PRIVATE)
            .edit()
            .remove("actividades")
            .apply()
    }

    override suspend fun crearTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    ): Pair<Tarea, ActividadTarea> {
        val guardada = if (apiService != null) {
            val response = apiService.guardarTareaBackend(tarea.tareaToBackendRequest())
                .requireBody("crear tarea")
            response.backendTareaToDomain(
                Cultivo(
                    id = response.cultivoId.toString(),
                    nombreComun = response.cultivoNombre,
                    sectorId = sector.id,
                    fechaSiembra = "",
                    estadoDesarrollo = "Sembrado",
                    frecuenciaRiego = "7"
                )
            )
        } else {
            tarea
        }
        val actividadGuardada = actividad.copy(tareaId = guardada.id)
        database.withTransaction {
            database.sectorDao().asegurar(sector.toEntity())
            database.usuarioDao().asegurar(estudiante.toEntity())
            database.usuarioDao().asegurar(actor.toEntity())
            database.tareaDao().insertar(guardada.toEntity())
            database.actividadTareaDao().insertar(actividadGuardada.toEntity())
        }
        return guardada to actividadGuardada
    }

    override suspend fun actualizarTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    ) {
        apiService?.actualizarTareaBackend(tarea.id, tarea.tareaToBackendRequest())
            ?.requireBody("actualizar tarea")
        database.withTransaction {
            database.usuarioDao().asegurar(actor.toEntity())
            database.usuarioDao().asegurar(estudiante.toEntity())
            database.sectorDao().asegurar(sector.toEntity())
            database.tareaDao().actualizar(tarea.toEntity())
            database.actividadTareaDao().insertar(actividad.toEntity())
        }
    }

    override suspend fun entregarTarea(
        tarea: Tarea,
        evidencia: Evidencia,
        estudiante: Usuario,
        actividad: ActividadTarea
    ) {
        try {
            if (apiService != null) {
                val archivo = File(evidencia.rutaArchivo)
                if (archivo.exists()) {
                    val reqFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", archivo.name, reqFile)
                    val tareaIdPart = tarea.id.toRequestBody("text/plain".toMediaTypeOrNull())
                    val comPart = evidencia.descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                    val tipoPart = "TAREA".toRequestBody("text/plain".toMediaTypeOrNull())
                    val entidadIdPart = tarea.id.toRequestBody("text/plain".toMediaTypeOrNull())
                    apiService.subirEvidencia(part, tareaIdPart, comPart, tipoPart, entidadIdPart)
                }
                apiService.actualizarTarea(tarea.id, tarea.toDto())
                apiService.actualizarEstadoTarea(tarea.id, mapOf("estado" to tarea.toTareaEstadoBackend()))
            }
        } catch (_: Exception) {}
        database.withTransaction {
            database.usuarioDao().asegurar(estudiante.toEntity())
            database.tareaDao().actualizar(tarea.toEntity())
            database.evidenciaDao().insertar(evidencia.toEntity())
            database.actividadTareaDao().insertar(actividad.toEntity())
        }
    }

    override suspend fun validarTarea(
        tarea: Tarea,
        validacion: Validacion,
        actividad: ActividadTarea,
        administrador: Usuario
    ) {
        try {
            apiService?.actualizarTarea(tarea.id, tarea.toDto())
            apiService?.actualizarEstadoTarea(tarea.id, mapOf("estado" to tarea.toTareaEstadoBackend()))
            apiService?.guardarValidacion(validacion.toDto())
        } catch (_: Exception) {}
        database.withTransaction {
            database.usuarioDao().asegurar(administrador.toEntity())
            database.usuarioDao().asegurar(
                database.usuarioDao().obtenerPorId(tarea.asignadoA)
                    ?: throw IllegalStateException("El responsable de la tarea ya no existe.")
            )
            database.tareaDao().actualizar(tarea.toEntity())
            database.validacionDao().insertar(validacion.toEntity())
            database.actividadTareaDao().insertar(actividad.toEntity())
        }
    }

    override suspend fun migrarActividad(actividad: ActividadTarea, actor: Usuario) {
        database.withTransaction {
            val tarea = database.tareaDao().obtenerPorId(actividad.tareaId) ?: return@withTransaction
            database.usuarioDao().asegurar(actor.toEntity())
            database.usuarioDao().asegurar(
                database.usuarioDao().obtenerPorId(tarea.asignadoA)
                    ?: throw IllegalStateException("El responsable de la actividad ya no existe.")
            )
            database.actividadTareaDao().insertar(actividad.toEntity())
        }
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
        try {
            if (apiService != null) {
                apiService.guardarReporte(reporte.toDto())
                val archivo = File(evidencia.rutaArchivo)
                if (archivo.exists()) {
                    val reqFile = archivo.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", archivo.name, reqFile)
                    val comPart = evidencia.descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
                    val tipoPart = "REPORTE".toRequestBody("text/plain".toMediaTypeOrNull())
                    val entidadIdPart = reporte.id.toRequestBody("text/plain".toMediaTypeOrNull())
                    apiService.subirEvidencia(part, null, comPart, tipoPart, entidadIdPart)
                }
            }
        } catch (_: Exception) {}
        try {
            database.withTransaction {
                database.sectorDao().asegurar(sector.toEntity())
                cultivo?.let { database.cultivoDao().asegurar(it.toEntity()) }
                database.usuarioDao().asegurar(usuario.toEntity())
                database.reporteProblemaDao().insertar(reporte.toEntity())
                database.evidenciaDao().insertar(evidencia.toEntity())
            }
        } catch (exception: RuntimeException) {
            eliminarArchivoEvidencia(evidencia.rutaArchivo)
            throw exception
        }
    }

    override suspend fun obtenerReportesProblema(usuarioId: String?): List<ReporteProblema> {
        try {
            if (apiService != null) {
                val response = apiService.obtenerReportes()
                if (response.isSuccessful && response.body() != null) {
                    val reportes = response.body()!!.map { it.toDomain() }
                    database.withTransaction {
                        reportes.forEach { database.reporteProblemaDao().insertar(it.toEntity()) }
                    }
                    if (usuarioId == null) return reportes
                    return reportes.filter { it.usuarioId == usuarioId }
                }
            }
        } catch (_: Exception) {}
        return if (usuarioId == null) {
            database.reporteProblemaDao().obtenerTodos().map { it.toDomain() }
        } else {
            database.reporteProblemaDao().obtenerPorUsuario(usuarioId).map { it.toDomain() }
        }
    }

    override suspend fun actualizarReporte(reporte: ReporteProblema) {
        try {
            apiService?.actualizarReporte(reporte.id, reporte.toDto())
        } catch (_: Exception) {}
        database.reporteProblemaDao().actualizar(reporte.toEntity())
    }

    private companion object {
        const val PREFERENCES_NAME = "huerto_room_bootstrap"
        const val DEMO_DATA_INITIALIZED = "demo_data_initialized"

        val sectoresDemo = listOf(
            Sector(
                id = "1",
                nombre = "Sector Norte",
                descripcion = "Zona de hortalizas de hoja",
                estado = "En uso",
                ubicacion = "Patio norte"
            ),
            Sector(
                id = "2",
                nombre = "Sector Sur",
                descripcion = "Zona de raíces y tubérculos",
                estado = "Disponible",
                ubicacion = "Patio sur"
            )
        )

        val cultivosDemo = listOf(
            Cultivo(
                "1", "Lechuga", "1",                 "2026-09-20", "En crecimiento", "2",
                "2026-11-20"
            ),
            Cultivo(
                "2", "Espinaca", "1",                 "2026-09-25", "En crecimiento", "2",
                "2026-11-25"
            ),
            Cultivo(
                "3", "Zanahoria", "2",                 "2026-09-10", "Desarrollo", "3",
                "2026-12-10"
            )
        )

        val tareasDemo = listOf(
            Tarea(
                id = "1",
                titulo = "Regar cultivos",
                descripcion = "Aplicar agua en la base de cada planta",
                sectorId = "1",
                asignadoA = "demo-estudiante",
                estado = "Pendiente",
                fechaProgramada = "2026-10-17"
            ),
            Tarea(
                id = "2",
                titulo = "Revisar zanahorias",
                descripcion = "Verificar humedad y retirar malezas",
                sectorId = "2",
                asignadoA = "demo-estudiante",
                estado = "Pendiente",
                fechaProgramada = "2026-10-18"
            )
        )
    }
}
