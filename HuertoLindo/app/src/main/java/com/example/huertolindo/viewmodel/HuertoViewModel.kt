package com.example.huertolindo.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.huertolindo.data.local.UsuarioDatabaseHelper
import com.example.huertolindo.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HuertoViewModel(application: Application) : AndroidViewModel(application) {
    private val usuarioDatabase = UsuarioDatabaseHelper(application)

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

    fun iniciarSesion(
        correo: String,
        password: String,
        onResult: (ResultadoInicioSesion) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    usuarioDatabase.autenticar(correo, password)
                }?.let { ResultadoInicioSesion.Exito(it) } ?: ResultadoInicioSesion.CredencialesInvalidas
            } catch (exception: SQLiteException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (exception: java.security.GeneralSecurityException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            }

            if (resultado is ResultadoInicioSesion.Exito) {
                usuarioActual = resultado.usuario
            }
            onResult(resultado)
        }
    }

    fun registrarUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        onResult: (ResultadoRegistro) -> Unit
    ) {
        viewModelScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    usuarioDatabase.registrar(nombre, apellido, correo, password)
                }
                ResultadoRegistro.Exito
            } catch (exception: SQLiteConstraintException) {
                ResultadoRegistro.CorreoYaRegistrado
            } catch (exception: SQLiteException) {
                ResultadoRegistro.ErrorDeBaseDeDatos
            } catch (exception: java.security.GeneralSecurityException) {
                ResultadoRegistro.ErrorDeBaseDeDatos
            }
            onResult(resultado)
        }
    }

    fun actualizarFotoPerfil(uri: Uri?) {
        fotoPerfilUri = uri
    }

    fun cerrarSesion() {
        usuarioActual = null
        fotoPerfilUri = null
    }

    override fun onCleared() {
        usuarioDatabase.close()
        super.onCleared()
    }

    sealed interface ResultadoInicioSesion {
        data class Exito(val usuario: Usuario) : ResultadoInicioSesion
        data object CredencialesInvalidas : ResultadoInicioSesion
        data object ErrorDeBaseDeDatos : ResultadoInicioSesion
    }

    sealed interface ResultadoRegistro {
        data object Exito : ResultadoRegistro
        data object CorreoYaRegistrado : ResultadoRegistro
        data object ErrorDeBaseDeDatos : ResultadoRegistro
    }
}