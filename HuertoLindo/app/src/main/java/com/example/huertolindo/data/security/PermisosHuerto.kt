package com.example.huertolindo.data.security

import com.example.huertolindo.data.model.Tarea

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

    fun puedeIniciarTarea(perfil: String, usuarioId: String, tarea: Tarea): Boolean =
        perfil == ESTUDIANTE &&
            tarea.asignadoA == usuarioId &&
            tarea.estado in setOf("Pendiente", "Rechazada")

    fun puedeValidarTarea(perfil: String?, estado: String): Boolean =
        esAdministrador(perfil) && estado == "Por validar"

    fun puedeRechazarTarea(perfil: String?, estado: String, motivo: String): Boolean =
        puedeValidarTarea(perfil, estado) && motivo.isNotBlank()

    fun tareasAtrasadas(tareas: List<Tarea>, fechaActual: String): List<Tarea> =
        tareas.filter { tarea ->
            tarea.estado != "Completada" &&
                tarea.fechaProgramada.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                tarea.fechaProgramada < fechaActual
        }
}
