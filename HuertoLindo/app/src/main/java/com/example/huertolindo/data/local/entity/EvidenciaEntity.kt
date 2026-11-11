package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = TareaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tarea_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CultivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cultivo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReporteProblemaEntity::class,
            parentColumns = ["id"],
            childColumns = ["reporte_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tarea_id"), Index("cultivo_id"), Index("reporte_id")]
)
data class EvidenciaEntity(
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