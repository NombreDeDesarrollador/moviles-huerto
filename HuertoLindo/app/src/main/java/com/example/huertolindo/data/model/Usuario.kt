package com.example.huertolindo.data.model
data class Usuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val perfil: String,
    val tallerAsignado: String,
    val apellido: String = ""
) {
    val nombreCompleto: String
        get() = listOf(nombre, apellido).filter(String::isNotBlank).joinToString(" ")
}