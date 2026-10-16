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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.huertolindo.data.model.SensorSimulado
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun SectoresScreen(viewModel: HuertoViewModel, onBack: () -> Unit, onSectorClick: (String) -> Unit) {
    ScreenLayout("Sectores del huerto", onBack) {
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
                }
            }
        }
    }
}

@Composable
fun DetalleSectorScreen(
    viewModel: HuertoViewModel,
    sectorId: String,
    onBack: () -> Unit,
    onCultivoClick: (String) -> Unit
) {
    val sector = viewModel.listaSectores.value.firstOrNull { it.id == sectorId }
    ScreenLayout(sector?.nombre ?: "Sector no encontrado", onBack) {
        if (sector == null) {
            Text("El sector seleccionado ya no está disponible.")
            return@ScreenLayout
        }
        Text(sector.descripcion, style = MaterialTheme.typography.bodyLarge)
        Text("Estado: ${sector.estado}", style = MaterialTheme.typography.bodyLarge)
        Text("Cultivos", style = MaterialTheme.typography.titleLarge)
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
                }
            }
        }
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
fun TareasScreen(viewModel: HuertoViewModel, onBack: () -> Unit) {
    ScreenLayout("Tareas del huerto", onBack) {
        val tareas = viewModel.listaTareas.value
        if (tareas.isEmpty()) Text("No hay tareas pendientes.")
        tareas.forEach { tarea ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(tarea.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(tarea.descripcion)
                    Text("Sector: ${viewModel.listaSectores.value.firstOrNull { it.id == tarea.sectorId }?.nombre ?: "No asignado"}")
                    Text("Asignada a: ${tarea.asignadoA} · ${tarea.estado}")
                    if (tarea.estado == "Completada") {
                        OutlinedButton(onClick = { viewModel.cambiarEstadoTarea(tarea.id) }) {
                            Text("Marcar pendiente")
                        }
                    } else {
                        Button(onClick = { viewModel.cambiarEstadoTarea(tarea.id) }) {
                            Text("Completar tarea")
                        }
                    }
                }
            }
        }
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
