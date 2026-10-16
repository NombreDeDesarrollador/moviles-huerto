package com.example.huertolindo.data

import com.example.huertolindo.data.model.Cultivo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CultivoRepository(private val cultivoDao: CultivoDao) {

    suspend fun guardarCultivo(cultivo: Cultivo) {
        // Movemos la inserción al hilo de fondo (Input/Output)
        withContext(Dispatchers.IO) {
            cultivoDao.insertar(cultivo)
        }
    }

    fun obtenerTodosLosCultivos(): Flow<List<Cultivo>> {
        return cultivoDao.obtenerCultivos()
    }
}