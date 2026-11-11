package com.example.huertolindo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.huertolindo.presentation.components.FotoScreen
import com.example.huertolindo.presentation.screens.AdminDashboardScreen
import com.example.huertolindo.presentation.screens.RegistroAdministradorScreen
import com.example.huertolindo.presentation.screens.EstudianteDashboardScreen
import com.example.huertolindo.presentation.screens.DetalleCultivoScreen
import com.example.huertolindo.presentation.screens.DetalleTareaScreen
import com.example.huertolindo.presentation.screens.DetalleSectorScreen
import com.example.huertolindo.presentation.screens.HistorialActividadesScreen
import com.example.huertolindo.presentation.screens.LoginScreen
import com.example.huertolindo.presentation.screens.ReportesProblemasScreen
import com.example.huertolindo.presentation.screens.ReportesScreen
import com.example.huertolindo.presentation.screens.RegistroScreen
import com.example.huertolindo.presentation.screens.SectoresScreen
import com.example.huertolindo.presentation.screens.SensoresScreen
import com.example.huertolindo.presentation.screens.TareasScreen
import com.example.huertolindo.presentation.theme.HuertoLindoTheme
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

class MainActivity : ComponentActivity() {

    // Inicializamos el ViewModel
    private val huertoViewModel: HuertoViewModel by viewModels {
        HuertoViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HuertoLindoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val huertoState by huertoViewModel.uiState.collectAsState()
                    var pantallaActual by rememberSaveable { mutableStateOf("login") }
                    var mensajeRegistro by rememberSaveable { mutableStateOf<String?>(null) }
                    var sectorSeleccionado by rememberSaveable { mutableStateOf("") }
                    var cultivoSeleccionado by rememberSaveable { mutableStateOf("") }
                    var tareaSeleccionada by rememberSaveable { mutableStateOf("") }

                    Crossfade(targetState = pantallaActual, label = "screen-navigation") { pantalla ->
                    when {
                        pantalla !in setOf("login", "registro") &&
                            huertoState.usuarioActual == null -> {
                            TextButton(onClick = { pantallaActual = "login" }) {
                                Text("Sesión no iniciada · Volver al inicio")
                            }
                        }
                        pantalla == "administradores" &&
                            !PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil) -> {
                            TextButton(onClick = { pantallaActual = "menu" }) {
                                Text("Acceso no autorizado · Volver")
                            }
                        }
                        pantalla == "reportes" &&
                            huertoState.usuarioActual?.perfil != "administrador" -> {
                            TextButton(onClick = { pantallaActual = "menu" }) {
                                Text("Acceso no autorizado · Volver")
                            }
                        }
                        else -> when (pantalla) {
                        "login" -> {
                            LoginScreen(
                                viewModel = huertoViewModel,
                                mensajeInformativo = mensajeRegistro,
                                onRegisterClick = {
                                    mensajeRegistro = null
                                    pantallaActual = "registro"
                                },
                                onLoginSuccess = {
                                    huertoViewModel.cargarReportesProblema()
                                    huertoViewModel.cargarUsuariosDisponibles()
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
                            if (huertoState.usuarioActual?.perfil == "administrador") {
                                AdminDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" },
                                    onSectorsClick = { pantallaActual = "sectores" },
                                    onTasksClick = { pantallaActual = "tareas" },
                                    onSensorsClick = { pantallaActual = "sensores" },
                                    onReportsClick = { pantallaActual = "reportes" },
                                    onProblemsClick = { pantallaActual = "problemas" },
                                    onHistoryClick = { pantallaActual = "historial" },
                                    onAdministratorsClick = { pantallaActual = "administradores" }
                                )
                            } else {
                                EstudianteDashboardScreen(
                                    viewModel = huertoViewModel,
                                    onProfileClick = { pantallaActual = "perfil" },
                                    onSectorsClick = { pantallaActual = "sectores" },
                                    onTasksClick = { pantallaActual = "tareas" },
                                    onSensorsClick = { pantallaActual = "sensores" },
                                    onProblemsClick = { pantallaActual = "problemas" },
                                    onHistoryClick = { pantallaActual = "historial" }
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
                        "administradores" -> RegistroAdministradorScreen(
                            viewModel = huertoViewModel,
                            onBack = { pantallaActual = "menu" },
                            onRegistrationSuccess = { pantallaActual = "menu" }
                        )
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
                        "problemas" -> ReportesProblemasScreen(
                            viewModel = huertoViewModel,
                            onBack = { pantallaActual = "menu" }
                        )
                        "historial" -> HistorialActividadesScreen(
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
}