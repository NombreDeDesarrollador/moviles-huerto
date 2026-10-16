package com.example.huertolindo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.huertolindo.data.AppDatabase
import com.example.huertolindo.data.CultivoRepository
import com.example.huertolindo.data.model.Cultivo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HuertoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = CultivoRepository(database.cultivoDao())

    // Estado observable para Jetpack Compose
    private val _uiState = MutableStateFlow(CultivoUiState())
    val uiState: StateFlow<CultivoUiState> = _uiState.asStateFlow()

    fun actualizarNombre(nuevoNombre: String) {
        _uiState.update { it.copy(nombre = nuevoNombre, errorNombre = false) }
    }

    fun actualizarSector(nuevoSector: String) {
        _uiState.update { it.copy(sector = nuevoSector, errorSector = false) }
    }

    fun registrarCultivo() {
        val estadoActual = _uiState.value

        // Validaciones simples
        val errorNombre = estadoActual.nombre.isBlank()
        val errorSector = estadoActual.sector.isBlank()

        if (errorNombre || errorSector) {
            _uiState.update {
                it.copy(errorNombre = errorNombre, errorSector = errorSector)
            }
        } else {
            val nuevoCultivo = Cultivo(
                nombre = estadoActual.nombre,
                sector = estadoActual.sector,
                fechaSiembra = "Por definir",
                estado = "Sembrado"
            )

            // Guardamos en la base de datos usando una corrutina
            viewModelScope.launch {
                repository.guardarCultivo(nuevoCultivo)
                // Limpiamos el formulario después de guardar
                _uiState.value = CultivoUiState()
            }
        }
    }
}