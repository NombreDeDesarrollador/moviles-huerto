package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sector_id"],
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
data class Tarea(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "sector_id") val sectorId: String,
    @ColumnInfo(name = "usuario_id") val asignadoA: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "fecha_programada") val fechaProgramada: String = "",
    @ColumnInfo(name = "motivo_rechazo") val motivoRechazo: String = "",
    @ColumnInfo(name = "tipo", defaultValue = "'Mantenimiento'")
    val tipo: String = TipoTarea.MANTENIMIENTO
)