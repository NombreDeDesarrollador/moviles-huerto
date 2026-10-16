package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "evidencias",
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["tarea_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Evidencia(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "tarea_id") val tareaId: String,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "ruta_archivo") val rutaArchivo: String,
    @ColumnInfo(name = "descripcion") val descripcion: String = "",
    @ColumnInfo(name = "fecha_registro") val fechaRegistro: String = ""
)