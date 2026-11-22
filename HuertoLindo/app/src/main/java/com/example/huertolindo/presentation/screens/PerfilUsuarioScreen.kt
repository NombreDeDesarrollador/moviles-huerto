package com.example.huertolindo.presentation.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.media.crearUriFotografia
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilUsuarioScreen(
    viewModel: HuertoViewModel,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val uiState = collectHuertoUiState(viewModel)
    val usuario = uiState.usuarioActual
    val fotoPerfil = uiState.fotoPerfilUri
    val context = LocalContext.current

    var uriPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }
    var mostrarConfirmarCierre by remember { mutableStateOf(false) }

    val selectorFotos = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.actualizarFotoPerfil(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakePicture()
    ) { fotoTomada ->
        if (fotoTomada) viewModel.actualizarFotoPerfil(uriPendiente)
        uriPendiente = null
    }

    val esAdmin = PermisosHuerto.esAdministrador(usuario?.perfil)

    // Estadísticas
    val tareasAsignadas = uiState.tareas.count { it.asignadoA == usuario?.id }
    val tareasCompletadas = uiState.tareas.count { it.asignadoA == usuario?.id && PermisosHuerto.esTareaCompletada(it.estado) }
    val tareasPendientes = uiState.tareas.count { it.asignadoA == usuario?.id && !PermisosHuerto.esTareaCompletada(it.estado) }
    val evidenciasSubidas = uiState.evidencias.size
    val validacionesRealizadas = uiState.validaciones.count { it.administradorId == usuario?.id }
    val tareasPorValidar = uiState.tareas.count { it.estado == "Por validar" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil Institucional") },
                navigationIcon = {
                    TextButton(onClick = onVolver) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección Avatar
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (fotoPerfil != null) {
                    AsyncImage(
                        model = fotoPerfil,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = (usuario?.nombre?.take(1) ?: "U") + (usuario?.apellido?.take(1) ?: ""),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Botones de Foto
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { selectorFotos.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
                ) {
                    Text("Galería")
                }
                FilledTonalButton(
                    onClick = {
                        try {
                            val uri = crearUriFotografia(context)
                            uriPendiente = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            uriPendiente = null
                            Log.e("PerfilUsuarioScreen", "Error al abrir cámara", e)
                            Toast.makeText(context, "No se pudo abrir la cámara.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Cámara")
                }
                if (fotoPerfil != null) {
                    OutlinedButton(onClick = { viewModel.actualizarFotoPerfil(null) }) {
                        Text("Quitar")
                    }
                }
            }

            // Datos Principales
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nombre
                    Text(
                        text = usuario?.nombreCompleto?.ifBlank { "Usuario Institucional" } ?: "Usuario",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = usuario?.correo ?: "correo@huertolindo.cl",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Badges de Rol y Estado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Rol
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (esAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = if (esAdmin) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = if (esAdmin) "Docente / Tallerista" else "Estudiante Colaborador",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Badge Estado
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ) {
                            Text(
                                text = "● Cuenta Activa",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    // Taller / Grupo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Taller / Grupo Asignado:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = usuario?.tallerAsignado?.ifBlank { "General" } ?: "General",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Sección Estadísticas de Actividad
            Text(
                text = "Resumen de Actividad",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            if (esAdmin) {
                // Métricas Docente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ItemMetricaPerfil("Sectores", uiState.sectores.size.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Cultivos", uiState.cultivos.size.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Por Validar", tareasPorValidar.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ItemMetricaPerfil("Validaciones", validacionesRealizadas.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Problemas", uiState.reportesProblemas.size.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Estudiantes", uiState.usuariosDisponibles.count { it.perfil == "estudiante" }.toString(), Modifier.weight(1f))
                }
            } else {
                // Métricas Estudiante
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ItemMetricaPerfil("Asignadas", tareasAsignadas.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Validadas", tareasCompletadas.toString(), Modifier.weight(1f))
                    ItemMetricaPerfil("Pendientes", tareasPendientes.toString(), Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botón de Cierre de Sesión
            OutlinedButton(
                onClick = { mostrarConfirmarCierre = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar Sesión")
            }
        }
    }

    // Diálogo de Confirmación de Cierre de Sesión
    if (mostrarConfirmarCierre) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarCierre = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se cerrará la sesión actual en este dispositivo. Podrás volver a ingresar en cualquier momento con tus credenciales.") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmarCierre = false
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí, cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarCierre = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ItemMetricaPerfil(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
