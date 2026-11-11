package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.huertolindo.domain.model.TipoTarea

@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(
            entity = SectorEntity::class,
            parentColumns = ["id"],
            childColumns = ["sector_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TareaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "sector_id") val sectorId: String,
    @ColumnInfo(name = "usuario_id") val asignadoA: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "fecha_programada") val fechaProgramada: String = "",
    @ColumnInfo(name = "motivo_rechazo") val motivoRechazo: String = "",
    @ColumnInfo(name = "tipo", defaultValue = "'Mantenimiento'")
    val tipo: String = TipoTarea.MANTENIMIENTO,
    @ColumnInfo(name = "cultivo_id") val cultivoId: String? = null
)