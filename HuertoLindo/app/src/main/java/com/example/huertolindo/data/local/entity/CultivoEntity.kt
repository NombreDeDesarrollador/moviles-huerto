package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cultivos",
    foreignKeys = [
        ForeignKey(
            entity = SectorEntity::class,
            parentColumns = ["id"],
            childColumns = ["sector_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CultivoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre_comun") val nombreComun: String,
    @ColumnInfo(name = "sector_id") val sectorId: String,
    @ColumnInfo(name = "fecha_siembra") val fechaSiembra: String,
    @ColumnInfo(name = "estado_desarrollo") val estadoDesarrollo: String,
    @ColumnInfo(name = "frecuencia_riego") val frecuenciaRiego: String,
    @ColumnInfo(name = "fecha_cosecha_estimada", defaultValue = "''")
    val fechaCosechaEstimada: String = "",
    @ColumnInfo(name = "fecha_cambio_estado", defaultValue = "''")
    val fechaCambioEstado: String = ""
)