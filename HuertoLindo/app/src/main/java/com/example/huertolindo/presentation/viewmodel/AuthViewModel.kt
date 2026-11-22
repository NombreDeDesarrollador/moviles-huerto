package com.example.huertolindo.presentation.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.domain.exception.CorreoYaRegistradoException
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.usecase.AuthUseCase
import com.example.huertolindo.presentation.state.AuthUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    application: Application,
    private val huertoRepository: HuertoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sessionManager: SessionManager = SessionManager(application),
    private val authUseCase: AuthUseCase = AuthUseCase(huertoRepository, sessionManager)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val usuarioActual: Usuario? get() = _uiState.value.usuarioActual
    val esAdministrador: Boolean get() = PermisosHuerto.esAdministrador(usuarioActual?.perfil)
    val fotoPerfilUri: Uri? get() = _uiState.value.fotoPerfilUri

    init {
        restaurarSesion()
        cargarUsuariosDisponibles()
    }

    fun restaurarSesion() {
        val sesion = authUseCase.obtenerSesionActiva()
        if (sesion != null) {
            _uiState.update { it.copy(usuarioActual = sesion) }
        }
    }

    fun iniciarSesion(
        correo: String,
        password: String,
        onResult: (ResultadoInicioSesion) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val resultado = try {
                val usuario = withContext(ioDispatcher) {
                    authUseCase.autenticar(correo, password)
                }
                usuario?.let { ResultadoInicioSesion.Exito(it) } ?: ResultadoInicioSesion.CredencialesInvalidas
            } catch (e: SQLiteException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (e: java.security.GeneralSecurityException) {
                ResultadoInicioSesion.ErrorDeBaseDeDatos
            } catch (e: Exception) {
                ResultadoInicioSesion.ErrorDelServidor(
                    e.message ?: "No se pudo completar el inicio de sesión con el servidor."
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }

            if (resultado is ResultadoInicioSesion.Exito) {
                _uiState.update { it.copy(usuarioActual = resultado.usuario, successMessage = "Sesión iniciada") }
            } else if (resultado is ResultadoInicioSesion.CredencialesInvalidas) {
                _uiState.update { it.copy(errorMessage = "Credenciales inválidas") }
            }
            onResult(resultado)
        }
    }

    fun registrarUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        tallerAsignado: String = "General",
        onResult: (ResultadoRegistro) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val resultado = try {
                withContext(ioDispatcher) {
                    authUseCase.registrarUsuario(nombre, apellido, correo, password, tallerAsignado)
                }
                ResultadoRegistro.Exito
            } catch (e: SQLiteConstraintException) {
                ResultadoRegistro.CorreoYaRegistrado
            } catch (e: CorreoYaRegistradoException) {
                ResultadoRegistro.CorreoYaRegistrado
            } catch (e: Exception) {
                ResultadoRegistro.ErrorDeBaseDeDatos
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
            onResult(resultado)
        }
    }

    fun registrarAdministrador(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        onResult: (ResultadoRegistroAdministrador) -> Unit
    ) {
        viewModelScope.launch {
            val adminId = usuarioActual?.takeIf { PermisosHuerto.esAdministrador(it.perfil) }?.id
            if (adminId == null) {
                onResult(ResultadoRegistroAdministrador.NoAutorizado)
                return@launch
            }
            _uiState.update { it.copy(isLoading = true) }
            val resultado = try {
                val ok = withContext(ioDispatcher) {
                    authUseCase.registrarAdministrador(nombre, apellido, correo, password, adminId)
                }
                if (ok) ResultadoRegistroAdministrador.Exito else ResultadoRegistroAdministrador.NoAutorizado
            } catch (e: SQLiteConstraintException) {
                ResultadoRegistroAdministrador.CorreoYaRegistrado
            } catch (e: CorreoYaRegistradoException) {
                ResultadoRegistroAdministrador.CorreoYaRegistrado
            } catch (e: Exception) {
                ResultadoRegistroAdministrador.ErrorDeBaseDeDatos
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
            onResult(resultado)
        }
    }

    fun actualizarFotoPerfil(uri: Uri?) {
        _uiState.update { it.copy(fotoPerfilUri = uri) }
    }

    fun cerrarSesion() {
        authUseCase.cerrarSesion()
        _uiState.update {
            it.copy(
                usuarioActual = null,
                fotoPerfilUri = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cargarUsuariosDisponibles() {
        viewModelScope.launch {
            try {
                val usuarios = withContext(ioDispatcher) { huertoRepository.obtenerUsuarios() }
                _uiState.update { it.copy(usuariosDisponibles = usuarios, errorCargaUsuarios = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorCargaUsuarios = "No se pudieron cargar los usuarios.") }
            }
        }
    }

    fun limpiarError() { _uiState.update { it.copy(errorMessage = null) } }
    fun limpiarExito() { _uiState.update { it.copy(successMessage = null) } }

    sealed interface ResultadoInicioSesion {
        data class Exito(val usuario: Usuario) : ResultadoInicioSesion
        data object CredencialesInvalidas : ResultadoInicioSesion
        data object ErrorDeBaseDeDatos : ResultadoInicioSesion
        data class ErrorDelServidor(val mensaje: String) : ResultadoInicioSesion
    }

    sealed interface ResultadoRegistro {
        data object Exito : ResultadoRegistro
        data object CorreoYaRegistrado : ResultadoRegistro
        data object ErrorDeBaseDeDatos : ResultadoRegistro
    }

    sealed interface ResultadoRegistroAdministrador {
        data object Exito : ResultadoRegistroAdministrador
        data object CorreoYaRegistrado : ResultadoRegistroAdministrador
        data object NoAutorizado : ResultadoRegistroAdministrador
        data object ErrorDeBaseDeDatos : ResultadoRegistroAdministrador
    }
}
