package com.example.huertolindo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.presentation.components.DashboardActionGrid
import com.example.huertolindo.presentation.components.DashboardActionItem
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("¡Hola, ${uiState.usuarioActual?.nombre ?: "Estudiante"}!")
                Text("Mi huerto", style = MaterialTheme.typography.headlineSmall)
            }
            FilledTonalButton(onClick = onProfileClick) { Text("Perfil") }
        }
        AnimatedVisibility(uiState.cargandoDatos) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("Cargando…")
            }
        }
        uiState.errorPersistencia?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "${viewModel.tareasVisibles.count { it.estado != "Completada" }} tareas por realizar",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (viewModel.tareasAtrasadas.isNotEmpty()) {
            ElevatedCard(onClick = onTasksClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${viewModel.tareasAtrasadas.size} tareas atrasadas · Toca para revisarlas",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        DashboardActionGrid(
            listOf(
                DashboardActionItem("Explorar sectores", "🌱", onSectorsClick),
                DashboardActionItem("Mis tareas", "🧤", onTasksClick),
                DashboardActionItem("Historial", "📋", onHistoryClick),
                DashboardActionItem("Sensores", "🌡️", onSensorsClick),
                DashboardActionItem("Reportar problema", "🪲", onProblemsClick)
            )
        )
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}
