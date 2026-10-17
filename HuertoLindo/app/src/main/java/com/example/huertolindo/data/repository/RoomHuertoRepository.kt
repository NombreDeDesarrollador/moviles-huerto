package com.example.huertolindo.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.huertolindo.data.local.HuertoDatabase
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.LegacyUsuarioImporter
import com.example.huertolindo.data.model.ActividadTarea
import com.example.huertolindo.data.model.Cultivo
import com.example.huertolindo.data.model.Evidencia
import com.example.huertolindo.data.model.ReporteProblema
import com.example.huertolindo.data.model.Sector
import com.example.huertolindo.data.model.Tarea
import com.example.huertolindo.data.model.TipoAsociacionEvidencia
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.data.model.Validacion
import com.example.huertolindo.data.security.PasswordHasher
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
        database.withTransaction {
            usuarios.forEach { usuario ->
                val normalizado = usuario.copy(correo = usuario.correo.trim().lowercase(Locale.ROOT))
                database.usuarioDao().asegurar(normalizado)
                if (normalizado.passwordHash.isNotBlank()) {
                    database.usuarioDao().importarCredencialesSiVacias(
                        normalizado.id,
                        normalizado.passwordSalt,
                        normalizado.passwordHash
                    )
                }
            }

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
                    sectoresDemo.forEach { database.sectorDao().asegurar(it) }
                    cultivosDemo.forEach { database.cultivoDao().asegurar(it) }
                    tareasDemo.forEach { database.tareaDao().insertar(it) }
                }
            }
        }
        preferences.edit().putBoolean(DEMO_DATA_INITIALIZED, true).apply()
    }

    override suspend fun obtenerUsuarios(): List<Usuario> =
        database.usuarioDao().obtenerTodos().map {
            it.copy(passwordSalt = "", passwordHash = "")
        }

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
        return usuario.takeIf { valido }?.copy(passwordSalt = "", passwordHash = "")
    }

    override suspend fun registrarUsuario(usuario: Usuario) {
        database.usuarioDao().insertar(usuario)
    }

    override suspend fun cargarSnapshot(): HuertoSnapshot = HuertoSnapshot(
        sectores = database.sectorDao().obtenerTodos(),
        cultivos = database.cultivoDao().obtenerTodos(),
        tareas = database.tareaDao().obtenerTodos(),
        evidencias = database.evidenciaDao().obtenerTodos(),
        validaciones = database.validacionDao().obtenerTodos(),
        reportesProblemas = database.reporteProblemaDao().obtenerTodos(),
        actividades = database.actividadTareaDao().obtenerTodos()
    )

    override suspend fun guardarSector(sector: Sector) {
        database.sectorDao().insertar(sector)
    }

    override suspend fun actualizarSector(sector: Sector) {
        database.sectorDao().actualizar(sector)
    }

    override suspend fun eliminarSector(sector: Sector) {
        database.sectorDao().eliminar(sector)
    }

    override suspend fun guardarCultivo(cultivo: Cultivo, sector: Sector) {
        database.withTransaction {
            database.sectorDao().asegurar(sector)
            database.cultivoDao().insertar(cultivo)
        }
    }

    override suspend fun actualizarCultivo(cultivo: Cultivo) {
        database.cultivoDao().actualizar(cultivo)
    }

    override suspend fun eliminarCultivo(cultivo: Cultivo) {
        database.cultivoDao().eliminar(cultivo)
    }

    override suspend fun guardarEvidencia(evidencia: Evidencia) {
        database.evidenciaDao().insertar(evidencia)
    }

    override suspend fun guardarEvidenciaLocal(
        tareaId: String,
        comentario: String,
        imagenUri: Uri
    ): Evidencia = copiarEvidenciaLocal(
        imagenUri = imagenUri,
        descripcion = comentario,
        tareaId = tareaId
    )

    override suspend fun guardarEvidenciaAsociada(
        tipoAsociacion: TipoAsociacionEvidencia,
        entidadId: String,
        imagenUri: Uri
    ): Evidencia {
        val evidencia = copiarEvidenciaLocal(
            imagenUri = imagenUri,
            tareaId = if (tipoAsociacion == TipoAsociacionEvidencia.TAREA) entidadId else null,
            cultivoId = if (tipoAsociacion == TipoAsociacionEvidencia.CULTIVO) entidadId else null,
            reporteId = if (tipoAsociacion == TipoAsociacionEvidencia.REPORTE) entidadId else null
        )
        var guardada = false
        try {
            database.evidenciaDao().insertar(evidencia)
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
        val carpeta = File(context.filesDir, "evidencias")
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw IOException("No se pudo crear el directorio de evidencias.")
        }
        val archivo = File(carpeta, "${UUID.randomUUID()}.jpg")
        val input = context.contentResolver.openInputStream(imagenUri)
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

    override suspend fun eliminarEvidencia(evidencia: Evidencia) {
        val archivo = File(evidencia.rutaArchivo)
        if (archivo.exists() && !archivo.delete()) {
            throw IOException("No se pudo eliminar el archivo de evidencia.")
        }
        database.evidenciaDao().eliminar(evidencia)
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
                database.evidenciaDao().insertar(evidencia)
            }
        }
        preferences.edit().remove("evidencias").apply()
    }

    override suspend fun migrarHistorialLegacy(actividadesExistentes: List<ActividadTarea>) {
        if (actividadesExistentes.isNotEmpty()) return
        val json = context.getSharedPreferences("historial_tareas", Context.MODE_PRIVATE)
            .getString("actividades", null) ?: return
        val elementos = JSONArray(json)
        val usuarios = database.usuarioDao().obtenerTodos().associateBy { it.id }
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
            database.sectorDao().asegurar(sector)
            database.usuarioDao().asegurar(estudiante)
            database.usuarioDao().asegurar(actor)
            database.tareaDao().insertar(tarea)
            database.actividadTareaDao().insertar(actividad)
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
            database.usuarioDao().asegurar(actor)
            database.usuarioDao().asegurar(estudiante)
            database.sectorDao().asegurar(sector)
            database.tareaDao().actualizar(tarea)
            database.actividadTareaDao().insertar(actividad)
        }
    }

    override suspend fun entregarTarea(
        tarea: Tarea,
        evidencia: Evidencia,
        estudiante: Usuario,
        actividad: ActividadTarea
    ) {
        database.withTransaction {
            database.usuarioDao().asegurar(estudiante)
            database.tareaDao().actualizar(tarea)
            database.evidenciaDao().insertar(evidencia)
            database.actividadTareaDao().insertar(actividad)
        }
    }

    override suspend fun validarTarea(
        tarea: Tarea,
        validacion: Validacion,
        actividad: ActividadTarea,
        administrador: Usuario
    ) {
        database.withTransaction {
            database.usuarioDao().asegurar(administrador)
            database.usuarioDao().asegurar(
                database.usuarioDao().obtenerPorId(tarea.asignadoA)
                    ?: throw IllegalStateException("El responsable de la tarea ya no existe.")
            )
            database.tareaDao().actualizar(tarea)
            database.validacionDao().insertar(validacion)
            database.actividadTareaDao().insertar(actividad)
        }
    }

    override suspend fun migrarActividad(actividad: ActividadTarea, actor: Usuario) {
        database.withTransaction {
            val tarea = database.tareaDao().obtenerPorId(actividad.tareaId) ?: return@withTransaction
            database.usuarioDao().asegurar(actor)
            database.usuarioDao().asegurar(
                database.usuarioDao().obtenerPorId(tarea.asignadoA)
                    ?: throw IllegalStateException("El responsable de la actividad ya no existe.")
            )
            database.actividadTareaDao().insertar(actividad)
        }
    }

    override suspend fun insertarReporte(
        reporte: ReporteProblema,
        sector: Sector,
        cultivo: Cultivo?,
        usuario: Usuario
    ) {
        database.withTransaction {
            database.sectorDao().asegurar(sector)
            cultivo?.let { database.cultivoDao().asegurar(it) }
            database.usuarioDao().asegurar(usuario)
            database.reporteProblemaDao().insertar(reporte)
        }
    }

    override suspend fun obtenerReportesProblema(usuarioId: String?): List<ReporteProblema> =
        if (usuarioId == null) {
            database.reporteProblemaDao().obtenerTodos()
        } else {
            database.reporteProblemaDao().obtenerPorUsuario(usuarioId)
        }

    override suspend fun actualizarReporte(reporte: ReporteProblema) {
        database.reporteProblemaDao().actualizar(reporte)
    }

    private companion object {
        const val PREFERENCES_NAME = "huerto_room_bootstrap"
        const val DEMO_DATA_INITIALIZED = "demo_data_initialized"

        val sectoresDemo = listOf(
            Sector(
                id = "1",
                nombre = "Sector Norte",
                descripcion = "Zona de hortalizas de hoja",
                estado = "En uso"
            ),
            Sector(
                id = "2",
                nombre = "Sector Sur",
                descripcion = "Zona de raíces y tubérculos",
                estado = "Disponible"
            )
        )

        val cultivosDemo = listOf(
            Cultivo("1", "Lechuga", "1", "2026-09-20", "En crecimiento", "Cada 2 días"),
            Cultivo("2", "Espinaca", "1", "2026-09-25", "En crecimiento", "Cada 2 días"),
            Cultivo("3", "Zanahoria", "2", "2026-09-10", "Desarrollo", "Cada 3 días")
        )

        val tareasDemo = listOf(
            Tarea(
                id = "1",
                titulo = "Regar cultivos",
                descripcion = "Aplicar agua en la base de cada planta",
                sectorId = "1",
                asignadoA = "student-vicente",
                estado = "Pendiente",
                fechaProgramada = "2026-10-17"
            ),
            Tarea(
                id = "2",
                titulo = "Revisar zanahorias",
                descripcion = "Verificar humedad y retirar malezas",
                sectorId = "2",
                asignadoA = "student-matias",
                estado = "Pendiente",
                fechaProgramada = "2026-10-18"
            )
        )
    }
}
