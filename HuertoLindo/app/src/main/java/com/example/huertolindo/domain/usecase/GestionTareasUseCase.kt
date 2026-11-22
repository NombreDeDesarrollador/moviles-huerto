package com.example.huertolindo.domain.usecase

import android.net.Uri
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.security.ReglasHuerto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GestionTareasUseCase(
    private val repository: HuertoRepository
) {
    fun tareasVisibles(perfil: String?, usuarioId: String?, tareas: List<Tarea>): List<Tarea> =
        PermisosHuerto.tareasVisibles(perfil, usuarioId, tareas)

    fun tareasAtrasadas(tareas: List<Tarea>): List<Tarea> {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        return PermisosHuerto.tareasAtrasadas(tareas, hoy)
    }

    suspend fun crearTareas(
        titulo: String,
        descripcion: String,
        sector: Sector,
        cultivoId: String?,
        estudiantes: List<Usuario>,
        actor: Usuario,
        fechaProgramada: String,
        tipo: String
    ): List<Pair<Tarea, ActividadTarea>> {
        if (!PermisosHuerto.esAdministrador(actor.perfil) ||
            titulo.isBlank() || descripcion.isBlank() ||
            !ReglasHuerto.esFechaIsoValida(fechaProgramada) ||
            !TipoTarea.esValido(tipo) ||
            cultivoId.isNullOrBlank() ||
            estudiantes.isEmpty()
        ) return emptyList()

        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val resultado = mutableListOf<Pair<Tarea, ActividadTarea>>()

        for (estudiante in estudiantes) {
            val tarea = Tarea(
                id = UUID.randomUUID().toString(),
                titulo = titulo.trim(),
                descripcion = descripcion.trim(),
                sectorId = sector.id,
                asignadoA = estudiante.id,
                estado = "Pendiente",
                fechaProgramada = fechaProgramada.trim(),
                tipo = tipo,
                cultivoId = cultivoId
            )
            val actividad = ActividadTarea(
                id = UUID.randomUUID().toString(),
                tareaId = tarea.id,
                tituloTarea = tarea.titulo,
                usuarioRelacionadoId = estudiante.id,
                usuarioId = actor.id,
                nombreUsuario = actor.nombreCompleto,
                accion = "Creó y asignó la tarea",
                estado = "Pendiente",
                fecha = fechaHora
            )
            resultado.add(repository.crearTarea(tarea, sector, estudiante, actor, actividad))
        }
        return resultado
    }

    fun puedeEditarTarea(
        tarea: Tarea,
        titulo: String,
        descripcion: String,
        sector: Sector,
        cultivo: Cultivo?,
        estudiante: Usuario,
        actor: Usuario,
        fechaProgramada: String,
        tipo: String
    ): Boolean =
        PermisosHuerto.esAdministrador(actor.perfil) &&
            !PermisosHuerto.esTareaCompletada(tarea.estado) &&
            titulo.isNotBlank() &&
            descripcion.isNotBlank() &&
            ReglasHuerto.esFechaIsoValida(fechaProgramada) &&
            TipoTarea.esValido(tipo) &&
            estudiante.perfil.equals(PermisosHuerto.ESTUDIANTE, ignoreCase = true) &&
            cultivo?.sectorId == sector.id

    suspend fun editarTarea(
        tarea: Tarea,
        titulo: String,
        descripcion: String,
        sector: Sector,
        cultivo: Cultivo?,
        estudiante: Usuario,
        actor: Usuario,
        fechaProgramada: String,
        tipo: String
    ): Pair<Tarea, ActividadTarea>? {
        if (!puedeEditarTarea(
                tarea, titulo, descripcion, sector, cultivo,
                estudiante, actor, fechaProgramada, tipo
            )
        ) return null

        val actualizada = tarea.copy(
            titulo = titulo.trim(),
            descripcion = descripcion.trim(),
            sectorId = sector.id,
            cultivoId = cultivo?.id,
            asignadoA = estudiante.id,
            estado = "Pendiente",
            motivoRechazo = "",
            fechaProgramada = fechaProgramada.trim(),
            tipo = tipo
        )
        val actividad = ActividadTarea(
            id = UUID.randomUUID().toString(),
            tareaId = actualizada.id,
            tituloTarea = actualizada.titulo,
            usuarioRelacionadoId = estudiante.id,
            usuarioId = actor.id,
            nombreUsuario = actor.nombreCompleto,
            accion = "Editó la tarea",
            estado = actualizada.estado,
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        )
        repository.actualizarTarea(actualizada, sector, estudiante, actor, actividad)
        return actualizada to actividad
    }

    suspend fun iniciarTarea(tarea: Tarea, usuario: Usuario): Pair<Tarea, ActividadTarea>? {
        if (!PermisosHuerto.puedeIniciarTarea(usuario.perfil, usuario.id, tarea)) return null
        val actualizada = tarea.copy(estado = "En desarrollo")
        val actividad = ActividadTarea(
            id = UUID.randomUUID().toString(),
            tareaId = actualizada.id,
            tituloTarea = actualizada.titulo,
            usuarioRelacionadoId = usuario.id,
            usuarioId = usuario.id,
            nombreUsuario = usuario.nombreCompleto,
            accion = "Inició el trabajo de la tarea",
            estado = "En desarrollo",
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        )
        return actualizada to actividad
    }

    suspend fun entregarTarea(
        tarea: Tarea,
        comentario: String,
        imagenUri: Uri,
        estudiante: Usuario
    ): Triple<Tarea, Evidencia, ActividadTarea> {
        if (!PermisosHuerto.puedeEnviarTarea(estudiante.perfil, estudiante.id, tarea)) {
            throw IllegalStateException("No tienes permiso para entregar esta tarea.")
        }
        val evidencia = repository.guardarEvidenciaLocal(
            tarea.id,
            comentario.trim(),
            imagenUri.toString()
        )
        val tareaEntregada = tarea.copy(estado = "Por validar")
        val actividad = ActividadTarea(
            id = UUID.randomUUID().toString(),
            tareaId = tareaEntregada.id,
            tituloTarea = tareaEntregada.titulo,
            usuarioRelacionadoId = estudiante.id,
            usuarioId = estudiante.id,
            nombreUsuario = estudiante.nombreCompleto,
            accion = "Entregó la tarea con comentario y evidencia",
            estado = "Por validar",
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        )
        repository.entregarTarea(tareaEntregada, evidencia, estudiante, actividad)
        return Triple(tareaEntregada, evidencia, actividad)
    }

    suspend fun validarTarea(
        tarea: Tarea,
        administrador: Usuario
    ): Pair<Tarea, Validacion> {
        if (!PermisosHuerto.puedeValidarTarea(administrador.perfil, administrador.id, tarea)) {
            throw IllegalStateException("No tienes permiso para validar esta tarea.")
        }
        val actualizada = tarea.copy(estado = "Validada")
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val validacion = Validacion(
            id = UUID.randomUUID().toString(),
            tareaId = tarea.id,
            administradorId = administrador.id,
            resultado = "Validada",
            observaciones = "Aprobada por docente supervisor",
            fechaValidacion = fechaHora
        )
        val actividad = ActividadTarea(
            id = UUID.randomUUID().toString(),
            tareaId = actualizada.id,
            tituloTarea = actualizada.titulo,
            usuarioRelacionadoId = tarea.asignadoA,
            usuarioId = administrador.id,
            nombreUsuario = administrador.nombreCompleto,
            accion = "Validó la tarea",
            estado = "Validada",
            fecha = fechaHora
        )
        repository.validarTarea(actualizada, validacion, actividad, administrador)
        return actualizada to validacion
    }

    suspend fun rechazarTarea(
        tarea: Tarea,
        motivo: String,
        administrador: Usuario
    ): Pair<Tarea, Validacion> {
        if (!PermisosHuerto.puedeRechazarTarea(administrador.perfil, tarea.estado, motivo)) {
            throw IllegalStateException("Solo se puede rechazar una tarea pendiente de validación con motivo explícito.")
        }
        val actualizada = tarea.copy(estado = "Rechazada", motivoRechazo = motivo.trim())
        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val validacion = Validacion(
            id = UUID.randomUUID().toString(),
            tareaId = tarea.id,
            administradorId = administrador.id,
            resultado = "Rechazada",
            observaciones = motivo.trim(),
            fechaValidacion = fechaHora
        )
        val actividad = ActividadTarea(
            id = UUID.randomUUID().toString(),
            tareaId = actualizada.id,
            tituloTarea = actualizada.titulo,
            usuarioRelacionadoId = tarea.asignadoA,
            usuarioId = administrador.id,
            nombreUsuario = administrador.nombreCompleto,
            accion = "Rechazó la tarea: ${motivo.trim()}",
            estado = "Rechazada",
            fecha = fechaHora
        )
        repository.validarTarea(actualizada, validacion, actividad, administrador)
        return actualizada to validacion
    }
}
