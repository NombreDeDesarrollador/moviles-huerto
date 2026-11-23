package com.example.huertolindo.presentation.viewmodel

import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.security.PermisosHuerto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun HuertoViewModel.esAdministradorCalculado(): Boolean =
    PermisosHuerto.esAdministrador(usuarioActual?.perfil)

internal fun HuertoViewModel.tareasVisiblesCalculadas(): List<Tarea> =
    PermisosHuerto.tareasVisibles(
        usuarioActual?.perfil,
        usuarioActual?.id,
        _listaTareas.value
    )

internal fun HuertoViewModel.tareasAtrasadasCalculadas(): List<Tarea> {
    val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    return PermisosHuerto.tareasAtrasadas(tareasVisiblesCalculadas(), hoy)
}

internal fun HuertoViewModel.cultivosConRiegoPendienteCalculados(): List<Cultivo> {
    val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    val fechaActual = parsearFechaHuerto(hoy) ?: return emptyList()
    return _listaCultivos.value.filter { cultivo ->
        val frecuencia = cultivo.frecuenciaRiego.toIntOrNull() ?: return@filter false
        val fechaSiembra = parsearFechaHuerto(cultivo.fechaSiembra) ?: return@filter false
        if (frecuencia <= 0 || fechaActual.before(fechaSiembra) ||
            cultivo.estadoDesarrollo == "Cosechado"
        ) return@filter false
        val dias =
            ((fechaActual.time - fechaSiembra.time) / HuertoViewModelConstants.MILLIS_POR_DIA).toInt()
        dias % frecuencia == 0
    }
}

internal fun HuertoViewModel.historialVisibleCalculado(): List<ActividadTarea> {
    if (esAdministradorCalculado()) return _historialActividades.value
    val usuarioId = usuarioActual?.id ?: return emptyList()
    return _historialActividades.value.filter { it.usuarioRelacionadoId == usuarioId }
}

internal fun HuertoViewModel.reportesProblemasVisiblesCalculados(): List<ReporteProblema> {
    if (esAdministradorCalculado()) return _reportesProblemas.value
    val usuarioId = usuarioActual?.id ?: return emptyList()
    return _reportesProblemas.value.filter { it.usuarioId == usuarioId }
}

internal fun parsearFechaHuerto(fecha: String): Date? =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }.parse(fecha)
