package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun LoginScreen(viewModel: HuertoViewModel, onLoginSuccess: (String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var perfilSeleccionado by remember { mutableStateOf("estudiante") }
    var intentoIniciarSesion by remember { mutableStateOf(false) }
    val nombreValido = nombre.length >= 2 && nombre.all { it.isLetter() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Huerto Escolar",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del usuario ficticio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = intentoIniciarSesion && !nombreValido,
            supportingText = if (intentoIniciarSesion && !nombreValido) {
                {
                    Text(
                        when {
                            nombre.isBlank() -> "Ingresa tu nombre."
                            nombre.length < 2 -> "El nombre debe tener al menos 2 letras."
                            else -> "El nombre solo puede contener letras."
                        }
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selecciona tu perfil:",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { perfilSeleccionado = "administrador" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (perfilSeleccionado == "administrador") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Text("Administrador")
            }

            Button(
                onClick = { perfilSeleccionado = "estudiante" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (perfilSeleccionado == "estudiante") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Text("Estudiante")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (!nombreValido) {
                    intentoIniciarSesion = true
                    return@Button
                }

                viewModel.iniciarSesion(
                    Usuario(
                        id = "1",
                        nombre = nombre,
                        perfil = perfilSeleccionado,
                        tallerAsignado = "General"
                    )
                )

                onLoginSuccess(perfilSeleccionado)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar Sesión")
        }
    }
}