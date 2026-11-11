package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actividades_tarea",
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
            childColumns = ["usuario_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuario_relacionado_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tarea_id"),
        Index("usuario_id"),
        Index("usuario_relacionado_id")
    ]
)
data class ActividadTareaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tarea_id") val tareaId: String,
    @ColumnInfo(name = "titulo_tarea") val tituloTarea: String,
    @ColumnInfo(name = "usuario_relacionado_id") val usuarioRelacionadoId: String,
    @ColumnInfo(name = "usuario_id") val usuarioId: String,
    @ColumnInfo(name = "nombre_usuario") val nombreUsuario: String,
    @ColumnInfo(name = "accion") val accion: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "fecha") val fecha: String
)
