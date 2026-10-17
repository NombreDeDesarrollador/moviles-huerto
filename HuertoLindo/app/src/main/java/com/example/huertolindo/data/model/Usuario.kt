package com.example.huertolindo.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)]
)
data class Usuario(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "correo") val correo: String,
    @ColumnInfo(name = "perfil") val perfil: String,
    @ColumnInfo(name = "taller_asignado") val tallerAsignado: String,
    @ColumnInfo(name = "apellido") val apellido: String = "",
    @ColumnInfo(name = "password_salt") val passwordSalt: String = "",
    @ColumnInfo(name = "password_hash") val passwordHash: String = ""
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellido).filter(String::isNotBlank).joinToString(" ")
}