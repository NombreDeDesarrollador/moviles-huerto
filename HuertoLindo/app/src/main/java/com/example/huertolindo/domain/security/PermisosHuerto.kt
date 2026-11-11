package com.example.huertolindo.domain.security

import com.example.huertolindo.domain.model.Tarea

object PermisosHuerto {
    const val ADMINISTRADOR = "administrador"
    const val ESTUDIANTE = "estudiante"

    fun esAdministrador(perfil: String?): Boolean = perfil == ADMINISTRADOR

    fun tareasVisibles(perfil: String?, usuarioId: String?, tareas: List<Tarea>): List<Tarea> =
        when {
            esAdministrador(perfil) -> tareas
            perfil == ESTUDIANTE && usuarioId != null ->
                tareas.filter { it.asignadoA == usuarioId }
            else -> emptyList()
        }

    fun puedeEnviarTarea(perfil: String, usuarioId: String, tarea: Tarea): Boolean =
        perfil == ESTUDIANTE &&
            tarea.asignadoA == usuarioId &&
            tarea.estado == "En desarrollo"

    fun puedeAdjuntarEvidenciaTarea(
        perfil: String?,
        usuarioId: String?,
        tarea: Tarea
    ): Boolean =
        perfil == ESTUDIANTE &&
            usuarioId != null &&
            tarea.asignadoA == usuarioId &&
            tarea.estado in setOf("En desarrollo", "Rechazada")

    fun puedeEliminarEvidenciaTarea(
        perfil: String?,
        usuarioId: String?,
        tarea: Tarea
    ): Boolean =
        perfil == ESTUDIANTE &&
            usuarioId != null &&
            tarea.asignadoA == usuarioId

    fun puedeIniciarTarea(perfil: String, usuarioId: String, tarea: Tarea): Boolean =
        perfil == ESTUDIANTE &&
            tarea.asignadoA == usuarioId &&
            tarea.estado in setOf("Pendiente", "Rechazada")

    fun puedeValidarTarea(perfil: String?, estado: String): Boolean =
        esAdministrador(perfil) && estado == "Por validar"

    fun puedeValidarTarea(perfil: String?, administradorId: String, tarea: Tarea): Boolean =
        puedeValidarTarea(perfil, tarea.estado) && administradorId != tarea.asignadoA

    fun puedeRechazarTarea(perfil: String?, estado: String, motivo: String): Boolean =
        puedeValidarTarea(perfil, estado) && motivo.isNotBlank()

    fun puedeActualizarEstadoProblema(perfil: String?, nuevoEstado: String): Boolean =
        esAdministrador(perfil) &&
            nuevoEstado in setOf("Informado", "En revisión", "Atendido", "Cerrado")

    fun tareasAtrasadas(tareas: List<Tarea>, fechaActual: String): List<Tarea> =
        tareas.filter { tarea ->
            tarea.estado !in setOf("Completada", "Validada") &&
                ReglasHuerto.esFechaIsoValida(tarea.fechaProgramada) &&
                tarea.fechaProgramada < fechaActual
        }

    fun esTareaCompletada(estado: String): Boolean =
        estado == "Completada" || estado == "Validada"
}
