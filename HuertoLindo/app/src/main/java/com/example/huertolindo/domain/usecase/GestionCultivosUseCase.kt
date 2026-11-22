package com.example.huertolindo.domain.usecase

import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.EstadoCultivo
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.security.ReglasHuerto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GestionCultivosUseCase(
    private val repository: HuertoRepository
) {
    private val MILLIS_POR_DIA = 86_400_000L

    fun validarCultivo(
        nombre: String,
        fechaSiembra: String,
        fechaCosechaEstimada: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String,
        sectorExiste: Boolean,
        actorPerfil: String?
    ): Boolean {
        if (!PermisosHuerto.esAdministrador(actorPerfil) ||
            nombre.isBlank() ||
            !ReglasHuerto.fechaCosechaValida(fechaSiembra, fechaCosechaEstimada) ||
            !EstadoCultivo.esValido(estadoDesarrollo) ||
            !ReglasHuerto.frecuenciaRiegoValida(frecuenciaRiego) ||
            !sectorExiste
        ) return false
        return true
    }

    suspend fun agregarCultivo(
        nombre: String,
        sector: Sector,
        fechaSiembra: String,
        fechaCosechaEstimada: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String,
        actorPerfil: String?
    ): Cultivo? {
        if (!validarCultivo(nombre, fechaSiembra, fechaCosechaEstimada, estadoDesarrollo, frecuenciaRiego, true, actorPerfil)) {
            return null
        }
        val cultivo = Cultivo(
            id = UUID.randomUUID().toString(),
            nombreComun = nombre.trim(),
            sectorId = sector.id,
            fechaSiembra = fechaSiembra.trim(),
            fechaCosechaEstimada = fechaCosechaEstimada.trim(),
            estadoDesarrollo = estadoDesarrollo,
            frecuenciaRiego = frecuenciaRiego.trim()
        )
        return repository.guardarCultivo(cultivo, sector)
    }

    suspend fun editarCultivo(
        cultivoActual: Cultivo,
        nombre: String,
        fechaSiembra: String,
        fechaCosechaEstimada: String,
        estadoDesarrollo: String,
        frecuenciaRiego: String,
        actorPerfil: String?
    ): Cultivo? {
        if (!validarCultivo(nombre, fechaSiembra, fechaCosechaEstimada, estadoDesarrollo, frecuenciaRiego, true, actorPerfil)) {
            return null
        }
        val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val actualizado = cultivoActual.copy(
            nombreComun = nombre.trim(),
            fechaSiembra = fechaSiembra.trim(),
            fechaCosechaEstimada = fechaCosechaEstimada.trim(),
            estadoDesarrollo = estadoDesarrollo,
            frecuenciaRiego = frecuenciaRiego.trim(),
            fechaCambioEstado = if (cultivoActual.estadoDesarrollo != estadoDesarrollo) ahora else cultivoActual.fechaCambioEstado
        )
        repository.actualizarCultivo(actualizado)
        return actualizado
    }

    suspend fun eliminarCultivo(
        cultivo: Cultivo,
        tareas: List<Tarea>,
        reportes: List<ReporteProblema>,
        actorPerfil: String?
    ): Boolean {
        if (!PermisosHuerto.esAdministrador(actorPerfil) ||
            tareas.any { it.cultivoId == cultivo.id } ||
            reportes.any { it.cultivoId == cultivo.id }
        ) return false
        repository.eliminarCultivo(cultivo)
        return true
    }

    fun cultivosConRiegoPendiente(cultivos: List<Cultivo>): List<Cultivo> {
        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        val fechaActual = parsearFecha(hoy) ?: return emptyList()
        return cultivos.filter { cultivo ->
            val frecuencia = cultivo.frecuenciaRiego.toIntOrNull() ?: return@filter false
            val fechaSiembra = parsearFecha(cultivo.fechaSiembra) ?: return@filter false
            if (frecuencia <= 0 || fechaActual.before(fechaSiembra) ||
                cultivo.estadoDesarrollo in setOf("Cosechado", "Finalizado")
            ) return@filter false
            val dias = ((fechaActual.time - fechaSiembra.time) / MILLIS_POR_DIA).toInt()
            dias % frecuencia == 0
        }
    }

    fun fechaProximoRiego(cultivo: Cultivo): String? {
        val frecuencia = cultivo.frecuenciaRiego.toIntOrNull() ?: return null
        val fechaSiembra = parsearFecha(cultivo.fechaSiembra) ?: return null
        if (frecuencia <= 0 || cultivo.estadoDesarrollo in setOf("Cosechado", "Finalizado")) return null

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val hoy = formato.parse(formato.format(Date())) ?: return null
        if (hoy.before(fechaSiembra)) return cultivo.fechaSiembra

        val diasDesdeSiembra = ((hoy.time - fechaSiembra.time) / MILLIS_POR_DIA).toInt()
        val diasHastaProximo = (frecuencia - (diasDesdeSiembra % frecuencia)) % frecuencia
        val fechaProximo = Date(hoy.time + diasHastaProximo * MILLIS_POR_DIA)
        return formato.format(fechaProximo)
    }

    private fun parsearFecha(fecha: String): Date? =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(fecha)
        } catch (_: Exception) { null }
}
