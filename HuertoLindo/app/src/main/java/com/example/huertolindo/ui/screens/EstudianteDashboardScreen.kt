package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun EstudianteDashboardScreen(
    viewModel: HuertoViewModel,
    onProfileClick: () -> Unit,
    onSectorsClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onSensorsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(onClick = onProfileClick) { Text("Mi perfil") }
        Text("Mi huerto", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Hola, ${viewModel.usuarioActual?.nombre ?: "Estudiante"}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Tienes ${viewModel.listaTareas.value.count { it.estado != "Completada" }} tareas por realizar.",
            style = MaterialTheme.typography.bodyLarge
        )
        DashboardAction("Explorar sectores", "Consulta cultivos y detalles de cada sector.", onSectorsClick)
        DashboardAction("Mis tareas", "Marca como completadas las labores que termines.", onTasksClick)
        DashboardAction("Sensores", "Revisa las condiciones simuladas del huerto.", onSensorsClick)
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
