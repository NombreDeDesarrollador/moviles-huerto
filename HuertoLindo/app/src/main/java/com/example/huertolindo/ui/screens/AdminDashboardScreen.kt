package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: HuertoViewModel,
    onProfileClick: () -> Unit,
    onSectorsClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onSensorsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {}
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
        Text("Panel de administración", style = MaterialTheme.typography.headlineMedium)
        Text("Rol asignado: Administrador", style = MaterialTheme.typography.labelLarge)
        Text(
            "Hola, ${viewModel.usuarioActual?.nombre ?: "Administrador"}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "${viewModel.listaSectores.value.size} sectores · ${viewModel.listaCultivos.value.size} cultivos · " +
                "${viewModel.tareasVisibles.count { it.estado != "Completada" }} tareas pendientes",
            style = MaterialTheme.typography.bodyLarge
        )
        DashboardAction("Sectores y cultivos", "Consultar los espacios de cultivo y su estado.", onSectorsClick)
        DashboardAction("Tareas", "Revisar y actualizar las labores del huerto.", onTasksClick)
        DashboardAction("Sensores", "Consultar lecturas simuladas de humedad y temperatura.", onSensorsClick)
        DashboardAction("Reportes", "Ver un resumen del estado general del huerto.", onReportsClick)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun DashboardAction(title: String, description: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
