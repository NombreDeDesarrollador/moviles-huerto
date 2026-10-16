package com.example.huertolindo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.huertolindo.data.model.Cultivo
import kotlinx.coroutines.flow.Flow

@Dao
interface CultivoDao {
    @Insert
    fun insertar(cultivo: Cultivo) // Función normal, sin suspend

    @Query("SELECT * FROM cultivos")
    fun obtenerCultivos(): Flow<List<Cultivo>>
}