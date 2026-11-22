package com.example.huertolindo.domain.usecase

import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.security.ReglasHuerto
import java.util.UUID

class GestionSectoresUseCase(
    private val repository: HuertoRepository
) {
    fun estadoValido(estado: String, inicio: String, termino: String): Boolean =
        when (estado) {
            "En mantenimiento" -> ReglasHuerto.fechaCosechaValida(inicio, termino)
            "Disponible", "En uso", "Inactivo" -> true
            else -> false
        }

    fun puedeEliminar(
        sectorId: String,
        cultivos: List<Cultivo>,
        tareas: List<Tarea>,
        reportes: List<ReporteProblema>
    ): Boolean =
        cultivos.none { it.sectorId == sectorId } &&
            tareas.none { it.sectorId == sectorId } &&
            reportes.none { it.sectorId == sectorId }

    suspend fun agregarSector(
        nombre: String,
        ubicacion: String,
        descripcion: String,
        estado: String,
        inicioMantenimiento: String,
        terminoMantenimiento: String,
        actorPerfil: String?
    ): Sector? {
        if (!PermisosHuerto.esAdministrador(actorPerfil) ||
            nombre.isBlank() ||
            !estadoValido(estado, inicioMantenimiento, terminoMantenimiento)
        ) return null

        val sector = Sector(
            id = UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            ubicacion = ubicacion.trim(),
            descripcion = descripcion.trim(),
            estado = estado,
            fechaInicioMantenimiento = inicioMantenimiento.trim().takeIf { estado == "En mantenimiento" }.orEmpty(),
            fechaTerminoMantenimiento = terminoMantenimiento.trim().takeIf { estado == "En mantenimiento" }.orEmpty()
        )
        return repository.guardarSector(sector)
    }

    suspend fun editarSector(
        sectorId: String,
        nombre: String,
        ubicacion: String,
        descripcion: String,
        estado: String,
        inicioMantenimiento: String,
        terminoMantenimiento: String,
        actorPerfil: String?
    ): Sector? {
        if (!PermisosHuerto.esAdministrador(actorPerfil) ||
            nombre.isBlank() ||
            !estadoValido(estado, inicioMantenimiento, terminoMantenimiento)
        ) return null

        val sector = Sector(
            id = sectorId,
            nombre = nombre.trim(),
            ubicacion = ubicacion.trim(),
            descripcion = descripcion.trim(),
            estado = estado,
            fechaInicioMantenimiento = inicioMantenimiento.trim().takeIf { estado == "En mantenimiento" }.orEmpty(),
            fechaTerminoMantenimiento = terminoMantenimiento.trim().takeIf { estado == "En mantenimiento" }.orEmpty()
        )
        repository.actualizarSector(sector)
        return sector
    }

    suspend fun eliminarSector(sector: Sector, actorPerfil: String?): Boolean {
        if (!PermisosHuerto.esAdministrador(actorPerfil)) return false
        repository.eliminarSector(sector)
        return true
    }
}
