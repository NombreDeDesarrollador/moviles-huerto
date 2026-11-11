package com.example.huertolindo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sectores")
data class SectorEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "ubicacion", defaultValue = "''") val ubicacion: String = ""
)