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
import com.example.huertolindo.data.security.PermisosHuerto
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

    var listaCultivos = mutableStateOf(listOf<Cultivo>())
        private set

    private var listaTareas = mutableStateOf(listOf<Tarea>())

    val esAdministrador: Boolean
        get() = PermisosHuerto.esAdministrador(usuarioActual?.perfil)

    val tareasVisibles: List<Tarea>
        get() = PermisosHuerto.tareasVisibles(
            usuarioActual?.perfil,
            usuarioActual?.id,
            listaTareas.value
        )

    var usuariosDisponibles = mutableStateOf(listOf<Usuario>())
        private set

    var errorCargaUsuarios by mutableStateOf<String?>(null)
        private set

    init {
        cargarDatosFicticios()
    }

    private fun cargarDatosFicticios() {
        listaSectores.value = listOf(
            Sector(id = "1", nombre = "Sector Norte", descripcion = "Zona de hortalizas de hoja", estado = "En uso"),
            Sector(id = "2", nombre = "Sector Sur", descripcion = "Zona de raíces y tubérculos", estado = "Disponible")
        )

        listaCultivos.value = listOf(
            Cultivo("1", "Lechuga", "1", "2026-09-20", "En crecimiento", "Cada 2 días"),
            Cultivo("2", "Espinaca", "1", "2026-09-25", "En crecimiento", "Cada 2 días"),
            Cultivo("3", "Zanahoria", "2", "2026-09-10", "Desarrollo", "Cada 3 días")
        )

        listaTareas.value = listOf(
            Tarea(id = "1", titulo = "Regar cultivos", descripcion = "Aplicar agua en la base de cada planta", sectorId = "1", asignadoA = "student-vicente", estado = "Pendiente"),
            Tarea(id = "2", titulo = "Revisar zanahorias", descripcion = "Verificar humedad y retirar malezas", sectorId = "2", asignadoA = "student-matias", estado = "Pendiente")
        )
    }

    fun enviarTareaARevision(tareaId: String): Boolean {
        val usuario = usuarioActual ?: return false
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (!PermisosHuerto.puedeEnviarTarea(usuario.perfil, usuario.id, tarea)) {
            return false
        }
        actualizarEstadoTarea(tareaId, "Por validar")
        return true
    }

    fun validarTarea(tareaId: String): Boolean {
        if (!esAdministrador) return false
        val tarea = listaTareas.value.firstOrNull { it.id == tareaId } ?: return false
        if (tarea.estado != "Por validar") return false
        actualizarEstadoTarea(tareaId, "Completada")
        return true
    }

    fun crearTarea(titulo: String, descripcion: String, sectorId: String, usuarioId: String): Boolean {
        if (!esAdministrador ||
            titulo.isBlank() || descripcion.isBlank() ||
            listaSectores.value.none { it.id == sectorId } ||
            usuariosDisponibles.value.none { it.id == usuarioId && it.perfil == "estudiante" }
        ) return false

        listaTareas.value = listaTareas.value + Tarea(
            id = java.util.UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descripcion = descripcion.trim(),
            sectorId = sectorId,
            asignadoA = usuarioId,
            estado = "Pendiente"
        )
        return true
    }

    fun agregarSector(nombre: String, descripcion: String): Boolean {
        if (!esAdministrador || nombre.isBlank() || descripcion.isBlank()) return false
        listaSectores.value = listaSectores.value + Sector(
            id = java.util.UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            estado = "Disponible"
        )
        return true
    }

    fun eliminarSector(sectorId: String): Boolean {
        if (!esAdministrador ||
            listaCultivos.value.any { it.sectorId == sectorId } ||
            listaTareas.value.any { it.sectorId == sectorId }
        ) return false
        val sectoresActualizados = listaSectores.value.filterNot { it.id == sectorId }
        if (sectoresActualizados.size == listaSectores.value.size) return false
        listaSectores.value = sectoresActualizados
        return true
    }

    fun agregarCultivo(
        nombre: String,
        sectorId: String,
        fechaSiembra: String,
        frecuenciaRiego: String
    ): Boolean {
        if (!esAdministrador || nombre.isBlank() ||
            fechaSiembra.isBlank() || frecuenciaRiego.isBlank() ||
            listaSectores.value.none { it.id == sectorId }
        ) return false

        listaCultivos.value = listaCultivos.value + Cultivo(
            id = java.util.UUID.randomUUID().toString(),
            nombreComun = nombre.trim(),
            sectorId = sectorId,
            fechaSiembra = fechaSiembra.trim(),
            estadoDesarrollo = "En crecimiento",
            frecuenciaRiego = frecuenciaRiego.trim()
        )
        return true
    }

    fun eliminarCultivo(cultivoId: String): Boolean {
        if (!esAdministrador) return false
        val cultivosActualizados = listaCultivos.value.filterNot { it.id == cultivoId }
        if (cultivosActualizados.size == listaCultivos.value.size) return false
        listaCultivos.value = cultivosActualizados
        return true
    }

    fun cargarUsuariosDisponibles() {
        if (!esAdministrador) return
        viewModelScope.launch {
            try {
                usuariosDisponibles.value = withContext(Dispatchers.IO) {
                    usuarioDatabase.obtenerUsuarios()
                }
                errorCargaUsuarios = null
            } catch (exception: SQLiteException) {
                errorCargaUsuarios = "No se pudieron cargar los estudiantes desde la base de datos."
            }
        }
    }

    private fun actualizarEstadoTarea(tareaId: String, estado: String) {
        listaTareas.value = listaTareas.value.map { tarea ->
            if (tarea.id == tareaId) tarea.copy(estado = estado) else tarea
        }
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
        usuariosDisponibles.value = emptyList()
        errorCargaUsuarios = null
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