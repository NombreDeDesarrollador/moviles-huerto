package com.example.huertolindo.domain.usecase

import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PasswordHasher
import com.example.huertolindo.domain.security.PermisosHuerto
import java.util.Locale
import java.util.UUID

class AuthUseCase(
    private val repository: HuertoRepository,
    private val sessionManager: SessionManager? = null
) {
    suspend fun autenticar(correo: String, password: String): Usuario? {
        val normalizado = correo.trim().lowercase(Locale.ROOT)
        val usuario = repository.autenticar(normalizado, password)
        if (usuario != null) {
            sessionManager?.guardarSesion(usuario)
        }
        return usuario
    }

    suspend fun registrarUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        tallerAsignado: String = "General"
    ): Usuario {
        val salt = PasswordHasher.createSalt()
        val usuario = Usuario(
            id = UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            correo = correo.trim().lowercase(Locale.ROOT),
            perfil = PermisosHuerto.ESTUDIANTE,
            tallerAsignado = tallerAsignado.trim(),
            apellido = apellido.trim(),
            passwordSalt = PasswordHasher.encodeBase64(salt),
            passwordHash = PasswordHasher.encodeBase64(PasswordHasher.hash(password, salt))
        )
        repository.registrarUsuario(usuario, password)
        return usuario
    }

    suspend fun registrarAdministrador(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        administradorActualId: String
    ): Boolean {
        val salt = PasswordHasher.createSalt()
        val nuevoAdmin = Usuario(
            id = UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            correo = correo.trim().lowercase(Locale.ROOT),
            perfil = PermisosHuerto.ADMINISTRADOR,
            tallerAsignado = "General",
            apellido = apellido.trim(),
            passwordSalt = PasswordHasher.encodeBase64(salt),
            passwordHash = PasswordHasher.encodeBase64(PasswordHasher.hash(password, salt))
        )
        return repository.registrarAdministrador(nuevoAdmin, administradorActualId, password)
    }

    fun obtenerSesionActiva(): Usuario? = sessionManager?.obtenerUsuarioSesion()

    fun cerrarSesion() {
        sessionManager?.cerrarSesion()
    }
}
