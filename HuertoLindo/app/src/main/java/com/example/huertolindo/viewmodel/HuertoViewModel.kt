package com.example.huertolindo.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.huertolindo.data.model.*

class HuertoViewModel : ViewModel() {

    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    var fotoPerfilUri by mutableStateOf<Uri?>(null)
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
            Sector(id = "1", nombre = "Sector Norte", descripcion = "Zona de hortalizas de hoja", estado = "En uso"),
            Sector(id = "2", nombre = "Sector Sur", descripcion = "Zona de raíces y tubérculos", estado = "Disponible")
        )

        listaTareas.value = listOf(
            Tarea(id = "1", titulo = "Regar tomates", descripcion = "Aplicar 500ml de agua en la base", sectorId = "1", asignadoA = "estudiante1", estado = "Pendiente")
        )
    }

    fun iniciarSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

    fun actualizarFotoPerfil(uri: Uri?) {
        fotoPerfilUri = uri
    }
}