package com.example.huertolindo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.data.model.SensorSimulado
import com.example.huertolindo.data.model.Sector
import com.example.huertolindo.data.model.Usuario
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun SectoresScreen(
    viewModel: HuertoViewModel,
    onBack: () -> Unit,
    onSectorClick: (String) -> Unit,
    esAdministrador: Boolean = false
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ScreenLayout("Sectores del huerto", onBack) {
        if (esAdministrador) {
            Button(onClick = { mostrarFormulario = true }) { Text("Agregar sector") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (viewModel.listaSectores.value.isEmpty()) {
            Text("Todavía no hay sectores registrados.")
        }
        viewModel.listaSectores.value.forEach { sector ->
            ElevatedCard(
                onClick = { onSectorClick(sector.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(sector.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(sector.descripcion)
                    Text("Estado: ${sector.estado}")
                    Text("${viewModel.listaCultivos.value.count { it.sectorId == sector.id }} cultivos")
                    if (esAdministrador) {
                        OutlinedButton(onClick = {
                            error = if (viewModel.eliminarSector(sector.id)) null
                            else "No se puede eliminar un sector que tiene cultivos o tareas."
                        }) { Text("Eliminar sector") }
                    }
                }
            }
        }
    }
    if (mostrarFormulario) {
        FormularioSector(
            onDismiss = { mostrarFormulario = false },
            onSave = { nombre, descripcion ->
                if (viewModel.agregarSector(nombre, descripcion)) {
                    error = null
                    mostrarFormulario = false
                } else {
                    error = "Ingresa el nombre y la descripción del sector."
                }
            }
        )
    }
}

@Composable
fun DetalleSectorScreen(
    viewModel: HuertoViewModel,
    sectorId: String,
    onBack: () -> Unit,
    onCultivoClick: (String) -> Unit,
    esAdministrador: Boolean = false
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sector = viewModel.listaSectores.value.firstOrNull { it.id == sectorId }
    ScreenLayout(sector?.nombre ?: "Sector no encontrado", onBack) {
        if (sector == null) {
            Text("El sector seleccionado ya no está disponible.")
            return@ScreenLayout
        }
        Text(sector.descripcion, style = MaterialTheme.typography.bodyLarge)
        Text("Estado: ${sector.estado}", style = MaterialTheme.typography.bodyLarge)
        Text("Cultivos", style = MaterialTheme.typography.titleLarge)
        if (esAdministrador) {
            Button(onClick = { mostrarFormulario = true }) { Text("Agregar cultivo") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        val cultivos = viewModel.listaCultivos.value.filter { it.sectorId == sector.id }
        if (cultivos.isEmpty()) Text("Este sector todavía no tiene cultivos.")
        cultivos.forEach { cultivo ->
            ElevatedCard(
                onClick = { onCultivoClick(cultivo.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(cultivo.nombreComun, style = MaterialTheme.typography.titleMedium)
                    Text("Desarrollo: ${cultivo.estadoDesarrollo}")
                    Text("Siembra: ${cultivo.fechaSiembra}")
                    if (esAdministrador) {
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
            onDismiss = { mostrarFormulario = false },
            onSave = { nombre, fecha, frecuencia ->
                if (viewModel.agregarCultivo(nombre, sectorId, fecha, frecuencia)) {
                    error = null
                    mostrarFormulario = false
                } else {
                    error = "Completa todos los datos del cultivo."
                }
            }
        )
    }
}

@Composable
fun DetalleCultivoScreen(viewModel: HuertoViewModel, cultivoId: String, onBack: () -> Unit) {
    val cultivo = viewModel.listaCultivos.value.firstOrNull { it.id == cultivoId }
    ScreenLayout(cultivo?.nombreComun ?: "Cultivo no encontrado", onBack) {
        if (cultivo == null) {
            Text("El cultivo seleccionado ya no está disponible.")
            return@ScreenLayout
        }
        val sector = viewModel.listaSectores.value.firstOrNull { it.id == cultivo.sectorId }
        Text("Sector: ${sector?.nombre ?: "Sin sector"}")
        Text("Fecha de siembra: ${cultivo.fechaSiembra}")
        Text("Estado de desarrollo: ${cultivo.estadoDesarrollo}")
        Text("Frecuencia de riego: ${cultivo.frecuenciaRiego}")
    }
}

@Composable
fun TareasScreen(
    viewModel: HuertoViewModel,
    onBack: () -> Unit,
    esAdministrador: Boolean = false
) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val usuarioActual = viewModel.usuarioActual
    val tareas = if (esAdministrador) {
        viewModel.listaTareas.value
    } else {
        viewModel.listaTareas.value.filter { it.asignadoA == usuarioActual?.id }
    }
    ScreenLayout("Tareas del huerto", onBack) {
        if (esAdministrador) {
            Button(onClick = { mostrarFormulario = true }) { Text("Crear y asignar tarea") }
            viewModel.errorCargaUsuarios?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (tareas.isEmpty()) Text("No tienes tareas asignadas.")
        tareas.forEach { tarea ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(tarea.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(tarea.descripcion)
                    Text("Sector: ${viewModel.listaSectores.value.firstOrNull { it.id == tarea.sectorId }?.nombre ?: "No asignado"}")
                    val asignado = viewModel.usuariosDisponibles.value.firstOrNull { it.id == tarea.asignadoA }
                    val nombreAsignado = asignado?.nombreCompleto ?: tarea.asignadoA
                    Text("Asignada a: $nombreAsignado · ${tarea.estado}")
                    if (esAdministrador && tarea.estado == "Por validar") {
                        Button(onClick = {
                            viewModel.validarTarea(tarea.id)
                        }) {
                            Text("Validar actividad")
                        }
                    } else if (!esAdministrador && tarea.estado == "Pendiente") {
                        Button(onClick = { viewModel.enviarTareaARevision(tarea.id) }) {
                            Text("Enviar a revisión")
                        }
                    }
                }
            }
        }
    }
    if (mostrarFormulario) {
        FormularioTarea(
            sectores = viewModel.listaSectores.value,
            estudiantes = viewModel.usuariosDisponibles.value.filter { it.perfil == "estudiante" },
            onDismiss = { mostrarFormulario = false },
            onSave = { titulo, descripcion, sectorId, usuarioId ->
                if (viewModel.crearTarea(titulo, descripcion, sectorId, usuarioId)) {
                    error = null
                    mostrarFormulario = false
                } else {
                    error = "Completa los datos y selecciona un sector y estudiante."
                }
            }
        )
    }
}

@Composable
fun SensoresScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    ScreenLayout("Sensores del huerto", onBack) {
        Text("Lecturas de demostración; no corresponden a sensores físicos.")
        viewModel.listaSectores.value.forEach { sector ->
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
                }
            }
        }
    }
}

@Composable
fun ReportesScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    val tareas = viewModel.listaTareas.value
    ScreenLayout("Resumen del huerto", onBack) {
        ReporteDato("Sectores", viewModel.listaSectores.value.size.toString())
        ReporteDato("Cultivos activos", viewModel.listaCultivos.value.size.toString())
        ReporteDato("Tareas completadas", tareas.count { it.estado == "Completada" }.toString())
        ReporteDato("Tareas pendientes", tareas.count { it.estado != "Completada" }.toString())
        Text(
            "Este resumen se actualiza al cambiar el estado de las tareas.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
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
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar sector") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(nombre, descripcion) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun FormularioCultivo(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var fechaSiembra by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar cultivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del cultivo") }, singleLine = true)
                OutlinedTextField(fechaSiembra, { fechaSiembra = it }, label = { Text("Fecha de siembra") }, singleLine = true)
                OutlinedTextField(frecuencia, { frecuencia = it }, label = { Text("Frecuencia de riego") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(nombre, fechaSiembra, frecuencia) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun FormularioTarea(
    sectores: List<Sector>,
    estudiantes: List<Usuario>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var sectorId by remember { mutableStateOf("") }
    var usuarioId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear y asignar tarea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción") })
                CampoSeleccion(
                    etiqueta = "Sector",
                    opciones = sectores.map { it.id to it.nombre },
                    seleccionado = sectorId,
                    onSeleccionar = { sectorId = it }
                )
                CampoSeleccion(
                    etiqueta = "Estudiante",
                    opciones = estudiantes.map { it.id to it.nombreCompleto },
                    seleccionado = usuarioId,
                    onSeleccionar = { usuarioId = it }
                )
                if (sectores.isEmpty() || estudiantes.isEmpty()) {
                    Text("Se necesita al menos un sector y un estudiante registrado.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(titulo, descripcion, sectorId, usuarioId) }) { Text("Crear") }
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
