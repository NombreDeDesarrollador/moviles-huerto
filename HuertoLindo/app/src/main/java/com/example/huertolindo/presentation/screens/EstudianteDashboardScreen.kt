package com.example.huertolindo.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.huertolindo.domain.security.PermisosHuerto
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
    onHistoryClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {}
) {
    val uiState = collectHuertoUiState(viewModel)
    val estudiante = uiState.usuarioActual
    val estudianteId = estudiante?.id ?: ""

    // Mis tareas asignadas únicamente
    val misTareas = uiState.tareas.filter { it.asignadoA == estudianteId }
    val misTareasRechazadas = misTareas.filter { it.estado == "Rechazada" }
    val misTareasEnCurso = misTareas.filter { it.estado == "En desarrollo" }
    val misTareasPendientes = misTareas.filter { it.estado == "Pendiente" }
    val misTareasPorValidar = misTareas.filter { it.estado == "Por validar" }
    val misTareasValidadas = misTareas.filter { PermisosHuerto.esTareaCompletada(it.estado) }
    val misTareasAtrasadas = viewModel.tareasAtrasadas.filter { it.asignadoA == estudianteId }

    val totalMisTareas = misTareas.size
    val porcentajeAvance = if (totalMisTareas > 0) misTareasValidadas.size.toFloat() / totalMisTareas else 0f
    val avanceAnimado by animateFloatAsState(
        targetValue = porcentajeAvance,
        animationSpec = tween(600),
        label = "avance-estudiante"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera con Saludo, Taller Asignado y Botón Perfil
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "¡Hola, ${estudiante?.nombre ?: "Estudiante"}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Mi Huerto Colaborativo",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Text(
                        text = "Taller: ${estudiante?.tallerAsignado?.ifBlank { "General" } ?: "General"}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Surface(
                onClick = onProfileClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🌱", fontSize = 16.sp)
                    Text("Perfil", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Estado de carga / sincronización
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
                    Text("Sincronizando tareas...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ALERTA DESTACADA: Tareas Rechazadas con Feedback del Docente
        AnimatedVisibility(visible = misTareasRechazadas.isNotEmpty()) {
            Card(
                onClick = onTasksClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🚨", fontSize = 18.sp)
                            Text(
                                "Tarea Rechazada (${misTareasRechazadas.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            "Requiere corrección",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    misTareasRechazadas.forEach { tarea ->
                        Text(
                            text = "• ${tarea.titulo}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (tarea.motivoRechazo.isNotBlank()) {
                            Text(
                                text = "Observación del docente: \"${tarea.motivoRechazo}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text(
                        text = "Toca aquí para revisar y reiniciar la tarea →",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Tarjeta de Resumen de Mis Tareas
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Resumen de Mis Tareas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColumnaMetricaEstudiante("Asignadas", misTareas.size.toString())
                    ColumnaMetricaEstudiante("En curso", misTareasEnCurso.size.toString())
                    ColumnaMetricaEstudiante("Por validar", misTareasPorValidar.size.toString())
                    ColumnaMetricaEstudiante("Validadas", misTareasValidadas.size.toString())
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mi progreso de cumplimiento", style = MaterialTheme.typography.labelSmall)
                        Text("${(avanceAnimado * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    LinearProgressIndicator(
                        progress = { avanceAnimado },
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

        // Alerta de Tareas Atrasadas
        AnimatedVisibility(visible = misTareasAtrasadas.isNotEmpty()) {
            Card(
                onClick = onTasksClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏰", fontSize = 22.sp)
                    Column {
                        Text(
                            "${misTareasAtrasadas.size} tareas con fecha vencida",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Revisa tus tareas pendientes de entrega.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Riegos pendientes del día
        AnimatedVisibility(visible = viewModel.cultivosConRiegoPendiente.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "💧 Recordatorio de Riego:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    viewModel.cultivosConRiegoPendiente.forEach { cultivo ->
                        Text(
                            "• ${cultivo.nombreComun} (sector programado para hoy)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Acciones del Estudiante (Exclusivamente de ejecución)
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        DashboardActionGrid(
            listOf(
                DashboardActionItem("Mis Tareas", "🧤", "Ver y entregar", (misTareasPendientes.size + misTareasEnCurso.size).takeIf { it > 0 }, null, null, onTasksClick),
                DashboardActionItem("Reportar Problema", "🪲", "Alertar al docente", null, null, null, onProblemsClick),
                DashboardActionItem("Explorar Sectores", "🌱", "Solo lectura", null, null, null, onSectorsClick),
                DashboardActionItem("Sensores", "🌡️", "Simulación académica", null, null, null, onSensorsClick),
                DashboardActionItem("Historial", "📋", "Mis actividades", null, null, null, onHistoryClick),
                DashboardActionItem("Calendario", "🗓️", "Fechas clave", null, null, null, onCalendarClick),
                DashboardActionItem("Galería", "🖼️", "Evidencias", null, null, null, onGalleryClick)
            )
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnaMetricaEstudiante(etiqueta: String, valor: String) {
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
