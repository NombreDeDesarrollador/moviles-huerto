package com.example.huertolindo.data.remote

import com.google.gson.annotations.SerializedName
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Validacion
import com.example.huertolindo.domain.security.PermisosHuerto
import java.util.Locale

data class BackendAuthResponseDto(
    @SerializedName("usuario") val usuario: BackendUsuarioDto
)

data class BackendUsuarioDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("activo") val activo: Boolean
)

data class BackendRegistroRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

data class BackendUsuarioRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("activo") val activo: Boolean
)

data class BackendSectorDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("ubicacion") val ubicacion: String?
)

data class BackendSectorRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("ubicacion") val ubicacion: String
)

data class BackendCultivoDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("especie") val especie: String?,
    @SerializedName("fechaSiembra") val fechaSiembra: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("sectorId") val sectorId: Long,
    @SerializedName("sectorNombre") val sectorNombre: String
)

data class BackendCultivoRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("especie") val especie: String,
    @SerializedName("fechaSiembra") val fechaSiembra: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("sectorId") val sectorId: Long
)

data class BackendTareaDto(
    @SerializedName("id") val id: Long,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("fechaProgramada") val fechaProgramada: String,
    @SerializedName("completada") val completada: Boolean,
    @SerializedName("cultivoId") val cultivoId: Long,
    @SerializedName("cultivoNombre") val cultivoNombre: String,
    @SerializedName("responsableId") val responsableId: Long,
    @SerializedName("responsableNombre") val responsableNombre: String,
    @SerializedName("estado") val estado: String? = null
)

data class BackendTareaRequestDto(
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("tipo") val tipo: String,
    @SerializedName("fechaProgramada") val fechaProgramada: String,
    @SerializedName("completada") val completada: Boolean,
    @SerializedName("estado") val estado: String,
    @SerializedName("cultivoId") val cultivoId: Long,
    @SerializedName("responsableId") val responsableId: Long
)

data class BackendValidacionRequestDto(
    @SerializedName("aprobado") val aprobado: Boolean,
    @SerializedName("observacion") val observacion: String?,
    @SerializedName("tareaId") val tareaId: Long?,
    @SerializedName("evidenciaId") val evidenciaId: Long?
)

data class BackendValidacionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("aprobado") val aprobado: Boolean,
    @SerializedName("observacion") val observacion: String?,
    @SerializedName("fechaValidacion") val fechaValidacion: String,
    @SerializedName("validadorId") val validadorId: Long,
    @SerializedName("validadorNombre") val validadorNombre: String,
    @SerializedName("tareaId") val tareaId: Long?,
    @SerializedName("evidenciaId") val evidenciaId: Long?
)

data class BackendActividadDto(
    @SerializedName("id") val id: Long,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("fechaRegistro") val fechaRegistro: String,
    @SerializedName("tareaId") val tareaId: Long,
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("usuarioNombre") val usuarioNombre: String
)

data class BackendEvidenciaDto(
    @SerializedName("id") val id: Long,
    @SerializedName("rutaImagen") val rutaImagen: String,
    @SerializedName("comentario") val comentario: String?,
    @SerializedName("fechaSubida") val fechaSubida: String,
    @SerializedName("tareaId") val tareaId: Long?,
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("usuarioNombre") val usuarioNombre: String,
    @SerializedName("cultivoId") val cultivoId: Long?,
    @SerializedName("reporteId") val reporteId: Long?
)

data class BackendReporteDto(
    @SerializedName("id") val id: Long,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("gravedad") val gravedad: String,
    @SerializedName("resuelto") val resuelto: Boolean,
    @SerializedName("fechaReporte") val fechaReporte: String,
    @SerializedName("cultivoId") val cultivoId: Long,
    @SerializedName("cultivoNombre") val cultivoNombre: String,
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("usuarioNombre") val usuarioNombre: String
)

data class BackendReporteRequestDto(
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("gravedad") val gravedad: String,
    @SerializedName("cultivoId") val cultivoId: Long,
    @SerializedName("usuarioId") val usuarioId: Long
)

data class BackendActividadRequestDto(
    @SerializedName("descripcion") val descripcion: String
)

fun BackendActividadDto.toDomain(tarea: Tarea): ActividadTarea = ActividadTarea(
    id = id.toString(),
    tareaId = tareaId.toString(),
    tituloTarea = tarea.titulo,
    usuarioRelacionadoId = tarea.asignadoA,
    usuarioId = usuarioId.toString(),
    nombreUsuario = usuarioNombre,
    accion = descripcion,
    estado = tarea.estado,
    fecha = fechaRegistro
)

fun BackendEvidenciaDto.toDomain(): Evidencia = Evidencia(
    id = id.toString(),
    tareaId = tareaId?.toString(),
    cultivoId = cultivoId?.toString(),
    reporteId = reporteId?.toString(),
    tipo = "foto",
    rutaArchivo = RetrofitClient.mediaUrl(rutaImagen),
    descripcion = comentario.orEmpty(),
    fechaRegistro = fechaSubida
)

fun BackendValidacionDto.toDomain(tareaId: String): Validacion = Validacion(
    id = id.toString(),
    tareaId = (this.tareaId ?: tareaId.toLongOrNull())?.toString().orEmpty(),
    administradorId = validadorId.toString(),
    resultado = if (aprobado) "Validada" else "Rechazada",
    observaciones = observacion.orEmpty(),
    fechaValidacion = fechaValidacion
)

fun BackendReporteDto.toDomain(cultivo: Cultivo): ReporteProblema = ReporteProblema(
    id = id.toString(),
    sectorId = cultivo.sectorId,
    cultivoId = cultivoId.toString(),
    usuarioId = usuarioId.toString(),
    descripcion = descripcion,
    prioridad = gravedad,
    estado = if (resuelto) "Cerrado" else "Informado",
    fechaReporte = fechaReporte,
    tipoProblema = titulo
)

fun BackendUsuarioDto.toDomain(): Usuario = Usuario(
    id = id.toString(),
    nombre = nombre,
    apellido = "",
    correo = correo,
    perfil = if (PermisosHuerto.esAdministrador(rol)) {
        PermisosHuerto.ADMINISTRADOR
    } else {
        PermisosHuerto.ESTUDIANTE
    },
    tallerAsignado = "General"
)

fun BackendSectorDto.toDomain(): Sector = Sector(
    id = id.toString(),
    nombre = nombre,
    descripcion = descripcion.orEmpty(),
    estado = "En uso",
    ubicacion = ubicacion.orEmpty()
)

fun Sector.toBackendRequest(): BackendSectorRequestDto = BackendSectorRequestDto(
    nombre = nombre,
    descripcion = descripcion,
    ubicacion = ubicacion
)

fun BackendCultivoDto.toDomain(): Cultivo = Cultivo(
    id = id.toString(),
    nombreComun = nombre,
    sectorId = sectorId.toString(),
    fechaSiembra = fechaSiembra.orEmpty(),
    estadoDesarrollo = estado.toCultivoEstadoHuerto(),
    frecuenciaRiego = "7"
)

fun Cultivo.toBackendRequest(): BackendCultivoRequestDto = BackendCultivoRequestDto(
    nombre = nombreComun,
    especie = "",
    fechaSiembra = fechaSiembra,
    estado = when (estadoDesarrollo.trim().uppercase(Locale.ROOT)) {
        "SEMBRADO" -> "SEMBRADO"
        "EN CRECIMIENTO", "EN_CRECIMIENTO", "DESARROLLO" -> "EN_CRECIMIENTO"
        "LISTO PARA COSECHAR", "LISTO_COSECHA" -> "LISTO_COSECHA"
        "COSECHADO" -> "COSECHADO"
        "CON PROBLEMAS", "CON_PROBLEMAS" -> "CON_PROBLEMAS"
        "PLANIFICADO" ->
            throw IllegalArgumentException("El backend no admite cultivos Planificados; selecciona Sembrado.")
        else -> throw IllegalArgumentException("El estado del cultivo no es compatible con la API.")
    },
    sectorId = sectorId.toLongOrNull()
        ?: throw IllegalArgumentException("El sector no existe en el backend.")
)

fun BackendTareaDto.toDomain(cultivo: Cultivo): Tarea = Tarea(
    id = id.toString(),
    titulo = titulo,
    descripcion = descripcion.orEmpty(),
    sectorId = cultivo.sectorId,
    asignadoA = responsableId.toString(),
    estado = estado?.toEstadoTareaHuerto() ?: if (completada) "Completada" else "Pendiente",
    fechaProgramada = fechaProgramada,
    tipo = tipo.toTipoTareaHuerto(),
    cultivoId = cultivoId.toString()
)

fun Tarea.toBackendRequest(): BackendTareaRequestDto = BackendTareaRequestDto(
    titulo = titulo,
    descripcion = descripcion,
    tipo = tipo.toTipoTareaBackend(),
    fechaProgramada = fechaProgramada,
    completada = estado.equals("Completada", ignoreCase = true) ||
        estado.equals("Validada", ignoreCase = true),
    estado = estado.toTareaEstadoBackend(),
    cultivoId = cultivoId?.toLongOrNull()
        ?: throw IllegalArgumentException("Debes asociar la tarea a un cultivo existente en el backend."),
    responsableId = asignadoA.toLongOrNull()
        ?: throw IllegalArgumentException("El estudiante asignado no existe en el backend.")
)

fun Validacion.toBackendRequest(): BackendValidacionRequestDto = BackendValidacionRequestDto(
    aprobado = resultado.equals("Validada", ignoreCase = true),
    observacion = observaciones.takeIf { it.isNotBlank() },
    tareaId = tareaId.toLongOrNull()
        ?: throw IllegalArgumentException("La tarea no existe en el backend."),
    evidenciaId = null
)

fun ReporteProblema.toBackendRequest(cultivoId: String? = this.cultivoId): BackendReporteRequestDto =
    BackendReporteRequestDto(
        titulo = tipoProblema,
        descripcion = descripcion,
        gravedad = prioridad,
        cultivoId = cultivoId?.toLongOrNull()
            ?: throw IllegalArgumentException("El reporte debe asociarse a un cultivo existente en el backend."),
        usuarioId = usuarioId.toLongOrNull()
            ?: throw IllegalArgumentException("El usuario del reporte no existe en el backend.")
    )

private fun String.toEstadoTareaHuerto(): String = when (trim().uppercase(Locale.ROOT)) {
    "PENDIENTE" -> "Pendiente"
    "EN_DESARROLLO", "EN DESARROLLO" -> "En desarrollo"
    "PENDIENTE_VALIDACION", "PENDIENTE DE VALIDACION", "PENDIENTE DE VALIDACIÓN", "POR VALIDAR" -> "Por validar"
    "VALIDADA" -> "Validada"
    "RECHAZADA" -> "Rechazada"
    "ATRASADA" -> "Atrasada"
    "COMPLETADA" -> "Completada"
    else -> this
}

private fun String.toCultivoEstadoHuerto(): String = when (trim().uppercase(Locale.ROOT)) {
    "SEMBRADO" -> "Sembrado"
    "EN_CRECIMIENTO" -> "En crecimiento"
    "LISTO_COSECHA" -> "Listo para cosechar"
    "COSECHADO" -> "Cosechado"
    "CON_PROBLEMAS" -> "Con problemas"
    else -> this
}

private fun String.toTipoTareaBackend(): String = when (trim().uppercase(Locale.ROOT)) {
    "RIEGO" -> "RIEGO"
    "PODA" -> "PODA"
    "COSECHA" -> "COSECHA"
    "SIEMBRA", "MANTENIMIENTO", "OBSERVACIÓN", "OBSERVACION" -> "OTRO"
    else -> "OTRO"
}

private fun String.toTipoTareaHuerto(): String = when (trim().uppercase(Locale.ROOT)) {
    "RIEGO" -> "Riego"
    "PODA" -> "Poda"
    "COSECHA" -> "Cosecha"
    "FERTILIZACION", "LIMPIEZA" -> "Mantenimiento"
    else -> "Mantenimiento"
}
