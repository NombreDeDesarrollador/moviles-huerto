package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["tarea_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cultivo::class,
            parentColumns = ["id"],
            childColumns = ["cultivo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReporteProblema::class,
            parentColumns = ["id"],
            childColumns = ["reporte_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tarea_id"), Index("cultivo_id"), Index("reporte_id")]
)
data class Evidencia(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tarea_id") val tareaId: String? = null,
    @ColumnInfo(name = "cultivo_id") val cultivoId: String? = null,
    @ColumnInfo(name = "reporte_id") val reporteId: String? = null,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "ruta_archivo") val rutaArchivo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String = "",
    @ColumnInfo(name = "fecha_registro") val fechaRegistro: String = ""
)

enum class TipoAsociacionEvidencia {
    TAREA,
    CULTIVO,
    REPORTE
}