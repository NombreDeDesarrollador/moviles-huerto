package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.huertoescolar.data.model.Usuario
import com.tuempresa.huertoescolar.viewmodel.HuertoViewModel

@Composable
fun LoginScreen(
    viewModel: HuertoViewModel,
    onLoginSuccess: (String) -> Unit // Esto recibe el perfil y avisa al MainActivity para cambiar de pantalla
) {
    var nombre by remember { mutableStateOf("") }
    var perfilSeleccionado by remember { mutableStateOf("estudiante") } // Por defecto estudiante

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título de la App
        Text(
            text = "🌱 Huerto Escolar",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa con una cuenta de prueba",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de texto para el nombre (aquí escribiste "Vicente")
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del usuario ficticio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Selector de rol (Administrador vs Estudiante)
        Text(text = "Selecciona tu rol:", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { perfilSeleccionado = "administrador" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (perfilSeleccionado == "administrador") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text("Administrador")
            }

            Button(
                onClick = { perfilSeleccionado = "estudiante" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (perfilSeleccionado == "estudiante") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text("Estudiante")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón de ingreso principal
        Button(
            onClick = {
                val nombreFinal = if (nombre.isBlank()) {
                    if (perfilSeleccionado == "administrador") "Profe Juan" else "Estudiante Ana"
                } else {
                    nombre
                }

                // 1. Creamos el usuario y lo guardamos en el ViewModel
                val usuarioFalso = Usuario(
                    id = "1",
                    nombre = nombreFinal,
                    perfil = perfilSeleccionado,
                    tallerAsignado = "Taller de Huerto Básico"
                )
                viewModel.iniciarSesion(usuarioFalso)

                // 2. ¡IMPORTANTE! Llamamos a esta función para que el MainActivity se entere y cambie de pantalla
                onLoginSuccess(perfilSeleccionado)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar Sesión")
        }
    }
}