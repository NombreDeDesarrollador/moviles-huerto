package com.example.huertolindo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
                    LaunchedEffect(huertoState.successMessage) {
                        huertoState.successMessage?.let { message ->
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            huertoViewModel.limpiarExito()
                        }
                    }
                    var pantallaActual by rememberSaveable { mutableStateOf("login") }
                    var historialPantallas by rememberSaveable {
                        mutableStateOf(listOf("login"))
                    }
                    var sectorSeleccionado by rememberSaveable { mutableStateOf("") }
                    var cultivoSeleccionado by rememberSaveable { mutableStateOf("") }
                    var tareaSeleccionada by rememberSaveable { mutableStateOf("") }

                    fun navegarA(pantalla: String) {
                        if (pantalla == pantallaActual) return
                        historialPantallas = historialPantallas + pantalla
                        pantallaActual = pantalla
                    }

                    fun reemplazarPantalla(pantalla: String) {
                        historialPantallas = listOf(pantalla)
                        pantallaActual = pantalla
                    }

                    fun volverPantalla() {
                        if (historialPantallas.size <= 1) return
                        historialPantallas = historialPantallas.dropLast(1)
                        pantallaActual = historialPantallas.last()
                    }

                    BackHandler {
                        volverPantalla()
                    }

                    // Si hay una sesión activa persistida y estamos en login, dirigir automáticamente al menú
                    LaunchedEffect(huertoState.usuarioActual) {
                        if (huertoState.usuarioActual != null && pantallaActual == "login") {
                            reemplazarPantalla("menu")
                        }
                    }

                    Crossfade(targetState = pantallaActual, label = "screen-navigation") { pantalla ->
                        when {
                            pantalla !in setOf("login", "registro") &&
                                huertoState.usuarioActual == null &&
                                !huertoState.cargandoDatos -> {
                                TextButton(onClick = { reemplazarPantalla("login") }) {
                                    Text("Sesión no iniciada · Volver al inicio")
                                }
                            }
                            pantalla == "administradores" &&
                                !PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil) -> {
                                TextButton(onClick = { volverPantalla() }) {
                                    Text("Acceso no autorizado · Volver")
                                }
                            }
                            pantalla == "reportes" &&
                                !PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil) -> {
                                TextButton(onClick = { volverPantalla() }) {
                                    Text("Acceso no autorizado · Volver")
                                }
                            }
                            pantalla == "cultivos" &&
                                !PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil) -> {
                                TextButton(onClick = { volverPantalla() }) {
                                    Text("Acceso no autorizado · Volver")
                                }
                            }
                            else -> when (pantalla) {
                                "login" -> {
                                    LoginScreen(
                                        viewModel = huertoViewModel,
                                        onRegisterClick = { navegarA("registro") },
                                        onLoginSuccess = {
                                            huertoViewModel.cargarReportesProblema()
                                            huertoViewModel.cargarUsuariosDisponibles()
                                            reemplazarPantalla("menu")
                                        }
                                    )
                                }
                                "registro" -> {
                                    RegistroScreen(
                                        viewModel = huertoViewModel,
                                        onBackToLogin = { volverPantalla() },
                                        onRegistrationSuccess = { reemplazarPantalla("login") }
                                    )
                                }
                                "menu" -> {
                                    if (PermisosHuerto.esAdministrador(huertoState.usuarioActual?.perfil)) {
                                        AdminDashboardScreen(
                                            viewModel = huertoViewModel,
                                            onProfileClick = { navegarA("perfil") },
                                            onSectorsClick = { navegarA("sectores") },
                                            onCultivosClick = { navegarA("cultivos") },
                                            onTasksClick = { navegarA("tareas") },
                                            onSensorsClick = { navegarA("sensores") },
                                            onReportsClick = { navegarA("reportes") },
                                            onProblemsClick = { navegarA("problemas") },
                                            onHistoryClick = { navegarA("historial") },
                                            onAdministratorsClick = { navegarA("administradores") },
                                            onCalendarClick = { navegarA("calendario") },
                                            onGalleryClick = { navegarA("galeria") }
                                        )
                                    } else {
                                        EstudianteDashboardScreen(
                                            viewModel = huertoViewModel,
                                            onProfileClick = { navegarA("perfil") },
                                            onSectorsClick = { navegarA("sectores") },
                                            onTasksClick = { navegarA("tareas") },
                                            onSensorsClick = { navegarA("sensores") },
                                            onProblemsClick = { navegarA("problemas") },
                                            onHistoryClick = { navegarA("historial") },
                                            onCalendarClick = { navegarA("calendario") },
                                            onGalleryClick = { navegarA("galeria") }
                                        )
                                    }
                                }
                                "perfil" -> {
                                    PerfilUsuarioScreen(
                                        viewModel = huertoViewModel,
                                        onVolver = { volverPantalla() },
                                        onCerrarSesion = {
                                            huertoViewModel.cerrarSesion()
                                            reemplazarPantalla("login")
                                        }
                                    )
                                }
                                "administradores" -> RegistroAdministradorScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() },
                                    onRegistrationSuccess = { volverPantalla() }
                                )
                                "sectores" -> SectoresScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() },
                                    onSectorClick = {
                                        sectorSeleccionado = it
                                        navegarA("detalleSector")
                                    }
                                )
                                "cultivos" -> CultivosScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "detalleSector" -> DetalleSectorScreen(
                                    viewModel = huertoViewModel,
                                    sectorId = sectorSeleccionado,
                                    onBack = { volverPantalla() },
                                    onCultivoClick = {
                                        cultivoSeleccionado = it
                                        navegarA("detalleCultivo")
                                    }
                                )
                                "detalleCultivo" -> DetalleCultivoScreen(
                                    viewModel = huertoViewModel,
                                    cultivoId = cultivoSeleccionado,
                                    onBack = { volverPantalla() }
                                )
                                "tareas" -> TareasScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() },
                                    onCreateCultivo = { sectorId ->
                                        if (sectorId.isNullOrBlank()) {
                                            navegarA("sectores")
                                        } else {
                                            sectorSeleccionado = sectorId
                                            navegarA("detalleSector")
                                        }
                                    },
                                    onTaskClick = {
                                        tareaSeleccionada = it
                                        navegarA("detalleTarea")
                                    }
                                )
                                "detalleTarea" -> DetalleTareaScreen(
                                    viewModel = huertoViewModel,
                                    tareaId = tareaSeleccionada,
                                    onBack = { volverPantalla() }
                                )
                                "sensores" -> SensoresScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "problemas" -> ReportesProblemasScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "historial" -> HistorialActividadesScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "calendario" -> CalendarioHuertoScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "galeria" -> GaleriaHuertoScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                                "reportes" -> ReportesScreen(
                                    viewModel = huertoViewModel,
                                    onBack = { volverPantalla() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
