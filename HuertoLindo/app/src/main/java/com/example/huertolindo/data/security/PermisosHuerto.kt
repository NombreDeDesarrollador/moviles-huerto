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
            tarea.estado == "Pendiente"
}
