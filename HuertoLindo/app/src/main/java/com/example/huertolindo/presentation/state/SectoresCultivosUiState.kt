package com.example.huertolindo.presentation.state

import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.SensorSimulado

data class SectoresCultivosUiState(
    val sectores: List<Sector> = emptyList(),
    val cultivos: List<Cultivo> = emptyList(),
    val sensores: List<SensorSimulado> = emptyList(),
    val sectorSeleccionadoId: String? = null,
    val cultivoSeleccionadoId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
