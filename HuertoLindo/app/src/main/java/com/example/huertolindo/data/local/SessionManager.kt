package com.example.huertolindo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.huertolindo.data.remote.RetrofitClient
import com.example.huertolindo.domain.model.Usuario

class SessionManager(context: Context) {
    private val preferences: SharedPreferences? = try {
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    } catch (_: Throwable) {
        null
    }

    fun guardarSesion(usuario: Usuario) {
        try {
            preferences?.edit()
                ?.putString(KEY_USER_ID, usuario.id)
                ?.putString(KEY_USER_CORREO, usuario.correo)
                ?.putString(KEY_USER_NOMBRE, usuario.nombre)
                ?.putString(KEY_USER_APELLIDO, usuario.apellido)
                ?.putString(KEY_USER_PERFIL, usuario.perfil)
                ?.putString(KEY_USER_TALLER, usuario.tallerAsignado)
                ?.putBoolean(KEY_SESSION_ACTIVE, true)
                ?.apply()
        } catch (_: Throwable) {}
    }

    fun obtenerUsuarioSesion(): Usuario? {
        return try {
            val prefs = preferences ?: return null
            if (!prefs.getBoolean(KEY_SESSION_ACTIVE, false)) return null
            val id = prefs.getString(KEY_USER_ID, null) ?: return null
            val correo = prefs.getString(KEY_USER_CORREO, "") ?: ""
            val nombre = prefs.getString(KEY_USER_NOMBRE, "") ?: ""
            val apellido = prefs.getString(KEY_USER_APELLIDO, "") ?: ""
            val perfil = prefs.getString(KEY_USER_PERFIL, "estudiante") ?: "estudiante"
            val taller = prefs.getString(KEY_USER_TALLER, "General") ?: "General"

            Usuario(
                id = id,
                nombre = nombre,
                apellido = apellido,
                correo = correo,
                perfil = perfil,
                tallerAsignado = taller
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun cerrarSesion() {
        try {
            preferences?.edit()?.clear()?.apply()
        } catch (_: Throwable) {}
        RetrofitClient.clearSessionCookies()
    }

    fun haySesionActiva(): Boolean = try {
        preferences?.getBoolean(KEY_SESSION_ACTIVE, false) ?: false
    } catch (_: Throwable) {
        false
    }

    companion object {
        private const val PREFS_NAME = "huerto_lindo_session_prefs"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_USER_ID = "session_user_id"
        private const val KEY_USER_CORREO = "session_user_correo"
        private const val KEY_USER_NOMBRE = "session_user_nombre"
        private const val KEY_USER_APELLIDO = "session_user_apellido"
        private const val KEY_USER_PERFIL = "session_user_perfil"
        private const val KEY_USER_TALLER = "session_user_taller"
    }
}
