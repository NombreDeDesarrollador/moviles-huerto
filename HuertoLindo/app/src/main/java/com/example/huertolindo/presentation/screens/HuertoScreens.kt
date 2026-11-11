package com.example.huertolindo.presentation.screens

import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huertolindo.presentation.media.crearUriFotografia
import com.example.huertolindo.presentation.media.eliminarFotografiaTemporal
import com.example.huertolindo.domain.model.SensorSimulado
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.EstadoCultivo
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.TipoAsociacionEvidencia
import com.example.huertolindo.domain.model.TipoTarea
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.components.HuertoStatusChip
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel
import java.io.File

@Composable
fun SectoresScreen(
    viewModel: HuertoViewModel,
    onBack: () -> Unit,
    onSectorClick: (String) -> Unit
) {
    val uiState = collectHuertoUiState(viewModel)
    val esAdministrador = viewModel.esAdministrador
    var mostrarFormulario by remember { mutableStateOf(false) }
    var sectorEnEdicion by remember { mutableStateOf<Sector?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenLayout("Sectores del huerto", onBack) {
        uiState.errorPersistencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (esAdministrador) {
            Button(onClick = {
                sectorEnEdicion = null
                mostrarFormulario = true
            }) { Text("Agregar sector") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (uiState.sectores.isEmpty()) {
            Text("Todavía no hay sectores registrados.")
        }
        uiState.sectores.forEach { sector ->
            ElevatedCard(
                onClick = { onSectorClick(sector.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(sector.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Ubicación: ${sector.ubicacion.ifBlank { "Sin ubicación registrada" }}")
                    Text(sector.descripcion)
                    HuertoStatusChip(sector.estado)
                    Text("${uiState.cultivos.count { it.sectorId == sector.id }} cultivos")
                    if (sector.estado == "En mantenimiento") {
                        Text("Mantenimiento: ${sector.fechaInicioMantenimiento} al ${sector.fechaTerminoMantenimiento}")
                    }
                    if (esAdministrador) {
                        OutlinedButton(onClick = {
                            sectorEnEdicion = sector
                            mostrarFormulario = true
                        }) { Text("Editar sector") }
                        OutlinedButton(onClick = {
                            error = if (viewModel.eliminarSector(sector.id)) null
                                else "No se puede eliminar un sector asociado a cultivos, tareas o reportes."
                        }) { Text("Eliminar sector") }
                    }
                }
            }
        }
    }
    if (mostrarFormulario) {
        FormularioSector(
            sector = sectorEnEdicion,
            onDismiss = {
                mostrarFormulario = false
                sectorEnEdicion = null
            },
            onSave = { nombre, ubicacion, descripcion, estado, inicio, termino ->
                val guardado = sectorEnEdicion?.let {
                    viewModel.editarSector(it.id, nombre, ubicacion, descripcion, estado, inicio, termino)
                } ?: viewModel.agregarSector(nombre, ubicacion, descripcion, estado, inicio, termino)
                if (guardado) {
                    error = null
                    mostrarFormulario = false
                    sectorEnEdicion = null
                } else {
                    error = "Completa el nombre, la ubicación y la descripción del sector."
                }
                guardado
            }
        )
    }
}

@Composable
fun DetalleSectorScreen(
    viewModel: HuertoViewModel,
    sectorId: String,
    onBack: () -> Unit,
    onCultivoClick: (String) -> Unit
) {
    val uiState = collectHuertoUiState(viewModel)
    val esAdministrador = viewModel.esAdministrador
    var mostrarFormulario by remember { mutableStateOf(false) }
    var cultivoEnEdicion by remember { mutableStateOf<Cultivo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val sector = uiState.sectores.firstOrNull { it.id == sectorId }
    ScreenLayout(sector?.nombre ?: "Sector no encontrado", onBack) {
        uiState.errorPersistencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (sector == null) {
            Text("El sector seleccionado ya no está disponible.")
            return@ScreenLayout
        }
        Text("Ubicación: ${sector.ubicacion.ifBlank { "Sin ubicación registrada" }}")
        Text(sector.descripcion, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado:", style = MaterialTheme.typography.bodyLarge)
            HuertoStatusChip(sector.estado)
        }
        Text("Cultivos", style = MaterialTheme.typography.titleLarge)
        if (esAdministrador) {
            Button(onClick = {
                cultivoEnEdicion = null
                mostrarFormulario = true
            }) { Text("Agregar cultivo") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        val cultivos = uiState.cultivos.filter { it.sectorId == sector.id }
        if (cultivos.isEmpty()) Text("Este sector todavía no tiene cultivos.")
        cultivos.forEach { cultivo ->
            ElevatedCard(
                onClick = { onCultivoClick(cultivo.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(cultivo.nombreComun, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Desarrollo:")
                        HuertoStatusChip(cultivo.estadoDesarrollo)
                    }
                    Text("Siembra: ${cultivo.fechaSiembra}")
                    Text(
                        "Cosecha estimada: ${
                            cultivo.fechaCosechaEstimada.ifBlank { "Sin fecha registrada" }
                        }"
                    )
                    if (esAdministrador) {
                        OutlinedButton(onClick = {
                            cultivoEnEdicion = cultivo
                            mostrarFormulario = true
                        }) { Text("Editar cultivo") }
                        OutlinedButton(onClick = {
                            viewModel.eliminarCultivo(cultivo.id)
                        }) { Text("Eliminar cultivo") }
                    }
                }
            }
        }
    }
    if (mostrarFormulario) {
        FormularioCultivo(
            cultivo = cultivoEnEdicion,
            onDismiss = {
                mostrarFormulario = false
                cultivoEnEdicion = null
            },
            onSave = { nombre, fechaSiembra, fechaCosecha, estado, frecuencia ->
                val guardado = cultivoEnEdicion?.let {
                    viewModel.editarCultivo(
                        it.id,
                        nombre,
                        fechaSiembra,
                        fechaCosecha,
                        estado,
                        frecuencia
                    )
                } ?: viewModel.agregarCultivo(
                    nombre,
                    sectorId,
                    fechaSiembra,
                    fechaCosecha,
                    estado,
                    frecuencia
                )
                if (guardado) {
                    error = null
                    mostrarFormulario = false
                    cultivoEnEdicion = null
                } else {
                    error = "Completa los datos e ingresa fechas válidas; la cosecha estimada debe ser igual o posterior a la siembra."
                }
                guardado
            }
        )
    }
}

@Composable
fun DetalleCultivoScreen(viewModel: HuertoViewModel, cultivoId: String, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    val cultivo = uiState.cultivos.firstOrNull { it.id == cultivoId }
    ScreenLayout(cultivo?.nombreComun ?: "Cultivo no encontrado", onBack) {
        if (cultivo == null) {
            Text("El cultivo seleccionado ya no está disponible.")
            return@ScreenLayout
        }
        val sector = uiState.sectores.firstOrNull { it.id == cultivo.sectorId }
        Text("Sector: ${sector?.nombre ?: "Sin sector"}")
        Text("Fecha de siembra: ${cultivo.fechaSiembra}")
        Text(
            "Cosecha estimada: ${
                cultivo.fechaCosechaEstimada.ifBlank { "Sin fecha registrada" }
            }"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado de desarrollo:")
            HuertoStatusChip(cultivo.estadoDesarrollo)
        }
        Text("Frecuencia de riego: cada ${cultivo.frecuenciaRiego} días")
        viewModel.fechaProximoRiego(cultivo)?.let { fecha ->
            Text("Próximo riego programado: $fecha")
        }
        if (cultivo.fechaCambioEstado.isNotBlank()) {
            Text("Estado actualizado: ${cultivo.fechaCambioEstado}")
        }
        EvidenciaFotograficaSection(
            titulo = "Registro fotográfico del cultivo",
            evidencias = viewModel.evidenciasDeCultivo(cultivo.id),
            onAgregarFoto = { uri, onResult ->
                viewModel.agregarEvidencia(
                    TipoAsociacionEvidencia.CULTIVO,
                    cultivo.id,
                    uri,
                    onResult
                )
            },
            onEliminarFoto = { evidencia, onResult ->
                viewModel.eliminarEvidencia(evidencia.id, onResult)
            }
        )
    }
}

@Composable
fun TareasScreen(
    viewModel: HuertoViewModel,
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val uiState = collectHuertoUiState(viewModel)
    val esAdministrador = viewModel.esAdministrador
    var mostrarFormulario by remember { mutableStateOf(false) }
    var tareaEnEdicion by remember { mutableStateOf<Tarea?>(null) }
    var estadoFiltro by remember { mutableStateOf("") }
    var fechaFiltro by remember { mutableStateOf("") }
    var responsableFiltro by remember { mutableStateOf("") }
    var tareaParaRechazar by remember { mutableStateOf<Tarea?>(null) }
    var motivoRechazo by remember { mutableStateOf("") }
    var errorMotivoRechazo by remember { mutableStateOf(false) }
    var enviandoRechazo by remember { mutableStateOf(false) }
    var errorOperacionRechazo by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val tareas = viewModel.tareasVisibles
    val tareasFiltradas = tareas.filter { tarea ->
        (estadoFiltro.isBlank() ||
            when (estadoFiltro) {
                "Atrasadas" -> viewModel.tareasAtrasadas.any { it.id == tarea.id }
                "Realizadas" -> PermisosHuerto.esTareaCompletada(tarea.estado)
                "Pendientes" -> !PermisosHuerto.esTareaCompletada(tarea.estado)
                else -> tarea.estado == estadoFiltro
            }) &&
            (fechaFiltro.isBlank() || tarea.fechaProgramada == fechaFiltro.trim()) &&
            (responsableFiltro.isBlank() || tarea.asignadoA == responsableFiltro)
    }
    ScreenLayout("Tareas del huerto", onBack) {
        uiState.errorPersistencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (esAdministrador) {
            Button(onClick = {
                tareaEnEdicion = null
                mostrarFormulario = true
            }) { Text("Crear y asignar tarea") }
            uiState.errorCargaUsuarios?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (viewModel.tareasAtrasadas.isNotEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${viewModel.tareasAtrasadas.size} tareas atrasadas",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    viewModel.tareasAtrasadas.forEach { tarea ->
                        Text("${tarea.titulo} · fecha límite ${tarea.fechaProgramada}")
                    }
                }
            }
        }
        Text("Filtrar tareas", style = MaterialTheme.typography.titleMedium)
        CampoSeleccion(
            etiqueta = "Estado",
            opciones = listOf("" to "Todos los estados") +
                listOf("Pendientes", "Realizadas", "Atrasadas", "Pendiente", "En desarrollo", "Por validar", "Rechazada", "Validada", "Completada")
                    .map { it to it },
            seleccionado = estadoFiltro,
            onSeleccionar = { estadoFiltro = it }
        )
        OutlinedTextField(
            value = fechaFiltro,
            onValueChange = { fechaFiltro = it },
            label = { Text("Fecha programada") },
            placeholder = { Text("AAAA-MM-DD") },
            supportingText = {
                Text("Escribe una fecha exacta para filtrar.")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (esAdministrador) {
            CampoSeleccion(
                etiqueta = "Responsable",
                opciones = listOf("" to "Todos los responsables") +
                    uiState.usuariosDisponibles
                        .filter { it.perfil == "estudiante" }
                        .map { it.id to it.nombreCompleto },
                seleccionado = responsableFiltro,
                onSeleccionar = { responsableFiltro = it }
            )
        }
        TextButton(onClick = {
            estadoFiltro = ""
            fechaFiltro = ""
            responsableFiltro = ""
        }) { Text("Limpiar filtros") }
        if (tareasFiltradas.isEmpty()) {
            Text(
                if (tareas.isEmpty()) "No tienes tareas asignadas."
                else "No hay tareas que coincidan con los filtros."
            )
        }
        tareasFiltradas.forEach { tarea ->
            ElevatedCard(
                onClick = { onTaskClick(tarea.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(tarea.titulo, style = MaterialTheme.typography.titleMedium)
                    HuertoStatusChip(tarea.estado)
                    Text("Tipo: ${tarea.tipo}")
                    Text("Fecha programada: ${tarea.fechaProgramada.ifBlank { "Sin fecha programada" }}")
                    if (viewModel.tareasAtrasadas.any { it.id == tarea.id }) {
                        HuertoStatusChip("Atrasada")
                    }
                    if (tarea.cultivoId != null) {
                        Text("Cultivo: ${uiState.cultivos.firstOrNull { it.id == tarea.cultivoId }?.nombreComun ?: "No disponible"}")
                    }
                    if (esAdministrador && !PermisosHuerto.esTareaCompletada(tarea.estado)) {
                        OutlinedButton(onClick = {
                            tareaEnEdicion = tarea
                            mostrarFormulario = true
                        }) { Text("Editar tarea") }
                    }
                    if (esAdministrador && tarea.estado == "Por validar") {
                        OutlinedButton(onClick = {
                            tareaParaRechazar = tarea
                            motivoRechazo = ""
                            errorMotivoRechazo = false
                            errorOperacionRechazo = null
                        }) { Text("Rechazar tarea") }
                    }
                }
            }
        }
    }
    if (mostrarFormulario) {
        FormularioTarea(
            sectores = uiState.sectores,
            cultivos = uiState.cultivos,
            estudiantes = uiState.usuariosDisponibles.filter { it.perfil == "estudiante" },
            tarea = tareaEnEdicion,
            onDismiss = {
                mostrarFormulario = false
                tareaEnEdicion = null
            },
            onSave = { titulo, instrucciones, sectorId, cultivoId, usuarioId, fechaProgramada, tipo ->
                val guardada = tareaEnEdicion?.let { tarea ->
                    viewModel.editarTarea(
                        tarea.id, titulo, instrucciones, sectorId, usuarioId, fechaProgramada, tipo, cultivoId
                    )
                } ?: viewModel.crearTarea(
                    titulo, instrucciones, sectorId, usuarioId, fechaProgramada, tipo, cultivoId
                )
                if (guardada) {
                    error = null
                    mostrarFormulario = false
                    tareaEnEdicion = null
                } else {
                    error = "Completa los datos, ingresa una fecha válida (AAAA-MM-DD) y selecciona un sector y estudiante."
                }
                guardada
            }
        )
    }
    tareaParaRechazar?.let { tarea ->
        AlertDialog(
            onDismissRequest = {
                if (!enviandoRechazo) tareaParaRechazar = null
            },
            title = { Text("Rechazar tarea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Indica al estudiante qué debe corregir.")
                    errorOperacionRechazo?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    OutlinedTextField(
                        value = motivoRechazo,
                        onValueChange = {
                            motivoRechazo = it
                            errorMotivoRechazo = false
                        },
                        label = { Text("Motivo del rechazo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        isError = errorMotivoRechazo || (errorOperacionRechazo != null && motivoRechazo.isBlank()),
                        supportingText = {
                            if (errorMotivoRechazo) {
                                Text("El motivo es obligatorio.")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (motivoRechazo.isBlank()) {
                        errorMotivoRechazo = true
                    } else {
                        enviandoRechazo = true
                        errorOperacionRechazo = null
                        viewModel.rechazarTarea(tarea.id, motivoRechazo) { resultado ->
                            enviandoRechazo = false
                            if (resultado == null) {
                                tareaParaRechazar = null
                                motivoRechazo = ""
                                error = null
                            } else {
                                errorOperacionRechazo = resultado
                            }
                        }
                    }
                }, enabled = !enviandoRechazo) {
                    Text(if (enviandoRechazo) "Guardando..." else "Confirmar rechazo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tareaParaRechazar = null },
                    enabled = !enviandoRechazo
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DetalleTareaScreen(
    viewModel: HuertoViewModel,
    tareaId: String,
    onBack: () -> Unit
) {
    val uiState = collectHuertoUiState(viewModel)
    val tarea = viewModel.tareasVisibles.firstOrNull { it.id == tareaId }
    var mostrarDialogoRechazo by remember { mutableStateOf(false) }
    var motivoRechazo by remember { mutableStateOf("") }
    var errorRechazo by remember { mutableStateOf<String?>(null) }
    var enviandoRechazo by remember { mutableStateOf(false) }
    var comentarioEntrega by remember(tareaId) { mutableStateOf("") }
    var imagenEntrega by remember(tareaId) { mutableStateOf<Uri?>(null) }
    var uriCamaraPendiente by remember(tareaId) { mutableStateOf<Uri?>(null) }
    var errorEntrega by remember { mutableStateOf<String?>(null) }
    var enviandoEntrega by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectorEvidencia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            imagenEntrega?.let { eliminarFotografiaTemporal(context, it) }
            imagenEntrega = uri
            errorEntrega = null
        }
    }
    val camaraEvidencia = rememberLauncherForActivityResult(TakePicture()) { capturada ->
        val uri = uriCamaraPendiente
        if (capturada && uri != null) {
            imagenEntrega?.let { eliminarFotografiaTemporal(context, it) }
            imagenEntrega = uri
            errorEntrega = null
        } else if (uri != null) {
            eliminarFotografiaTemporal(context, uri)
        }
        uriCamaraPendiente = null
    }
    val evidencias = viewModel.evidenciasDeTarea(tareaId)
    ScreenLayout(tarea?.titulo ?: "Tarea no encontrada", onBack) {
        uiState.errorPersistencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (tarea == null) {
            Text("La tarea seleccionada ya no está disponible.")
            return@ScreenLayout
        }
        val sector = uiState.sectores.firstOrNull { it.id == tarea.sectorId }
        val estudiante = uiState.usuariosDisponibles.firstOrNull { it.id == tarea.asignadoA }
        Text("Instrucciones", style = MaterialTheme.typography.titleMedium)
        Text(tarea.descripcion, style = MaterialTheme.typography.bodyLarge)
        Text("Tipo de tarea: ${tarea.tipo}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado:", style = MaterialTheme.typography.bodyLarge)
            HuertoStatusChip(tarea.estado)
        }
        if (tarea.estado == "Rechazada") {
            Text(
                "Motivo del rechazo: ${tarea.motivoRechazo}",
                color = MaterialTheme.colorScheme.error
            )
        }
        Text(
            "Fecha programada: ${tarea.fechaProgramada.ifBlank { "Sin fecha programada" }}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text("Sector: ${sector?.nombre ?: "No asignado"}")
        tarea.cultivoId?.let { cultivoId ->
            Text("Cultivo: ${uiState.cultivos.firstOrNull { it.id == cultivoId }?.nombreComun ?: "No disponible"}")
        }
        Text("Asignada a: ${estudiante?.nombreCompleto ?: tarea.asignadoA}")
        if (evidencias.any { it.descripcion.isNotBlank() }) {
            Text("Comentario de entrega", style = MaterialTheme.typography.titleMedium)
            Text(uiState.comentariosEntrega[tarea.id].orEmpty())
        }
        EvidenciaFotograficaSection(
            titulo = "Registro fotográfico de la tarea",
            evidencias = evidencias,
            editable = viewModel.esAdministrador ||
                viewModel.puedeAdjuntarEvidenciaTarea(tarea),
            puedeAgregar = viewModel.puedeAdjuntarEvidenciaTarea(tarea) &&
                tarea.estado != "En desarrollo",
            puedeEliminar = viewModel.puedeEliminarEvidenciaTarea(tarea),
            onAgregarFoto = { uri, onResult ->
                viewModel.agregarEvidencia(
                    TipoAsociacionEvidencia.TAREA,
                    tarea.id,
                    uri,
                    onResult
                )
            },
            onEliminarFoto = { evidencia, onResult ->
                viewModel.eliminarEvidencia(evidencia.id, onResult)
            }
        )
        val validaciones = uiState.validaciones.filter { it.tareaId == tarea.id }
        if (validaciones.isNotEmpty()) {
            Text("Historial de validación", style = MaterialTheme.typography.titleMedium)
            validaciones.forEach { validacion ->
                val administrador = uiState.usuariosDisponibles
                    .firstOrNull { it.id == validacion.administradorId }
                Text("${validacion.resultado} · ${validacion.fechaValidacion}")
                Text("Revisó: ${administrador?.nombreCompleto ?: validacion.administradorId}")
                if (validacion.observaciones.isNotBlank()) {
                    Text("Observaciones: ${validacion.observaciones}")
                }
            }
        }
        if (viewModel.esAdministrador && tarea.estado == "Por validar") {
            Button(onClick = { viewModel.validarTarea(tarea.id) }) {
                Text("Validar actividad")
            }
            OutlinedButton(onClick = {
                motivoRechazo = ""
                errorRechazo = null
                mostrarDialogoRechazo = true
            }) { Text("Rechazar actividad") }
        } else if (!viewModel.esAdministrador && tarea.estado in setOf("Pendiente", "Rechazada")) {
            Button(onClick = { viewModel.iniciarTarea(tarea.id) }) {
                Text(if (tarea.estado == "Rechazada") "Corregir y volver a iniciar" else "Iniciar tarea")
            }
        } else if (!viewModel.esAdministrador && tarea.estado == "En desarrollo") {
            Text("Entrega de actividad", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = comentarioEntrega,
                onValueChange = {
                    comentarioEntrega = it
                    errorEntrega = null
                },
                label = { Text("Comentario del trabajo realizado") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                isError = errorEntrega != null && comentarioEntrega.isBlank()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selectorEvidencia.launch(
                        PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                    )
                }) { Text("Elegir fotografía") }
                OutlinedButton(onClick = {
                    var uri: Uri? = null
                    try {
                        uri = crearUriFotografia(context)
                        uriCamaraPendiente = uri
                        camaraEvidencia.launch(uri)
                    } catch (exception: java.io.IOException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        uriCamaraPendiente = null
                        errorEntrega = "No se pudo preparar la cámara."
                    } catch (exception: SecurityException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        uriCamaraPendiente = null
                        errorEntrega = "No se pudo acceder a la cámara."
                    } catch (exception: ActivityNotFoundException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        uriCamaraPendiente = null
                        errorEntrega = "No hay una aplicación de cámara disponible."
                    } catch (exception: IllegalArgumentException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        uriCamaraPendiente = null
                        errorEntrega = "No se pudo preparar el archivo para la cámara."
                    }
                }) { Text("Tomar foto") }
            }
            imagenEntrega?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Vista previa de la evidencia",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
                TextButton(onClick = {
                    imagenEntrega?.let { eliminarFotografiaTemporal(context, it) }
                    imagenEntrega = null
                    errorEntrega = null
                }) { Text("Quitar fotografía") }
            }
            errorEntrega?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    enviandoEntrega = true
                    errorEntrega = null
                    viewModel.entregarTarea(
                        tareaId = tarea.id,
                        comentario = comentarioEntrega,
                        imagenUri = imagenEntrega
                    ) { resultado ->
                        enviandoEntrega = false
                        errorEntrega = resultado
                        if (resultado == null) {
                            imagenEntrega?.let { eliminarFotografiaTemporal(context, it) }
                            comentarioEntrega = ""
                            imagenEntrega = null
                        }
                    }
                },
                enabled = !enviandoEntrega
            ) {
                Text(if (enviandoEntrega) "Enviando..." else "Entregar para revisión")
            }
        }
    }
    if (mostrarDialogoRechazo && tarea != null) {
        AlertDialog(
            onDismissRequest = {
                if (!enviandoRechazo) mostrarDialogoRechazo = false
            },
            title = { Text("Rechazar actividad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Indica qué debe corregir el estudiante.")
                    OutlinedTextField(
                        value = motivoRechazo,
                        onValueChange = {
                            motivoRechazo = it
                            errorRechazo = null
                        },
                        label = { Text("Motivo del rechazo") },
                        isError = errorRechazo != null,
                        supportingText = {
                            errorRechazo?.let { Text(it) }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (motivoRechazo.isBlank()) {
                        errorRechazo = "El motivo del rechazo es obligatorio."
                    } else {
                        enviandoRechazo = true
                        errorRechazo = null
                        viewModel.rechazarTarea(tarea.id, motivoRechazo) { resultado ->
                            enviandoRechazo = false
                            if (resultado == null) {
                                mostrarDialogoRechazo = false
                            } else {
                                errorRechazo = resultado
                            }
                        }
                    }
                }, enabled = !enviandoRechazo) {
                    Text(if (enviandoRechazo) "Guardando..." else "Rechazar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoRechazo = false },
                    enabled = !enviandoRechazo
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun SensoresScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    ScreenLayout("Sensores del huerto", onBack) {
        Text("Lecturas de demostración; no corresponden a sensores físicos.")
        Text("Las alertas son informativas: no activan bombas ni riego automático.")
        uiState.sectores.forEach { sector ->
            val lectura = if (sector.id == "1") {
                SensorSimulado(sector.id, 62.0, 21.5, 54.0)
            } else {
                SensorSimulado(sector.id, 48.0, 23.0, 49.0)
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sector.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("Humedad del suelo: ${lectura.humedadSuelo}%")
                    Text("Temperatura ambiental: ${lectura.temperaturaAmbiental} °C")
                    Text("Humedad ambiental: ${lectura.humedadAmbiental}%")
                    Text(lectura.origen, style = MaterialTheme.typography.bodySmall)
                    if (lectura.humedadSuelo < 50.0) {
                        HuertoStatusChip("Bajo · pendiente de revisión")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportesScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    val tareas = viewModel.tareasVisibles
    ScreenLayout("Resumen del huerto", onBack) {
        if (!viewModel.esAdministrador) {
            Text("No tienes permiso para consultar este resumen.")
        } else {
            ReporteDato("Sectores", uiState.sectores.size.toString())
            ReporteDato("Cultivos activos", uiState.cultivos.size.toString())
            ReporteDato(
                "Tareas completadas",
                tareas.count { PermisosHuerto.esTareaCompletada(it.estado) }.toString()
            )
            ReporteDato(
                "Tareas pendientes",
                tareas.count { !PermisosHuerto.esTareaCompletada(it.estado) }.toString()
            )
            Text(
                "Este resumen se actualiza al cambiar el estado de las tareas.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun HistorialActividadesScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    var sectorFiltro by remember { mutableStateOf("") }
    var cultivoFiltro by remember { mutableStateOf("") }
    var desde by remember { mutableStateOf("") }
    var hasta by remember { mutableStateOf("") }
    ScreenLayout("Historial de actividades", onBack) {
        CampoSeleccion(
            etiqueta = "Sector",
            opciones = listOf("" to "Todos los sectores") + uiState.sectores.map { it.id to it.nombre },
            seleccionado = sectorFiltro,
            onSeleccionar = {
                sectorFiltro = it
                cultivoFiltro = ""
            }
        )
        CampoSeleccion(
            etiqueta = "Cultivo",
            opciones = listOf("" to "Todos los cultivos") +
                uiState.cultivos.filter { sectorFiltro.isBlank() || it.sectorId == sectorFiltro }
                    .map { it.id to it.nombreComun },
            seleccionado = cultivoFiltro,
            onSeleccionar = { cultivoFiltro = it }
        )
        OutlinedTextField(
            value = desde,
            onValueChange = { desde = it },
            label = { Text("Desde (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hasta,
            onValueChange = { hasta = it },
            label = { Text("Hasta (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val tareas = viewModel.tareasVisibles.associateBy { it.id }
        val reportes = uiState.reportesProblemas.filter {
            viewModel.esAdministrador || it.usuarioId == uiState.usuarioActual?.id
        }
        val items = buildList {
            viewModel.historialVisible.forEach { actividad ->
                val tarea = tareas[actividad.tareaId]
                add(
                    HistorialItem(
                        actividad.tituloTarea,
                        actividad.accion,
                        actividad.estado,
                        actividad.fecha,
                        tarea?.sectorId.orEmpty(),
                        tarea?.cultivoId
                    )
                )
            }
            uiState.validaciones.forEach { validacion ->
                val tarea = tareas[validacion.tareaId] ?: return@forEach
                add(
                    HistorialItem(
                        tarea.titulo,
                        "${validacion.resultado}: ${validacion.observaciones}".trimEnd(),
                        validacion.resultado,
                        validacion.fechaValidacion,
                        tarea.sectorId,
                        tarea.cultivoId
                    )
                )
            }
            uiState.evidencias.forEach { evidencia ->
                val tarea = evidencia.tareaId?.let(tareas::get)
                val cultivo = uiState.cultivos.firstOrNull { it.id == evidencia.cultivoId }
                val reporte = reportes.firstOrNull { it.id == evidencia.reporteId }
                if (evidencia.tareaId != null && tarea == null) return@forEach
                if (evidencia.reporteId != null && reporte == null) return@forEach
                add(
                    HistorialItem(
                        "Fotografía adjunta",
                        evidencia.descripcion.ifBlank { "Evidencia fotográfica agregada" },
                        "Registrada",
                        evidencia.fechaRegistro,
                        tarea?.sectorId ?: cultivo?.sectorId ?: reporte?.sectorId.orEmpty(),
                        tarea?.cultivoId ?: cultivo?.id ?: reporte?.cultivoId,
                        evidencia.rutaArchivo
                    )
                )
            }
            reportes.forEach { reporte ->
                add(
                    HistorialItem(
                        "Problema: ${reporte.tipoProblema}",
                        "Reportado: ${reporte.descripcion}",
                        reporte.estado,
                        reporte.fechaReporte,
                        reporte.sectorId,
                        reporte.cultivoId
                    )
                )
                reporte.comentarioSeguimiento.lineSequence()
                    .filter(String::isNotBlank)
                    .forEach { nota ->
                        add(
                            HistorialItem(
                                "Seguimiento de problema",
                                nota,
                                reporte.estado,
                                nota.take(19),
                                reporte.sectorId,
                                reporte.cultivoId
                            )
                        )
                    }
                if (reporte.fechaResolucion.isNotBlank()) {
                    add(
                        HistorialItem(
                            "Resolución de problema",
                            reporte.comentarioSeguimiento,
                            reporte.estado,
                            reporte.fechaResolucion,
                            reporte.sectorId,
                            reporte.cultivoId
                        )
                    )
                }
            }
            uiState.cultivos.filter { it.fechaCambioEstado.isNotBlank() }.forEach { cultivo ->
                add(
                    HistorialItem(
                        "Cambio de estado: ${cultivo.nombreComun}",
                        "El cultivo cambió a ${cultivo.estadoDesarrollo}",
                        cultivo.estadoDesarrollo,
                        cultivo.fechaCambioEstado,
                        cultivo.sectorId,
                        cultivo.id
                    )
                )
            }
            uiState.sectores.filter { it.estado == "En mantenimiento" }.forEach { sector ->
                listOf(
                    "Inicio" to sector.fechaInicioMantenimiento,
                    "Término" to sector.fechaTerminoMantenimiento
                ).filter { it.second.isNotBlank() }.forEach { (evento, fecha) ->
                    add(
                        HistorialItem(
                            "$evento de mantenimiento: ${sector.nombre}",
                            "Sector en mantenimiento",
                            sector.estado,
                            fecha,
                            sector.id,
                            null
                        )
                    )
                }
            }
        }.filter { item ->
            (sectorFiltro.isBlank() || item.sectorId == sectorFiltro) &&
                (cultivoFiltro.isBlank() || item.cultivoId == cultivoFiltro) &&
                (desde.isBlank() || item.fecha.take(10) >= desde.trim()) &&
                (hasta.isBlank() || item.fecha.take(10) <= hasta.trim())
        }.sortedByDescending { it.fecha }

        if (items.isEmpty()) {
            Text("Todavía no hay actividades registradas.")
        }
        items.forEach { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(item.descripcion)
                    HuertoStatusChip(item.estado)
                    Text("Fecha: ${item.fecha}")
                    item.foto?.let { ruta ->
                        AsyncImage(
                            model = Uri.fromFile(File(ruta)),
                            contentDescription = "Fotografía del historial",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

private data class HistorialItem(
    val titulo: String,
    val descripcion: String,
    val estado: String,
    val fecha: String,
    val sectorId: String,
    val cultivoId: String?,
    val foto: String? = null
)

@Composable
fun CalendarioHuertoScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT)
        .format(java.util.Date())
    ScreenLayout("Próximos eventos del huerto", onBack) {
        Text("Cosechas estimadas", style = MaterialTheme.typography.titleLarge)
        val cosechas = uiState.cultivos.filter {
            it.estadoDesarrollo != "Cosechado" &&
                it.fechaCosechaEstimada.isNotBlank() &&
                it.fechaCosechaEstimada >= hoy
        }.sortedBy { it.fechaCosechaEstimada }
        cosechas.forEach { cultivo ->
            val sector = uiState.sectores.firstOrNull { it.id == cultivo.sectorId }
            Text("${cultivo.nombreComun} · ${cultivo.fechaCosechaEstimada} · ${sector?.nombre ?: "Sector"}")
        }
        if (cosechas.isEmpty()) Text("No hay cosechas próximas registradas.")
        Text("Mantenimientos programados", style = MaterialTheme.typography.titleLarge)
        val mantenimientos = uiState.sectores.filter {
            it.estado == "En mantenimiento" &&
                it.fechaTerminoMantenimiento >= hoy
        }.sortedBy { it.fechaInicioMantenimiento }
        mantenimientos.forEach { sector ->
            Text("${sector.nombre} · ${sector.fechaInicioMantenimiento} al ${sector.fechaTerminoMantenimiento}")
        }
        if (mantenimientos.isEmpty()) Text("No hay mantenimientos próximos registrados.")
    }
}

@Composable
fun GaleriaHuertoScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    ScreenLayout("Galería del huerto", onBack) {
        Text("Imágenes de demostración de plantas y herramientas; no se admiten rostros.")
        val tareasVisibles = viewModel.tareasVisibles.map { it.id }.toSet()
        val evidencias = uiState.evidencias.filter { evidencia ->
            when {
                evidencia.tareaId != null -> evidencia.tareaId in tareasVisibles
                evidencia.reporteId != null -> viewModel.esAdministrador ||
                    uiState.reportesProblemas.any {
                        it.id == evidencia.reporteId && it.usuarioId == uiState.usuarioActual?.id
                    }
                else -> true
            }
        }.sortedByDescending { it.fechaRegistro }
        if (evidencias.isEmpty()) Text("Todavía no hay fotografías registradas.")
        evidencias.forEach { evidencia ->
            val titulo = when {
                evidencia.tareaId != null ->
                    uiState.tareas.firstOrNull { it.id == evidencia.tareaId }?.titulo ?: "Tarea"
                evidencia.cultivoId != null ->
                    uiState.cultivos.firstOrNull { it.id == evidencia.cultivoId }?.nombreComun ?: "Cultivo"
                evidencia.reporteId != null -> "Reporte de problema"
                else -> "Registro fotográfico"
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text("Fecha: ${evidencia.fechaRegistro}")
                AsyncImage(
                    model = Uri.fromFile(File(evidencia.rutaArchivo)),
                    contentDescription = "Fotografía del huerto",
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun ReportesProblemasScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val uiState = collectHuertoUiState(viewModel)
    var mostrarFormulario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var seguimiento by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var estadoFiltro by remember { mutableStateOf("") }

    ScreenLayout(
        if (viewModel.esAdministrador) "Problemas del huerto" else "Mis reportes de problemas",
        onBack
    ) {
        Button(onClick = {
            error = null
            mostrarFormulario = true
        }) { Text("Registrar problema") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        uiState.errorReportesProblemas?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (viewModel.esAdministrador) {
            CampoSeleccion(
                etiqueta = "Filtrar por estado",
                opciones = listOf("" to "Todos") +
                    listOf("Informado", "En revisión", "Atendido", "Cerrado").map { it to it },
                seleccionado = estadoFiltro,
                onSeleccionar = { estadoFiltro = it }
            )
        }
        if (uiState.cargandoReportesProblemas) {
            androidx.compose.material3.CircularProgressIndicator()
        }

        val reportes = if (viewModel.esAdministrador) {
            uiState.reportesProblemas
        } else {
            val usuarioId = uiState.usuarioActual?.id
            uiState.reportesProblemas.filter { it.usuarioId == usuarioId }
        }
        if (reportes.isEmpty() && !uiState.cargandoReportesProblemas &&
            uiState.errorReportesProblemas == null
        ) {
            Text(
                if (viewModel.esAdministrador) "No hay problemas reportados."
                else "Todavía no has reportado problemas."
            )
        }
        reportes.filter { estadoFiltro.isBlank() || it.estado == estadoFiltro }.forEach { reporte ->
            val sector = uiState.sectores.firstOrNull { it.id == reporte.sectorId }
            val cultivo = uiState.cultivos.firstOrNull { it.id == reporte.cultivoId }
            val reportante = uiState.usuariosDisponibles
                .firstOrNull { it.id == reporte.usuarioId }
                ?: uiState.usuarioActual?.takeIf { it.id == reporte.usuarioId }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(reporte.descripcion, style = MaterialTheme.typography.titleMedium)
                    Text("Tipo: ${reporte.tipoProblema}")
                    Text("Sector: ${sector?.nombre ?: "No disponible"}")
                    Text("Cultivo: ${cultivo?.nombreComun ?: "No especificado"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Prioridad:")
                        HuertoStatusChip(reporte.prioridad)
                    }
                    Text("Reportado por: ${reportante?.nombreCompleto ?: "Usuario"}")
                    Text("Fecha: ${reporte.fechaReporte}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estado:")
                        HuertoStatusChip(reporte.estado)
                    }
                    EvidenciaFotograficaSection(
                        titulo = "Fotografías del reporte",
                        evidencias = viewModel.evidenciasDeReporte(reporte.id),
                        editable = viewModel.esAdministrador ||
                            reporte.usuarioId == uiState.usuarioActual?.id,
                        onAgregarFoto = { uri, onResult ->
                            viewModel.agregarEvidencia(
                                TipoAsociacionEvidencia.REPORTE,
                                reporte.id,
                                uri,
                                onResult
                            )
                        },
                        onEliminarFoto = { evidencia, onResult ->
                            viewModel.eliminarEvidencia(evidencia.id, onResult)
                        }
                    )
                    if (viewModel.esAdministrador) {
                        OutlinedTextField(
                            value = seguimiento[reporte.id].orEmpty(),
                            onValueChange = { seguimiento = seguimiento + (reporte.id to it) },
                            label = { Text("Comentario de seguimiento") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        CampoSeleccion(
                            etiqueta = "Cambiar estado",
                            opciones = listOf(
                                "Informado" to "Informado",
                                "En revisión" to "En revisión",
                                "Atendido" to "Atendido",
                                "Cerrado" to "Cerrado"
                            ),
                            seleccionado = reporte.estado,
                            onSeleccionar = {
                                viewModel.actualizarEstadoReporte(
                                    reporte.id,
                                    it,
                                    seguimiento[reporte.id].orEmpty()
                                ) { errorEstado ->
                                    error = errorEstado
                                    if (errorEstado == null) {
                                        seguimiento = seguimiento - reporte.id
                                    }
                                }
                            }
                        )
                        if (reporte.fechaResolucion.isNotBlank()) {
                            Text("Resolución/cierre: ${reporte.fechaResolucion}")
                        }
                        if (reporte.comentarioSeguimiento.isNotBlank()) {
                            Text("Seguimiento: ${reporte.comentarioSeguimiento}")
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioReporteProblema(
            sectores = uiState.sectores,
            cultivos = uiState.cultivos,
            onDismiss = { mostrarFormulario = false },
            onSave = { descripcion, sectorId, cultivoId, prioridad, tipoProblema, fotografia, onResult ->
                viewModel.crearReporteProblema(
                    descripcion,
                    sectorId,
                    cultivoId,
                    prioridad,
                    tipoProblema,
                    fotografia
                ) { resultado ->
                    error = resultado
                    if (resultado == null) mostrarFormulario = false
                    onResult(resultado)
                }
            }
        )
    }
}

@Composable
private fun FormularioReporteProblema(
    sectores: List<Sector>,
    cultivos: List<Cultivo>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, String, Uri?, (String?) -> Unit) -> Unit
) {
    var descripcion by remember { mutableStateOf("") }
    var sectorId by remember { mutableStateOf("") }
    var cultivoId by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf("Media") }
    var tipoProblema by remember { mutableStateOf("Otro") }
    var fotografia by remember { mutableStateOf<Uri?>(null) }
    var mostrarError by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    val selectorFotografia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) fotografia = uri
    }
    val cultivosDelSector = cultivos.filter { it.sectorId == sectorId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar problema") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        mostrarError = false
                    },
                    label = { Text("Descripción del problema") },
                    isError = mostrarError && descripcion.isBlank(),
                    supportingText = {
                        if (mostrarError && descripcion.isBlank()) Text("La descripción es obligatoria.")
                    }
                )
                CampoSeleccion(
                    etiqueta = "Sector",
                    opciones = sectores.map { it.id to it.nombre },
                    seleccionado = sectorId,
                    onSeleccionar = {
                        sectorId = it
                        cultivoId = ""
                    }
                )
                CampoSeleccion(
                    etiqueta = "Cultivo (opcional)",
                    opciones = listOf("" to "Sin cultivo específico") +
                        cultivosDelSector.map { it.id to it.nombreComun },
                    seleccionado = cultivoId,
                    onSeleccionar = { cultivoId = it }
                )
                CampoSeleccion(
                    etiqueta = "Tipo de problema",
                    opciones = HuertoViewModel.TIPOS_PROBLEMA.map { it to it },
                    seleccionado = tipoProblema,
                    onSeleccionar = { tipoProblema = it }
                )
                CampoSeleccion(
                    etiqueta = "Prioridad",
                    opciones = listOf("Baja", "Media", "Alta").map { it to it },
                    seleccionado = prioridad,
                    onSeleccionar = { prioridad = it }
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        selectorFotografia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                ) { Text(if (fotografia == null) "Adjuntar fotografía" else "Cambiar fotografía") }
                if (mostrarError && fotografia == null) {
                    Text(
                        "Adjunta una fotografía referencial para enviar el reporte.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                fotografia?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Vista previa de fotografía del problema",
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text("Usa una imagen de prueba de plantas o herramientas; no incluyas rostros ni datos personales.")
                if (sectores.isEmpty()) {
                    Text("Primero debe existir al menos un sector.")
                }
                if (mostrarError && sectorId.isBlank()) {
                    Text("Selecciona el sector afectado.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                mostrarError = descripcion.isBlank() || sectorId.isBlank() || fotografia == null
                if (!mostrarError && !guardando) {
                    guardando = true
                    onSave(
                        descripcion,
                        sectorId,
                        cultivoId.ifBlank { null },
                        prioridad,
                        tipoProblema,
                        fotografia
                    ) {
                        guardando = false
                        if (it != null) mostrarError = true
                    }
                }
            }, enabled = !guardando) {
                Text(if (guardando) "Enviando..." else "Enviar reporte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !guardando) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ReporteDato(etiqueta: String, valor: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(etiqueta)
            Text(valor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FormularioSector(
    sector: Sector? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Boolean
) {
    var nombre by remember(sector?.id) { mutableStateOf(sector?.nombre.orEmpty()) }
    var ubicacion by remember(sector?.id) { mutableStateOf(sector?.ubicacion.orEmpty()) }
    var descripcion by remember(sector?.id) { mutableStateOf(sector?.descripcion.orEmpty()) }
    var estado by remember(sector?.id) { mutableStateOf(sector?.estado ?: "Disponible") }
    var inicioMantenimiento by remember(sector?.id) {
        mutableStateOf(sector?.fechaInicioMantenimiento.orEmpty())
    }
    var terminoMantenimiento by remember(sector?.id) {
        mutableStateOf(sector?.fechaTerminoMantenimiento.orEmpty())
    }
    var mostrarError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sector == null) "Agregar sector" else "Editar sector") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    isError = mostrarError && nombre.isBlank()
                )
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    singleLine = true,
                    isError = mostrarError && ubicacion.isBlank()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    isError = mostrarError && descripcion.isBlank()
                )
                CampoSeleccion(
                    etiqueta = "Estado",
                    opciones = listOf("Disponible", "En uso", "Inactivo", "En mantenimiento").map { it to it },
                    seleccionado = estado,
                    onSeleccionar = {
                        estado = it
                        if (it != "En mantenimiento") {
                            inicioMantenimiento = ""
                            terminoMantenimiento = ""
                        }
                    }
                )
                if (estado == "En mantenimiento") {
                    OutlinedTextField(
                        value = inicioMantenimiento,
                        onValueChange = { inicioMantenimiento = it },
                        label = { Text("Inicio del mantenimiento (AAAA-MM-DD)") },
                        singleLine = true,
                        isError = mostrarError
                    )
                    OutlinedTextField(
                        value = terminoMantenimiento,
                        onValueChange = { terminoMantenimiento = it },
                        label = { Text("Término del mantenimiento (AAAA-MM-DD)") },
                        singleLine = true,
                        isError = mostrarError
                    )
                }
                if (mostrarError) {
                    Text(
                        "Completa el nombre, la ubicación y la descripción.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                mostrarError = !onSave(
                    nombre,
                    ubicacion,
                    descripcion,
                    estado,
                    inicioMantenimiento,
                    terminoMantenimiento
                )
            }) {
                Text(if (sector == null) "Guardar" else "Guardar cambios")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun FormularioCultivo(
    cultivo: Cultivo? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Boolean
) {
    var nombre by remember(cultivo?.id) { mutableStateOf(cultivo?.nombreComun.orEmpty()) }
    var fechaSiembra by remember(cultivo?.id) { mutableStateOf(cultivo?.fechaSiembra.orEmpty()) }
    var fechaCosechaEstimada by remember(cultivo?.id) {
        mutableStateOf(cultivo?.fechaCosechaEstimada.orEmpty())
    }
    var estado by remember(cultivo?.id) {
        mutableStateOf(cultivo?.estadoDesarrollo ?: "Planificado")
    }
    var frecuencia by remember(cultivo?.id) { mutableStateOf(cultivo?.frecuenciaRiego.orEmpty()) }
    var mostrarError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cultivo == null) "Agregar cultivo" else "Editar cultivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del cultivo") },
                    singleLine = true,
                    isError = mostrarError && nombre.isBlank()
                )
                OutlinedTextField(
                    value = fechaSiembra,
                    onValueChange = { fechaSiembra = it },
                    label = { Text("Fecha de siembra (AAAA-MM-DD)") },
                    singleLine = true,
                    isError = mostrarError
                )
                OutlinedTextField(
                    value = fechaCosechaEstimada,
                    onValueChange = { fechaCosechaEstimada = it },
                    label = { Text("Cosecha estimada (AAAA-MM-DD)") },
                    singleLine = true,
                    isError = mostrarError
                )
                CampoSeleccion(
                    etiqueta = "Estado de desarrollo",
                    opciones = (if (cultivo == null) {
                        EstadoCultivo.opciones.filter { it in setOf("Planificado", "Sembrado") }
                    } else {
                        EstadoCultivo.opciones
                    }).map { it to it },
                    seleccionado = estado,
                    onSeleccionar = { estado = it }
                )
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = { frecuencia = it },
                    label = { Text("Frecuencia de riego (días)") },
                    singleLine = true,
                    isError = mostrarError && frecuencia.isBlank()
                )
                if (mostrarError) {
                    Text(
                        "Completa todos los campos con fechas válidas; la cosecha estimada debe ser igual o posterior a la siembra.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                mostrarError = !onSave(
                    nombre,
                    fechaSiembra,
                    fechaCosechaEstimada,
                    estado,
                    frecuencia
                )
            }) { Text(if (cultivo == null) "Guardar" else "Guardar cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun FormularioTarea(
    sectores: List<Sector>,
    cultivos: List<Cultivo>,
    estudiantes: List<Usuario>,
    tarea: Tarea? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String, String, String) -> Boolean
) {
    var titulo by remember(tarea?.id) { mutableStateOf(tarea?.titulo.orEmpty()) }
    var instrucciones by remember(tarea?.id) { mutableStateOf(tarea?.descripcion.orEmpty()) }
    var sectorId by remember(tarea?.id) { mutableStateOf(tarea?.sectorId.orEmpty()) }
    var cultivoId by remember(tarea?.id) { mutableStateOf(tarea?.cultivoId) }
    var usuarioId by remember(tarea?.id) { mutableStateOf(tarea?.asignadoA.orEmpty()) }
    var fechaProgramada by remember(tarea?.id) {
        mutableStateOf(tarea?.fechaProgramada.orEmpty())
    }
    var tipo by remember(tarea?.id) {
        mutableStateOf(tarea?.tipo ?: TipoTarea.MANTENIMIENTO)
    }
    var mostrarError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tarea == null) "Crear y asignar tarea" else "Editar tarea") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(instrucciones, { instrucciones = it }, label = { Text("Instrucciones") })
                OutlinedTextField(
                    fechaProgramada,
                    { fechaProgramada = it },
                    label = { Text("Fecha programada (AAAA-MM-DD)") },
                    singleLine = true
                )
                CampoSeleccion(
                    etiqueta = "Tipo de tarea",
                    opciones = TipoTarea.opciones.map { it to it },
                    seleccionado = tipo,
                    onSeleccionar = { tipo = it }
                )
                CampoSeleccion(
                    etiqueta = "Sector",
                    opciones = sectores.map { it.id to it.nombre },
                    seleccionado = sectorId,
                    onSeleccionar = {
                        sectorId = it
                        if (cultivos.none { cultivo -> cultivo.id == cultivoId && cultivo.sectorId == it }) {
                            cultivoId = null
                        }
                    }
                )
                CampoSeleccion(
                    etiqueta = "Cultivo (opcional)",
                    opciones = listOf("" to "Sin cultivo asociado") +
                        cultivos.filter { it.sectorId == sectorId }.map { it.id to it.nombreComun },
                    seleccionado = cultivoId.orEmpty(),
                    onSeleccionar = { cultivoId = it.ifBlank { null } }
                )
                CampoSeleccion(
                    etiqueta = "Estudiante o grupo",
                    opciones = estudiantes.map { it.id to it.nombreCompleto } +
                        if (tarea == null) estudiantes.map { it.tallerAsignado }
                            .filter(String::isNotBlank).distinct().map {
                            HuertoViewModel.GROUP_ASSIGNMENT_PREFIX + it to "Grupo: $it"
                        } else emptyList(),
                    seleccionado = usuarioId,
                    onSeleccionar = { usuarioId = it }
                )
                if (sectores.isEmpty() || estudiantes.isEmpty()) {
                    Text("Se necesita al menos un sector y un estudiante registrado.")
                }
                if (mostrarError) {
                    Text(
                        "Completa los campos, selecciona sector y estudiante e ingresa una fecha válida (AAAA-MM-DD).",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                mostrarError = !onSave(
                    titulo,
                    instrucciones,
                    sectorId,
                    cultivoId,
                    usuarioId,
                    fechaProgramada,
                    tipo
                )
            }) { Text(if (tarea == null) "Crear" else "Guardar cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CampoSeleccion(
    etiqueta: String,
    opciones: List<Pair<String, String>>,
    seleccionado: String,
    onSeleccionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val nombre = opciones.firstOrNull { it.first == seleccionado }?.second ?: "Seleccionar $etiqueta"
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { expandido = true }, enabled = opciones.isNotEmpty()) {
            Text(nombre)
        }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { (id, valor) ->
                DropdownMenuItem(
                    text = { Text(valor) },
                    onClick = {
                        onSeleccionar(id)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ScreenLayout(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        content()
    }
}
