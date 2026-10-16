package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reporte_problemas",
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["tarea_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReporteProblema(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tarea_id") val tareaId: String,
    @ColumnInfo(name = "usuario_id") val usuarioId: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "prioridad") val prioridad: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "fecha_reporte") val fechaReporte: String = ""
)