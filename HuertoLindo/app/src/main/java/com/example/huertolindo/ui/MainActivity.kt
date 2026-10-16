package com.example.huertolindo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.huertolindo.ui.screens.LoginScreen
import com.example.huertolindo.ui.theme.HuertoLindoTheme
import com.tuempresa.huertoescolar.viewmodel.HuertoViewModel

class MainActivity : ComponentActivity() {

    // Inicializamos el ViewModel que gestionará los datos de la app
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
                    // Llamamos a nuestra pantalla de Login pasándole el ViewModel
                    LoginScreen(viewModel = huertoViewModel) { perfilSeleccionado ->
                        // Aquí sabremos a qué rol ingresó ("administrador" o "estudiante")
                        // Más adelante cambiaremos esto para abrir pantallas distintas según el rol.
                    }
                }
            }
        }
    }
}

