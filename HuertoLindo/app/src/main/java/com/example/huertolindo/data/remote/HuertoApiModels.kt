package com.example.huertolindo.data.remote

import com.google.gson.annotations.SerializedName
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.Validacion
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.security.PermisosHuerto
import java.util.Locale

data class LoginRequestDto(
    @SerializedName("correo") val correo: String,
    @SerializedName("password") val password: String
)

data class UsuarioDto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellido") val apellido: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("perfil") val perfil: String,
    @SerializedName("tallerAsignado") val tallerAsignado: String?,
    @SerializedName("passwordSalt") val passwordSalt: String?,
    @SerializedName("passwordHash") val passwordHash: String?
)

data class SectorDto(
    @SerializedName("id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("tipoSuelo") val tipoSuelo: String?,
    @SerializedName("superficieMetros") val superficieMetros: Double,
    @SerializedName("ubicacion") val ubicacion: String?
)

data class CultivoDto(
    @SerializedName("id") val id: String,
    @SerializedName("sectorId") val sectorId: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("especie") val especie: String?,
    @SerializedName("variedad") val variedad: String?,
    @SerializedName("fechaSiembra") val fechaSiembra: String,
    @SerializedName("fechaCosechaEstimada") val fechaCosechaEstimada: String?,
    @SerializedName("frecuenciaRiego") val frecuenciaRiego: String?,
    @SerializedName("estadoDesarrollo") val estadoDesarrollo: String,
    @SerializedName("observaciones") val observaciones: String?
)

data class TareaDto(
    @SerializedName("id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("sectorId") val sectorId: String,
    @SerializedName("cultivoId") val cultivoId: String?,
    @SerializedName("asignadoAId") val asignadoAId: String?,
    @SerializedName("fechaCreacion") val fechaCreacion: String,
    @SerializedName("fechaVencimiento") val fechaVencimiento: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("tipoTarea") val tipoTarea: String,
    @SerializedName("prioridad") val prioridad: String
)

data class EvidenciaDto(
    @SerializedName("id") val id: String,
    @SerializedName("tareaId") val tareaId: String?,
    @SerializedName("tipoAsociacion") val tipoAsociacion: String?,
    @SerializedName("entidadId") val entidadId: String?,
    @SerializedName("rutaArchivo") val rutaArchivo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("fechaCreacion") val fechaCreacion: String
)

data class ValidacionDto(
    @SerializedName("id") val id: String,
    @SerializedName("tareaId") val tareaId: String,
    @SerializedName("administradorId") val administradorId: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("comentario") val comentario: String?,
    @SerializedName("fechaValidacion") val fechaValidacion: String
)

data class ReporteProblemaDto(
    @SerializedName("id") val id: String,
    @SerializedName("sectorId") val sectorId: String,
    @SerializedName("cultivoId") val cultivoId: String?,
    @SerializedName("usuarioId") val usuarioId: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaReporte") val fechaReporte: String,
    @SerializedName("fotografiaUri") val fotografiaUri: String?
)

fun UsuarioDto.toDomain() = Usuario(
    id = this.id,
    nombre = this.nombre,
    apellido = this.apellido,
    correo = this.correo,
    perfil = when (this.perfil.trim().uppercase(Locale.ROOT)) {
        "ADMIN", "ADMINISTRADOR" -> PermisosHuerto.ADMINISTRADOR
        else -> PermisosHuerto.ESTUDIANTE
    },
    tallerAsignado = this.tallerAsignado ?: "",
    passwordSalt = this.passwordSalt ?: "",
    passwordHash = this.passwordHash ?: ""
)

fun Usuario.toDto() = UsuarioDto(
    id = this.id,
    nombre = this.nombre,
    apellido = this.apellido,
    correo = this.correo,
    perfil = when (this.perfil.trim().uppercase(Locale.ROOT)) {
        "ADMIN", "ADMINISTRADOR" -> "ADMIN"
        else -> "ESTUDIANTE"
    },
    tallerAsignado = this.tallerAsignado,
    passwordSalt = this.passwordSalt.takeIf { it.isNotBlank() },
    passwordHash = this.passwordHash.takeIf { it.isNotBlank() }
)

fun SectorDto.toDomain() = Sector(
    id = this.id,
    nombre = this.nombre,
    descripcion = this.descripcion ?: "",
    estado = "En uso",
    ubicacion = this.ubicacion ?: ""
)

fun Sector.toDto() = SectorDto(
    id = this.id,
    nombre = this.nombre,
    descripcion = this.descripcion,
    tipoSuelo = "General",
    superficieMetros = 100.0,
    ubicacion = this.ubicacion
)

fun CultivoDto.toDomain(): Cultivo {
    val estadoMapeado = when (this.estadoDesarrollo.trim().uppercase(Locale.ROOT)) {
        "PLANIFICADO" -> "Planificado"
        "SEMBRADO" -> "Sembrado"
        "EN_CRECIMIENTO", "EN CRECIMIENTO" -> "En crecimiento"
        "LISTO_COSECHA", "LISTO PARA COSECHAR" -> "Listo para cosechar"
        "COSECHADO" -> "Cosechado"
        "CON_PROBLEMAS", "CON PROBLEMAS" -> "Con problemas"
        "FINALIZADO" -> "Finalizado"
        else -> this.estadoDesarrollo
    }
    return Cultivo(
        id = this.id,
        nombreComun = this.nombre,
        sectorId = this.sectorId,
        fechaSiembra = this.fechaSiembra,
        estadoDesarrollo = estadoMapeado,
        frecuenciaRiego = this.frecuenciaRiego ?: "7",
        fechaCosechaEstimada = this.fechaCosechaEstimada ?: ""
    )
}

fun Cultivo.toDto(): CultivoDto {
    val estadoDto = toCultivoEstadoBackend()
    return CultivoDto(
        id = this.id,
        sectorId = this.sectorId,
        nombre = this.nombreComun,
        especie = "",
        variedad = "",
        fechaSiembra = this.fechaSiembra,
        fechaCosechaEstimada = this.fechaCosechaEstimada.takeIf { it.isNotBlank() },
        frecuenciaRiego = this.frecuenciaRiego,
        estadoDesarrollo = estadoDto,
        observaciones = ""
    )
}

fun Cultivo.toCultivoEstadoBackend(): String = when (this.estadoDesarrollo.trim().uppercase(Locale.ROOT)) {
    "PLANIFICADO" -> "PLANIFICADO"
    "SEMBRADO" -> "SEMBRADO"
    "EN CRECIMIENTO", "EN_CRECIMIENTO", "DESARROLLO" -> "EN_CRECIMIENTO"
    "LISTO PARA COSECHAR", "LISTO_COSECHA" -> "LISTO_COSECHA"
    "COSECHADO" -> "COSECHADO"
    "CON PROBLEMAS", "CON_PROBLEMAS" -> "CON_PROBLEMAS"
    "FINALIZADO" -> "FINALIZADO"
    else -> this.estadoDesarrollo.uppercase(Locale.ROOT).replace(" ", "_")
}

fun TareaDto.toDomain(): Tarea {
    val estadoMapeado = when (this.estado.trim().uppercase(Locale.ROOT)) {
        "PENDIENTE" -> "Pendiente"
        "EN_DESARROLLO", "EN DESARROLLO" -> "En desarrollo"
        "PENDIENTE_VALIDACION", "PENDIENTE DE VALIDACION", "PENDIENTE DE VALIDACIÓN", "POR VALIDAR" -> "Por validar"
        "VALIDADA" -> "Validada"
        "RECHAZADA" -> "Rechazada"
        "ATRASADA" -> "Atrasada"
        "COMPLETADA" -> "Completada"
        else -> this.estado
    }
    return Tarea(
        id = this.id,
        titulo = this.titulo,
        descripcion = this.descripcion ?: "",
        sectorId = this.sectorId,
        asignadoA = this.asignadoAId ?: "",
        estado = estadoMapeado,
        fechaProgramada = this.fechaVencimiento ?: "",
        tipo = this.tipoTarea,
        cultivoId = this.cultivoId
    )
}

fun Tarea.toDto(): TareaDto {
    val estadoDto = toTareaEstadoBackend()
    return TareaDto(
        id = this.id,
        titulo = this.titulo,
        descripcion = this.descripcion,
        sectorId = this.sectorId,
        cultivoId = this.cultivoId,
        asignadoAId = this.asignadoA.takeIf { it.isNotBlank() },
        fechaCreacion = this.fechaProgramada.takeIf { it.isNotBlank() } ?: "2026-01-01",
        fechaVencimiento = this.fechaProgramada.takeIf { it.isNotBlank() },
        estado = estadoDto,
        tipoTarea = this.tipo,
        prioridad = "Media"
    )
}

fun String.toTareaEstadoBackend(): String = when (this.trim().uppercase(Locale.ROOT)) {
    "PENDIENTE" -> "PENDIENTE"
    "EN DESARROLLO", "EN_DESARROLLO" -> "EN_DESARROLLO"
    "POR VALIDAR", "PENDIENTE DE VALIDACION", "PENDIENTE DE VALIDACIÓN", "PENDIENTE_VALIDACION" -> "PENDIENTE_VALIDACION"
    "VALIDADA" -> "VALIDADA"
    "RECHAZADA" -> "RECHAZADA"
    "ATRASADA" -> "ATRASADA"
    "COMPLETADA" -> "COMPLETADA"
    else -> this.uppercase(Locale.ROOT).replace(" ", "_")
}

fun Tarea.toTareaEstadoBackend(): String = this.estado.toTareaEstadoBackend()

fun EvidenciaDto.toDomain() = Evidencia(
    id = this.id,
    tareaId = this.tareaId,
    cultivoId = if (this.tipoAsociacion == "CULTIVO") this.entidadId else null,
    reporteId = if (this.tipoAsociacion == "REPORTE") this.entidadId else null,
    tipo = "foto",
    rutaArchivo = this.rutaArchivo,
    descripcion = this.descripcion ?: "",
    fechaRegistro = this.fechaCreacion
)

fun Evidencia.toDto() = EvidenciaDto(
    id = this.id,
    tareaId = this.tareaId,
    tipoAsociacion = when {
        this.cultivoId != null -> "CULTIVO"
        this.reporteId != null -> "REPORTE"
        else -> "TAREA"
    },
    entidadId = this.tareaId ?: this.cultivoId ?: this.reporteId,
    rutaArchivo = this.rutaArchivo,
    descripcion = this.descripcion,
    fechaCreacion = this.fechaRegistro
)

fun ValidacionDto.toDomain() = Validacion(
    id = this.id,
    tareaId = this.tareaId,
    administradorId = this.administradorId,
    resultado = when (this.estado.trim().uppercase(Locale.ROOT)) {
        "VALIDADA", "APROBADA" -> "Validada"
        "RECHAZADA" -> "Rechazada"
        else -> this.estado
    },
    observaciones = this.comentario ?: "",
    fechaValidacion = this.fechaValidacion
)

fun Validacion.toDto() = ValidacionDto(
    id = this.id,
    tareaId = this.tareaId,
    administradorId = this.administradorId,
    estado = when (this.resultado.trim().uppercase(Locale.ROOT)) {
        "VALIDADA", "APROBADA" -> "VALIDADA"
        "RECHAZADA" -> "RECHAZADA"
        else -> this.resultado.uppercase(Locale.ROOT)
    },
    comentario = this.observaciones,
    fechaValidacion = this.fechaValidacion
)

fun ReporteProblemaDto.toDomain() = ReporteProblema(
    id = this.id,
    sectorId = this.sectorId,
    cultivoId = this.cultivoId,
    usuarioId = this.usuarioId,
    descripcion = this.descripcion ?: "",
    prioridad = "Media",
    estado = when (this.estado.trim().uppercase(Locale.ROOT)) {
        "INFORMADO", "PENDIENTE" -> "Informado"
        "EN_REVISION", "EN REVISION", "EN REVISIÓN" -> "En revisión"
        "ATENDIDO" -> "Atendido"
        "CERRADO", "RESUELTO" -> "Cerrado"
        else -> this.estado
    },
    fechaReporte = this.fechaReporte,
    tipoProblema = this.titulo
)

fun ReporteProblema.toDto() = ReporteProblemaDto(
    id = this.id,
    sectorId = this.sectorId,
    cultivoId = this.cultivoId,
    usuarioId = this.usuarioId,
    titulo = this.tipoProblema,
    descripcion = this.descripcion,
    estado = when (this.estado.trim().uppercase(Locale.ROOT)) {
        "INFORMADO", "PENDIENTE" -> "INFORMADO"
        "EN REVISION", "EN REVISIÓN", "EN_REVISION" -> "EN_REVISION"
        "ATENDIDO" -> "ATENDIDO"
        "CERRADO", "RESUELTO" -> "CERRADO"
        else -> this.estado.uppercase(Locale.ROOT).replace(" ", "_")
    },
    fechaReporte = this.fechaReporte,
    fotografiaUri = null
)
