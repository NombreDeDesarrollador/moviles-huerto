package com.example.huertolindo.data.repository

import android.content.Context
import android.net.Uri
import com.example.huertolindo.data.model.ActividadTarea
import com.example.huertolindo.data.model.Cultivo
import com.example.huertolindo.data.model.Evidencia
import com.example.huertolindo.data.model.ReporteProblema
import com.example.huertolindo.data.model.Sector
import com.example.huertolindo.data.model.Tarea
import com.example.huertolindo.data.model.TipoAsociacionEvidencia
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.data.model.Validacion

data class HuertoSnapshot(
    val sectores: List<Sector>,
    val cultivos: List<Cultivo>,
    val tareas: List<Tarea>,
    val evidencias: List<Evidencia>,
    val validaciones: List<Validacion>,
    val reportesProblemas: List<ReporteProblema>,
    val actividades: List<ActividadTarea>
)

interface HuertoRepository {
    suspend fun inicializar()
    suspend fun obtenerUsuarios(): List<Usuario>
    suspend fun autenticar(correo: String, password: String): Usuario?
    suspend fun registrarUsuario(usuario: Usuario)
    suspend fun cargarSnapshot(): HuertoSnapshot
    suspend fun guardarSector(sector: Sector)
    suspend fun actualizarSector(sector: Sector)
    suspend fun eliminarSector(sector: Sector)
    suspend fun guardarCultivo(cultivo: Cultivo, sector: Sector)
    suspend fun actualizarCultivo(cultivo: Cultivo)
    suspend fun eliminarCultivo(cultivo: Cultivo)
    suspend fun guardarEvidencia(evidencia: Evidencia)
    suspend fun crearTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    )
    suspend fun actualizarTarea(
        tarea: Tarea,
        sector: Sector,
        estudiante: Usuario,
        actor: Usuario,
        actividad: ActividadTarea
    )
    suspend fun entregarTarea(
        tarea: Tarea,
        evidencia: Evidencia,
        estudiante: Usuario,
        actividad: ActividadTarea
    )
    suspend fun validarTarea(
        tarea: Tarea,
        validacion: Validacion,
        actividad: ActividadTarea,
        administrador: Usuario
    )
    suspend fun migrarActividad(actividad: ActividadTarea, actor: Usuario)
    suspend fun insertarReporte(
        reporte: ReporteProblema,
        sector: Sector,
        cultivo: Cultivo?,
        usuario: Usuario
    )
    suspend fun obtenerReportesProblema(usuarioId: String?): List<ReporteProblema>
    suspend fun actualizarReporte(reporte: ReporteProblema)
    suspend fun guardarEvidenciaLocal(tareaId: String, comentario: String, imagenUri: Uri): Evidencia
    suspend fun guardarEvidenciaAsociada(
        tipoAsociacion: TipoAsociacionEvidencia,
        entidadId: String,
        imagenUri: Uri
    ): Evidencia
    suspend fun eliminarEvidencia(evidencia: Evidencia)
    fun eliminarArchivoEvidencia(rutaArchivo: String): Boolean
    suspend fun migrarEvidenciasLegacy(evidenciasExistentes: List<Evidencia>)
    suspend fun migrarHistorialLegacy(actividadesExistentes: List<ActividadTarea>)

    companion object {
        fun create(context: Context): HuertoRepository = RoomHuertoRepository(context)
    }
}
