package com.example.huertolindo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cultivos")
data class Cultivo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val sector: String,
    val fechaSiembra: String,
    val estado: String,
    val fotoUri: String? = null
)