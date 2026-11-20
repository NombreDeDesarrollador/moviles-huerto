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
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.screens.*
import com.example.huertolindo.presentation.theme.HuertoLindoTheme
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

class MainActivity : ComponentActivity() {

    // Inicializamos el ViewModel con la fábrica personalizada
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

                    // Si hay una sesión activa persistida y estamos en login, dirigir automáticamente al menú
                    LaunchedEffect(huertoState.usuarioActual) {
                        if (huertoState.usuarioActual != null && pantallaActual == "login") {
                            pantallaActual = "menu"
                        }
                    }

                    Crossfade(targetState = pantallaActual, label = "screen-navigation") { pantalla ->
                        when {
                            pantalla !in setOf("login", "registro") &&
                                huertoState.usuarioActual == null &&
                                !huertoState.cargandoDatos -> {
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
                                !PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil) -> {
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
                                            mensajeRegistro = "Cuenta creada exitosamente. Ahora puedes iniciar sesión."
                                            pantallaActual = "login"
                                        }
                                    )
                                }
                                "menu" -> {
                                    if (PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil)) {
                                        AdminDashboardScreen(
                                            viewModel = huertoViewModel,
                                            onProfileClick = { pantallaActual = "perfil" },
                                            onSectorsClick = { pantallaActual = "sectores" },
                                            onTasksClick = { pantallaActual = "tareas" },
                                            onSensorsClick = { pantallaActual = "sensores" },
                                            onReportsClick = { pantallaActual = "reportes" },
                                            onProblemsClick = { pantallaActual = "problemas" },
                                            onHistoryClick = { pantallaActual = "historial" },
                                            onAdministratorsClick = { pantallaActual = "administradores" },
                                            onCalendarClick = { pantallaActual = "calendario" },
                                            onGalleryClick = { pantallaActual = "galeria" }
                                        )
                                    } else {
                                        EstudianteDashboardScreen(
                                            viewModel = huertoViewModel,
                                            onProfileClick = { pantallaActual = "perfil" },
                                            onSectorsClick = { pantallaActual = "sectores" },
                                            onTasksClick = { pantallaActual = "tareas" },
                                            onSensorsClick = { pantallaActual = "sensores" },
                                            onProblemsClick = { pantallaActual = "problemas" },
                                            onHistoryClick = { pantallaActual = "historial" },
                                            onCalendarClick = { pantallaActual = "calendario" },
                                            onGalleryClick = { pantallaActual = "galeria" }
                                        )
                                    }
                                }
                                "perfil" -> {
                                    PerfilUsuarioScreen(
                                        viewModel = huertoViewModel,
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
                                "calendario" -> CalendarioHuertoScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { pantallaActual = "menu" }
                                )
                                "galeria" -> GaleriaHuertoScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { pantallaActual = "menu" }
                                )
                                "reportes" -> ReportesScreen(
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
