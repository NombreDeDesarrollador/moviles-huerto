package com.example.huertolindo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.huertolindo.ui.FotoScreen
import com.example.huertolindo.ui.screens.AdminDashboardScreen
import com.example.huertolindo.ui.screens.EstudianteDashboardScreen
import com.example.huertolindo.ui.screens.DetalleCultivoScreen
import com.example.huertolindo.ui.screens.DetalleTareaScreen
import com.example.huertolindo.ui.screens.DetalleSectorScreen
import com.example.huertolindo.ui.screens.LoginScreen
import com.example.huertolindo.ui.screens.ReportesScreen
import com.example.huertolindo.ui.screens.RegistroScreen
import com.example.huertolindo.ui.screens.SectoresScreen
import com.example.huertolindo.ui.screens.SensoresScreen
import com.example.huertolindo.ui.screens.TareasScreen
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
                    var mensajeRegistro by remember { mutableStateOf<String?>(null) }
                    var sectorSeleccionado by remember { mutableStateOf("") }
                    var cultivoSeleccionado by remember { mutableStateOf("") }
                    var tareaSeleccionada by remember { mutableStateOf("") }

                    when {
                        pantallaActual !in setOf("login", "registro") &&
                            huertoViewModel.usuarioActual == null -> {
                            TextButton(onClick = { pantallaActual = "login" }) {
                                Text("Sesión no iniciada · Volver al inicio")
                            }
                        }
                        pantallaActual == "reportes" && !huertoViewModel.esAdministrador -> {
                            TextButton(onClick = { pantallaActual = "menu" }) {
                                Text("Acceso no autorizado · Volver")
                            }
                        }
                        else -> when (pantallaActual) {
                        "login" -> {
                            LoginScreen(
                                viewModel = huertoViewModel,
                                mensajeInformativo = mensajeRegistro,
                                onRegisterClick = {
                                    mensajeRegistro = null
                                    pantallaActual = "registro"
                                },
                                onLoginSuccess = {
                                    if (huertoViewModel.esAdministrador) {
                                        huertoViewModel.cargarUsuariosDisponibles()
                                    }
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
                            if (huertoViewModel.esAdministrador) {
                                AdminDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" },
                                    onSectorsClick = { pantallaActual = "sectores" },
                                    onTasksClick = { pantallaActual = "tareas" },
                                    onSensorsClick = { pantallaActual = "sensores" },
                                    onReportsClick = { pantallaActual = "reportes" }
                                )
                            } else {
                                EstudianteDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" },
                                    onSectorsClick = { pantallaActual = "sectores" },
                                    onTasksClick = { pantallaActual = "tareas" },
                                    onSensorsClick = { pantallaActual = "sensores" }
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
                                    mensajeRegistro = null
                                    pantallaActual = "login"
                                }
                            )
                        }
                        "sectores" -> SectoresScreen(
                            viewModel = huertoViewModel,
                            onBack = { pantallaActual = "menu" },
                            onSectorClick = {
                                sectorSeleccionado = it
                                pantallaActual = "detalleSector"
                            }
                        )
                        "detalleSector" -> DetalleSectorScreen(
                            viewModel = huertoViewModel,
                            sectorId = sectorSeleccionado,
                            onBack = { pantallaActual = "sectores" },
                            onCultivoClick = {
                                cultivoSeleccionado = it
                                pantallaActual = "detalleCultivo"
                            }
                        )
                        "detalleCultivo" -> DetalleCultivoScreen(
                            viewModel = huertoViewModel,
                            cultivoId = cultivoSeleccionado,
                            onBack = { pantallaActual = "detalleSector" }
                        )
                        "tareas" -> TareasScreen(
                            viewModel = huertoViewModel,
                            onBack = { pantallaActual = "menu" },
                            onTaskClick = {
                                tareaSeleccionada = it
                                pantallaActual = "detalleTarea"
                            }
                        )
                        "detalleTarea" -> DetalleTareaScreen(
                            viewModel = huertoViewModel,
                            tareaId = tareaSeleccionada,
                            onBack = { pantallaActual = "tareas" }
                        )
                        "sensores" -> SensoresScreen(
                            viewModel = huertoViewModel,
                            onBack = { pantallaActual = "menu" }
                        )
                        "reportes" -> {
                            ReportesScreen(
                                viewModel = huertoViewModel,
                                onBack = { pantallaActual = "menu" }
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}