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
import com.example.huertolindo.data.local.mapper.toDomain
import com.example.huertolindo.data.local.mapper.toEntity
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

class RoomHuertoRepository(context: Context) : HuertoRepository {
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
                    passwordSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                    passwordHash = Base64.encodeToString(
                        PasswordHasher.hash(password, salt),
                        Base64.NO_WRAP
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
                val snapshot = cargarSnapshot()
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

    override suspend fun obtenerUsuarios(): List<Usuario> =
        database.usuarioDao()
            .obtenerTodos()
            .map { it.toDomain().copy(passwordSalt = "", passwordHash = "") }

    override suspend fun autenticar(correo: String, password: String): Usuario? {
        val usuario = database.usuarioDao()
            .obtenerPorCorreo(correo.trim().lowercase(Locale.ROOT))
            ?: return null
        if (usuario.passwordSalt.isBlank() || usuario.passwordHash.isBlank()) return null
        val valido = PasswordHasher.verify(
            password,
            Base64.decode(usuario.passwordSalt, Base64.NO_WRAP),
            Base64.decode(usuario.passwordHash, Base64.NO_WRAP)
        )
        return usuario.takeIf { valido }
            ?.toDomain()
            ?.copy(passwordSalt = "", passwordHash = "")
    }

    override suspend fun registrarUsuario(usuario: Usuario) {
        database.usuarioDao().insertar(usuario.toEntity())
    }

    override suspend fun registrarAdministrador(usuario: Usuario, administradorId: String): Boolean =
        database.withTransaction {
            val administrador = database.usuarioDao().obtenerPorId(administradorId)
            if (!PermisosHuerto.esAdministrador(administrador?.perfil) ||
                !PermisosHuerto.esAdministrador(usuario.perfil)
            ) {
                return@withTransaction false
            }
            database.usuarioDao().insertar(usuario.toEntity())
            true
        }

    override suspend fun cargarSnapshot(): HuertoSnapshot = HuertoSnapshot(
        sectores = database.sectorDao().obtenerTodos().map { it.toDomain() },
        cultivos = database.cultivoDao().obtenerTodos().map { it.toDomain() },
        tareas = database.tareaDao().obtenerTodos().map { it.toDomain() },
        evidencias = database.evidenciaDao().obtenerTodos().map { it.toDomain() },
        validaciones = database.validacionDao().obtenerTodos().map { it.toDomain() },
        reportesProblemas = database.reporteProblemaDao().obtenerTodos().map { it.toDomain() },
        actividades = database.actividadTareaDao().obtenerTodos().map { it.toDomain() }
    )

    override suspend fun guardarSector(sector: Sector) {
        database.sectorDao().insertar(sector.toEntity())
    }

    override suspend fun actualizarSector(sector: Sector) {
        database.sectorDao().actualizar(sector.toEntity())
    }

    override suspend fun eliminarSector(sector: Sector) {
        database.sectorDao().eliminar(sector.toEntity())
    }

    override suspend fun guardarCultivo(cultivo: Cultivo, sector: Sector) {
        database.withTransaction {
            database.sectorDao().asegurar(sector.toEntity())
            database.cultivoDao().insertar(cultivo.toEntity())
        }
    }

    override suspend fun actualizarCultivo(cultivo: Cultivo) {
        database.cultivoDao().actualizar(cultivo.toEntity())
    }

    override suspend fun eliminarCultivo(cultivo: Cultivo) {
        database.cultivoDao().eliminar(cultivo.toEntity())
    }

    override suspend fun guardarEvidencia(evidencia: Evidencia) {
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
    ) {
        database.withTransaction {
            database.sectorDao().asegurar(sector.toEntity())
            database.usuarioDao().asegurar(estudiante.toEntity())
            database.usuarioDao().asegurar(actor.toEntity())
            database.tareaDao().insertar(tarea.toEntity())
            database.actividadTareaDao().insertar(actividad.toEntity())
        }
    }

    override suspend fun actualizarTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    ) {
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

    override suspend fun obtenerReportesProblema(usuarioId: String?): List<ReporteProblema> =
        if (usuarioId == null) {
            database.reporteProblemaDao().obtenerTodos().map { it.toDomain() }
        } else {
            database.reporteProblemaDao().obtenerPorUsuario(usuarioId).map { it.toDomain() }
        }

    override suspend fun actualizarReporte(reporte: ReporteProblema) {
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
