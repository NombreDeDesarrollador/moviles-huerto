package com.example.huertolindo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.huertolindo.data.repository.RoomHuertoRepository
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

class HuertoViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(HuertoViewModel::class.java)) {
            throw IllegalArgumentException("ViewModel no soportado: ${modelClass.name}")
        }
        return modelClass.cast(
            HuertoViewModel(application, RoomHuertoRepository(application))
        ) ?: throw IllegalArgumentException("No se pudo crear ${modelClass.name}")
    }
}
