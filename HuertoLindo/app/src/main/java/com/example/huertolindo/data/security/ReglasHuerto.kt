package com.example.huertolindo.data.security

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

object ReglasHuerto {
    fun esFechaIsoValida(fecha: String): Boolean {
        if (!fecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        return formato.parse(fecha, posicion) != null && posicion.index == fecha.length
    }

    fun fechaCosechaValida(fechaSiembra: String, fechaCosechaEstimada: String): Boolean =
        esFechaIsoValida(fechaSiembra) &&
            esFechaIsoValida(fechaCosechaEstimada) &&
            fechaCosechaEstimada >= fechaSiembra
}
