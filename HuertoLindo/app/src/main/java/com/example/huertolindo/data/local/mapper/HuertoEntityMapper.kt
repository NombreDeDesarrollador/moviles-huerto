package com.example.huertolindo.data.local.mapper

import com.example.huertolindo.data.local.entity.ActividadTareaEntity
import com.example.huertolindo.data.local.entity.CultivoEntity
import com.example.huertolindo.data.local.entity.EvidenciaEntity
import com.example.huertolindo.data.local.entity.ReporteProblemaEntity
import com.example.huertolindo.data.local.entity.SectorEntity
import com.example.huertolindo.data.local.entity.TareaEntity
import com.example.huertolindo.data.local.entity.UsuarioEntity
import com.example.huertolindo.data.local.entity.ValidacionEntity
import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Validacion

fun ActividadTarea.toEntity() = ActividadTareaEntity(
    id = id,
    tareaId = tareaId,
    tituloTarea = tituloTarea,
    usuarioRelacionadoId = usuarioRelacionadoId,
    usuarioId = usuarioId,
    nombreUsuario = nombreUsuario,
    accion = accion,
    estado = estado,
    fecha = fecha
)

fun ActividadTareaEntity.toDomain() = ActividadTarea(
    id = id,
    tareaId = tareaId,
    tituloTarea = tituloTarea,
    usuarioRelacionadoId = usuarioRelacionadoId,
    usuarioId = usuarioId,
    nombreUsuario = nombreUsuario,
    accion = accion,
    estado = estado,
    fecha = fecha
)

fun Cultivo.toEntity() = CultivoEntity(
    id = id,
    nombreComun = nombreComun,
    sectorId = sectorId,
    fechaSiembra = fechaSiembra,
    estadoDesarrollo = estadoDesarrollo,
    frecuenciaRiego = frecuenciaRiego,
    fechaCosechaEstimada = fechaCosechaEstimada,
    fechaCambioEstado = fechaCambioEstado
)

fun CultivoEntity.toDomain() = Cultivo(
    id = id,
    nombreComun = nombreComun,
    sectorId = sectorId,
    fechaSiembra = fechaSiembra,
    estadoDesarrollo = estadoDesarrollo,
    frecuenciaRiego = frecuenciaRiego,
    fechaCosechaEstimada = fechaCosechaEstimada,
    fechaCambioEstado = fechaCambioEstado
)

fun Evidencia.toEntity() = EvidenciaEntity(
    id = id,
    tareaId = tareaId,
    cultivoId = cultivoId,
    reporteId = reporteId,
    tipo = tipo,
    rutaArchivo = rutaArchivo,
    descripcion = descripcion,
    fechaRegistro = fechaRegistro
)

fun EvidenciaEntity.toDomain() = Evidencia(
    id = id,
    tareaId = tareaId,
    cultivoId = cultivoId,
    reporteId = reporteId,
    tipo = tipo,
    rutaArchivo = rutaArchivo,
    descripcion = descripcion,
    fechaRegistro = fechaRegistro
)

fun ReporteProblema.toEntity() = ReporteProblemaEntity(
    id = id,
    sectorId = sectorId,
    cultivoId = cultivoId,
    usuarioId = usuarioId,
    descripcion = descripcion,
    prioridad = prioridad,
    estado = estado,
    fechaReporte = fechaReporte,
    tipoProblema = tipoProblema,
    comentarioSeguimiento = comentarioSeguimiento,
    fechaResolucion = fechaResolucion
)

fun ReporteProblemaEntity.toDomain() = ReporteProblema(
    id = id,
    sectorId = sectorId,
    cultivoId = cultivoId,
    usuarioId = usuarioId,
    descripcion = descripcion,
    prioridad = prioridad,
    estado = estado,
    fechaReporte = fechaReporte,
    tipoProblema = tipoProblema,
    comentarioSeguimiento = comentarioSeguimiento,
    fechaResolucion = fechaResolucion
)

fun Sector.toEntity() = SectorEntity(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    estado = estado,
    ubicacion = ubicacion,
    fechaInicioMantenimiento = fechaInicioMantenimiento,
    fechaTerminoMantenimiento = fechaTerminoMantenimiento
)

fun SectorEntity.toDomain() = Sector(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    estado = estado,
    ubicacion = ubicacion,
    fechaInicioMantenimiento = fechaInicioMantenimiento,
    fechaTerminoMantenimiento = fechaTerminoMantenimiento
)

fun Tarea.toEntity() = TareaEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    sectorId = sectorId,
    asignadoA = asignadoA,
    estado = estado,
    fechaProgramada = fechaProgramada,
    motivoRechazo = motivoRechazo,
    tipo = tipo,
    cultivoId = cultivoId
)

fun TareaEntity.toDomain() = Tarea(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    sectorId = sectorId,
    asignadoA = asignadoA,
    estado = estado,
    fechaProgramada = fechaProgramada,
    motivoRechazo = motivoRechazo,
    tipo = tipo,
    cultivoId = cultivoId
)

fun Usuario.toEntity() = UsuarioEntity(
    id = id,
    nombre = nombre,
    correo = correo,
    perfil = perfil,
    tallerAsignado = tallerAsignado,
    apellido = apellido,
    passwordSalt = passwordSalt,
    passwordHash = passwordHash
)

fun UsuarioEntity.toDomain() = Usuario(
    id = id,
    nombre = nombre,
    correo = correo,
    perfil = perfil,
    tallerAsignado = tallerAsignado,
    apellido = apellido,
    passwordSalt = passwordSalt,
    passwordHash = passwordHash
)

fun Validacion.toEntity() = ValidacionEntity(
    id = id,
    tareaId = tareaId,
    administradorId = administradorId,
    resultado = resultado,
    observaciones = observaciones,
    fechaValidacion = fechaValidacion
)

fun ValidacionEntity.toDomain() = Validacion(
    id = id,
    tareaId = tareaId,
    administradorId = administradorId,
    resultado = resultado,
    observaciones = observaciones,
    fechaValidacion = fechaValidacion
)
