package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun EstudianteDashboardScreen(viewModel: HuertoViewModel, onProfileClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = onProfileClick,
                modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 56.dp)
            ) {
                Text("Mi perfil")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Menú de estudiante",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = viewModel.usuarioActual?.nombre ?: "Estudiante",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}