package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.CircularProgressIndicator
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
    onSensorsClick: () -> Unit = {},
    onProblemsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {}
) {
    val uiState = collectHuertoUiState(viewModel)
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
        Text("Rol asignado: Estudiante", style = MaterialTheme.typography.labelLarge)
        Text(
            "Hola, ${uiState.usuarioActual?.nombre ?: "Estudiante"}",
            style = MaterialTheme.typography.titleLarge
        )
        AnimatedVisibility(uiState.cargandoDatos) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("Cargando información del huerto")
            }
        }
        uiState.errorPersistencia?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Tienes ${viewModel.tareasVisibles.count { it.estado != "Completada" }} tareas asignadas por realizar.",
            style = MaterialTheme.typography.bodyLarge
        )
        if (viewModel.tareasAtrasadas.isNotEmpty()) {
            DashboardAction(
                "Aviso: ${viewModel.tareasAtrasadas.size} tareas atrasadas",
                viewModel.tareasAtrasadas.take(3).joinToString("\n") {
                    "${it.titulo} · vencía ${it.fechaProgramada}"
                },
                onTasksClick
            )
        }
        DashboardAction("Explorar sectores", "Consulta cultivos y detalles de cada sector.", onSectorsClick)
        DashboardAction("Mis tareas", "Envía a revisión las labores que termines.", onTasksClick)
        DashboardAction("Historial de actividades", "Consulta los cambios y entregas de tus tareas.", onHistoryClick)
        DashboardAction("Sensores", "Revisa las condiciones simuladas del huerto.", onSensorsClick)
        DashboardAction("Reportar problema", "Registra y consulta problemas del huerto.", onProblemsClick)
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
