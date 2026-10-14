package com.tuempresa.huertoescolar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.huertolindo.data.model.Sector
import com.example.huertolindo.data.model.Tarea
import com.tuempresa.huertoescolar.data.model.*

class HuertoViewModel : ViewModel() {

    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    var listaSectores = mutableStateOf(listOf<Sector>())
        private set

    var listaTareas = mutableStateOf(listOf<Tarea>())
        private set

    init {
        cargarDatosFicticios()
    }

    private fun cargarDatosFicticios() {
        listaSectores.value = listOf(
            Sector("1", "Sector Norte", "Zona de hortalizas de hoja", "En uso"),
            Sector("2", "Sector Sur", "Zona de raíces y tubérculos", "Disponible")
        )

        listaTareas.value = listOf(
            Tarea("1", "Regar tomates", "Aplicar 500ml de agua en la base", "1", "estudiante1", "Pendiente")
        )
    }

    fun iniciarSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

}