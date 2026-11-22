package com.example.huertolindo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.huertolindo.data.remote.RetrofitClient
import com.example.huertolindo.data.repository.RoomHuertoRepository
import com.example.huertolindo.presentation.viewmodel.AuthViewModel
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel
import com.example.huertolindo.presentation.viewmodel.ReportesHistorialViewModel
import com.example.huertolindo.presentation.viewmodel.SectoresCultivosViewModel
import com.example.huertolindo.presentation.viewmodel.TareasViewModel

class HuertoViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    private val repository by lazy {
        RetrofitClient.initialize(application)
        RoomHuertoRepository(application, RetrofitClient.api)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(SectoresCultivosViewModel::class.java) -> {
                SectoresCultivosViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(TareasViewModel::class.java) -> {
                TareasViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(ReportesHistorialViewModel::class.java) -> {
                ReportesHistorialViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(HuertoViewModel::class.java) -> {
                HuertoViewModel(application, repository) as T
            }
            else -> throw IllegalArgumentException("ViewModel no soportado: ${modelClass.name}")
        }
    }
}
