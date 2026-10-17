package com.example.huertolindo.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huertolindo.data.model.Evidencia
import com.example.huertolindo.ui.crearUriFotografia
import com.example.huertolindo.ui.eliminarFotografiaTemporal
import java.io.File
import java.io.IOException

@Composable
fun EvidenciaFotograficaSection(
    titulo: String,
    evidencias: List<Evidencia>,
    editable: Boolean = true,
    onAgregarFoto: (Uri, (String?) -> Unit) -> Unit,
    onEliminarFoto: (Evidencia, (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val fotosPendientes = remember { mutableStateListOf<Uri>() }
    var error by remember { mutableStateOf<String?>(null) }
    var uriCamaraPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    fun guardarFoto(uri: Uri) {
        fotosPendientes.add(uri)
        error = null
        onAgregarFoto(uri) { resultado ->
            fotosPendientes.remove(uri)
            error = resultado
            eliminarFotografiaTemporal(context, uri)
        }
    }

    val selectorFotos = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) guardarFoto(uri)
    }
    val camara = rememberLauncherForActivityResult(TakePicture()) { capturada ->
        val uri = uriCamaraPendiente?.let(Uri::parse)
        uriCamaraPendiente = null
        if (capturada && uri != null) {
            guardarFoto(uri)
        } else if (uri != null) {
            eliminarFotografiaTemporal(context, uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        if (editable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selectorFotos.launch(
                        PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text("Elegir de galería")
                }
                OutlinedButton(onClick = {
                    error = null
                    var uri: Uri? = null
                    try {
                        uri = crearUriFotografia(context)
                        uriCamaraPendiente = uri.toString()
                        camara.launch(uri)
                    } catch (exception: IOException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        error = "No se pudo preparar la cámara."
                    } catch (exception: SecurityException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        error = "No se pudo acceder a la cámara."
                    } catch (exception: ActivityNotFoundException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        error = "No hay una aplicación de cámara disponible."
                    } catch (exception: IllegalArgumentException) {
                        uri?.let { eliminarFotografiaTemporal(context, it) }
                        error = "No se pudo preparar el archivo para la cámara."
                    } finally {
                        if (error != null) uriCamaraPendiente = null
                    }
                }) {
                    Text("Tomar foto")
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        evidencias.forEach { evidencia ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AsyncImage(
                    model = Uri.fromFile(File(evidencia.rutaArchivo)),
                    contentDescription = "Fotografía adjunta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                Text("Adjuntada: ${evidencia.fechaRegistro}")
                if (editable) {
                    TextButton(
                        onClick = {
                            onEliminarFoto(evidencia) { resultado -> error = resultado }
                        }
                    ) {
                        Text("Quitar fotografía")
                    }
                }
            }
        }
        fotosPendientes.forEach { uri ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Vista previa de la fotografía",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
                Text("Guardando fotografía…")
            }
        }
    }
}
