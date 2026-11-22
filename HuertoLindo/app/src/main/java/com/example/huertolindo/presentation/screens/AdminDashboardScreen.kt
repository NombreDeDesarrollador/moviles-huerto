package com.example.huertolindo.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.components.DashboardActionGrid
import com.example.huertolindo.presentation.components.DashboardActionItem
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: HuertoViewModel,
    onProfileClick: () -> Unit,
    onSectorsClick: () -> Unit = {},
    onCultivosClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onSensorsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onProblemsClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onAdministratorsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {}
) {
    val uiState = collectHuertoUiState(viewModel)
    val admin = uiState.usuarioActual

    // Tareas pendientes de validación
    val tareasPorValidar = uiState.tareas.filter { it.estado == "Por validar" }
    val tareasPendientesTotal = uiState.tareas.count { !PermisosHuerto.esTareaCompletada(it.estado) }
    val tareasValidadasTotal = uiState.tareas.count { PermisosHuerto.esTareaCompletada(it.estado) }
    val totalTareas = uiState.tareas.size
    val porcentajeCompletado = if (totalTareas > 0) tareasValidadasTotal.toFloat() / totalTareas else 0f
    val progresoAnimado by animateFloatAsState(
        targetValue = porcentajeCompletado,
        animationSpec = tween(600),
        label = "progreso-cumplimiento"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera con Saludo y Perfil
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "¡Hola, ${admin?.nombre ?: "Docente"}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Supervisión Docente",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                onClick = onProfileClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🎓", fontSize = 16.sp)
                    Text("Mi Perfil", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Estado de carga o error
        AnimatedVisibility(uiState.isLoading) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Sincronizando con Spring Boot...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        uiState.errorMessage?.let { errorMsg ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Tarjeta de Métricas Globales del Huerto
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Métricas Globales del Huerto",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColumnaMetrica("Sectores", uiState.sectores.size.toString())
                    ColumnaMetrica("Cultivos", uiState.cultivos.size.toString())
                    ColumnaMetrica("Pendientes", tareasPendientesTotal.toString())
                    ColumnaMetrica("Problemas", uiState.reportesProblemas.size.toString())
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Avance de tareas validadas", style = MaterialTheme.typography.labelSmall)
                        Text("${(progresoAnimado * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    LinearProgressIndicator(
                        progress = { progresoAnimado },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // BANDEJA DESTACADA: Tareas Pendientes de Validación
        AnimatedVisibility(visible = tareasPorValidar.isNotEmpty()) {
            Card(
                onClick = onTasksClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("📥", fontSize = 18.sp)
                            Text(
                                "Bandeja de Validación (${tareasPorValidar.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                "Requiere revisión",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    tareasPorValidar.take(3).forEach { tarea ->
                        val estudiante = uiState.usuariosDisponibles.firstOrNull { it.id == tarea.asignadoA }?.nombreCompleto ?: "Estudiante"
                        Text(
                            text = "• ${tarea.titulo} — Entregado por: $estudiante",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = "Toca para abrir y calificar/aprobar tareas →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Alerta de Tareas Atrasadas
        AnimatedVisibility(visible = viewModel.tareasAtrasadas.isNotEmpty()) {
            Card(
                onClick = onTasksClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 24.sp)
                    Column {
                        Text(
                            "${viewModel.tareasAtrasadas.size} tareas con fecha vencida",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Revisa la programación del huerto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Riegos pendientes
        AnimatedVisibility(visible = viewModel.cultivosConRiegoPendiente.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "💧 Riego programado para hoy:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    viewModel.cultivosConRiegoPendiente.forEach { cultivo ->
                        Text(
                            "• ${cultivo.nombreComun} (Frecuencia: cada ${cultivo.frecuenciaRiego} días)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Módulo de Acciones Rápidas (Grid Modular)
        Text(
            text = "Gestión del Huerto",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        DashboardActionGrid(
            listOf(
                DashboardActionItem("Sectores y Cultivos", "🌱", "Administrar zonas", null, null, null, onSectorsClick),
                DashboardActionItem("Cultivos", "🪴", "Crear y consultar", null, null, null, onCultivosClick),
                DashboardActionItem("Gestión de Tareas", "🧤", "Asignar y validar", tareasPorValidar.size.takeIf { it > 0 }, null, null, onTasksClick),
                DashboardActionItem("Historial", "📋", "Auditoría de eventos", null, null, null, onHistoryClick),
                DashboardActionItem("Sensores", "🌡️", "Simulación académica", null, null, null, onSensorsClick),
                DashboardActionItem("Problemas", "🪲", "Alertas y plagas", uiState.reportesProblemas.size.takeIf { it > 0 }, null, null, onProblemsClick),
                DashboardActionItem("Reportes", "📊", "Métricas y resumen", null, null, null, onReportsClick),
                DashboardActionItem("Administradores", "👥", "Nuevo docente", null, null, null, onAdministratorsClick),
                DashboardActionItem("Calendario", "🗓️", "Cronograma", null, null, null, onCalendarClick),
                DashboardActionItem("Galería", "🖼️", "Evidencias fotográficas", null, null, null, onGalleryClick)
            )
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnaMetrica(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
