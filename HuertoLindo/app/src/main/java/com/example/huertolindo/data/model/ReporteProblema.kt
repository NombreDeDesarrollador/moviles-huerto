package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reporte_problemas",
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sector_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cultivo::class,
            parentColumns = ["id"],
            childColumns = ["cultivo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sector_id"), Index("cultivo_id"), Index("usuario_id")]
)
data class ReporteProblema(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sector_id") val sectorId: String,
    @ColumnInfo(name = "cultivo_id") val cultivoId: String? = null,
    @ColumnInfo(name = "usuario_id") val usuarioId: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "prioridad") val prioridad: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "fecha_reporte") val fechaReporte: String = ""
)