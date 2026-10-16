package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cultivos",
    foreignKeys = [
        ForeignKey(
            entity = Sector::class,
            parentColumns = ["id"],
            childColumns = ["sector_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Cultivo(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre_comun") val nombreComun: String,
    @ColumnInfo(name = "sector_id") val sectorId: String,
    @ColumnInfo(name = "fecha_siembra") val fechaSiembra: String,
    @ColumnInfo(name = "estado_desarrollo") val estadoDesarrollo: String,
    @ColumnInfo(name = "frecuencia_riego") val frecuenciaRiego: String
)