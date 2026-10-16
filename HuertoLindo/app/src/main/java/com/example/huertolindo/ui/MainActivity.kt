package com.example.huertolindo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.huertolindo.ui.FotoScreen
import com.example.huertolindo.ui.screens.AdminDashboardScreen
import com.example.huertolindo.ui.screens.EstudianteDashboardScreen
import com.example.huertolindo.ui.screens.LoginScreen
import com.example.huertolindo.ui.screens.RegistroScreen
import com.example.huertolindo.ui.theme.HuertoLindoTheme
import com.example.huertolindo.viewmodel.HuertoViewModel

class MainActivity : ComponentActivity() {

    // Inicializamos el ViewModel
    private val huertoViewModel: HuertoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertoLindoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var pantallaActual by remember { mutableStateOf("login") }
                    var perfilActual by remember { mutableStateOf("estudiante") }
                    var mensajeRegistro by remember { mutableStateOf<String?>(null) }

                    when (pantallaActual) {
                        "login" -> {
                            LoginScreen(
                                viewModel = huertoViewModel,
                                mensajeInformativo = mensajeRegistro,
                                onRegisterClick = {
                                    mensajeRegistro = null
                                    pantallaActual = "registro"
                                },
                                onLoginSuccess = { perfilSeleccionado ->
                                    perfilActual = perfilSeleccionado
                                    pantallaActual = "menu"
                                }
                            )
                        }
                        "registro" -> {
                            RegistroScreen(
                                viewModel = huertoViewModel,
                                onBackToLogin = { pantallaActual = "login" },
                                onRegistrationSuccess = {
                                    mensajeRegistro = "Cuenta creada. Ahora puedes iniciar sesión."
                                    pantallaActual = "login"
                                }
                            )
                        }
                        "menu" -> {
                            if (perfilActual == "administrador") {
                                AdminDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" }
                                )
                            } else {
                                EstudianteDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" }
                                )
                            }
                        }
                        "perfil" -> {
                            FotoScreen(
                                nombreUsuario = huertoViewModel.usuarioActual?.nombreCompleto ?: "Usuario",
                                fotoPerfil = huertoViewModel.fotoPerfilUri,
                                onFotoPerfilChange = huertoViewModel::actualizarFotoPerfil,
                                onVolver = { pantallaActual = "menu" },
                                onCerrarSesion = {
                                    huertoViewModel.cerrarSesion()
                                    perfilActual = "estudiante"
                                    mensajeRegistro = null
                                    pantallaActual = "login"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}