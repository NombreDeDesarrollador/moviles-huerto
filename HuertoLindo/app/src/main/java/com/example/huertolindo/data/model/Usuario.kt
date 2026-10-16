package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "correo") val correo: String,
    @ColumnInfo(name = "perfil") val perfil: String,
    @ColumnInfo(name = "taller_asignado") val tallerAsignado: String,
    @ColumnInfo(name = "apellido") val apellido: String = ""
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellido).filter(String::isNotBlank).joinToString(" ")
}