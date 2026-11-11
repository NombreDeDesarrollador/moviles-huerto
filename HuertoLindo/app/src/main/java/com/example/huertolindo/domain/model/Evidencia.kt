package com.example.huertolindo.domain.model


data class Evidencia(
    val id: String,
    val tareaId: String? = null,
    val cultivoId: String? = null,
    val reporteId: String? = null,
    val tipo: String,
    val rutaArchivo: String,
    val descripcion: String = "",
    val fechaRegistro: String = ""
)

enum class TipoAsociacionEvidencia {
    TAREA,
    CULTIVO,
    REPORTE
}