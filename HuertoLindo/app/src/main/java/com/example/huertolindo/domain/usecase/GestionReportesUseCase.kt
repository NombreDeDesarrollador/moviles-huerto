package com.example.huertolindo.domain.usecase

import android.net.Uri
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GestionReportesUseCase(
    private val repository: HuertoRepository
) {
    suspend fun crearReporteProblema(
        descripcion: String,
        sector: Sector,
        cultivo: Cultivo?,
        tipoProblema: String,
        usuario: Usuario,
        fotografia: Uri
    ): ReporteProblema {
        val reporte = ReporteProblema(
            id = UUID.randomUUID().toString(),
            sectorId = sector.id,
            cultivoId = cultivo?.id,
            usuarioId = usuario.id,
            descripcion = descripcion.trim(),
            prioridad = "Media",
            estado = "Informado",
            fechaReporte = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()),
            tipoProblema = tipoProblema
        )
        repository.insertarReporte(reporte, sector, cultivo, usuario, fotografia)
        return reporte
    }

    suspend fun actualizarEstadoProblema(
        reporte: ReporteProblema,
        nuevoEstado: String,
        comentario: String,
        administrador: Usuario
    ): ReporteProblema {
        if (!PermisosHuerto.puedeActualizarEstadoProblema(administrador.perfil, nuevoEstado)) {
            throw IllegalStateException("Solo el administrador puede cambiar a un estado válido de problema.")
        }
        val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val seguimientoNuevo = listOf(
            reporte.comentarioSeguimiento,
            "$ahora · ${administrador.nombreCompleto}: ${comentario.trim()}"
        ).filter(String::isNotBlank).joinToString("\n")

        val actualizado = reporte.copy(
            estado = nuevoEstado,
            comentarioSeguimiento = seguimientoNuevo,
            fechaResolucion = if (nuevoEstado in setOf("Atendido", "Cerrado")) ahora else ""
        )
        repository.actualizarReporte(actualizado)
        return actualizado
    }

    suspend fun obtenerReportes(usuario: Usuario): List<ReporteProblema> {
        val filtro = if (PermisosHuerto.esAdministrador(usuario.perfil)) null else usuario.id
        return repository.obtenerReportesProblema(filtro)
    }
}
