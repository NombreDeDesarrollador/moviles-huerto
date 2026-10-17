package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sectores")
data class Sector(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "descripcion") val descripcion: String,
    @ColumnInfo(name = "estado") val estado: String,
    @ColumnInfo(name = "ubicacion", defaultValue = "''") val ubicacion: String = ""
)