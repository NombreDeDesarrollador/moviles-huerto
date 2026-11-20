package com.example.huertolindo.presentation.state

import android.net.Uri
import com.example.huertolindo.domain.model.Usuario

data class AuthUiState(
    val usuarioActual: Usuario? = null,
    val fotoPerfilUri: Uri? = null,
    val usuariosDisponibles: List<Usuario> = emptyList(),
    val errorCargaUsuarios: String? = null,
    val cargandoDatos: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
