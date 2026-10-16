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
import androidx.compose.ui.Modifier
import com.example.huertolindo.ui.screens.AdminDashboardScreen
import com.example.huertolindo.ui.screens.EstudianteDashboardScreen
import com.example.huertolindo.ui.screens.LoginScreen
import com.example.huertolindo.ui.theme.HuertoLindoTheme
import com.tuempresa.huertoescolar.viewmodel.HuertoViewModel

class MainActivity : ComponentActivity() {

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
                    // Variable de estado para cambiar la pantalla en tiempo real
                    var pantallaActual by remember { mutableStateOf("login") }

                    when (pantallaActual) {
                        "login" -> {
                            LoginScreen(viewModel = huertoViewModel) { perfil ->
                                // Cuando presiones 'Iniciar Sesión', cambia la variable
                                if (perfil == "administrador") {
                                    pantallaActual = "admin"
                                } else {
                                    pantallaActual = "estudiante"
                                }
                            }
                        }
                        "admin" -> AdminDashboardScreen(viewModel = huertoViewModel)
                        "estudiante" -> EstudianteDashboardScreen(viewModel = huertoViewModel)
                    }
                }
            }
        }
    }
}