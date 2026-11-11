package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "validaciones",
    foreignKeys = [
        ForeignKey(
            entity = TareaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tarea_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["administrador_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ValidacionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tarea_id") val tareaId: String,
    @ColumnInfo(name = "administrador_id") val administradorId: String,
    @ColumnInfo(name = "resultado") val resultado: String,
    @ColumnInfo(name = "observaciones") val observaciones: String = "",
    @ColumnInfo(name = "fecha_validacion") val fechaValidacion: String = ""
)