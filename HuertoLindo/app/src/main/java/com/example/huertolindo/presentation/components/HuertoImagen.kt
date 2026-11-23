package com.example.huertolindo.presentation.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.huertolindo.data.remote.RetrofitClient
import java.io.File

fun resolverModeloImagen(context: Context, ruta: Any?): Any? {
    if (ruta == null) return null
    if (ruta is Uri || ruta is File) return ruta
    val str = ruta.toString().trim()
    if (str.isBlank()) return null

    // 1. URLs completas remotas
    if (str.startsWith("http://", ignoreCase = true) || str.startsWith("https://", ignoreCase = true)) {
        return str.replace("http://localhost:8080", RetrofitClient.SERVER_URL)
            .replace("http://127.0.0.1:8080", RetrofitClient.SERVER_URL)
    }

    // 2. URIs de Android (content:// o file://)
    if (str.startsWith("content://", ignoreCase = true) || str.startsWith("file://", ignoreCase = true)) {
        return Uri.parse(str)
    }

    // 3. Archivo local existente en el dispositivo (prevalece sobre startsWith("/"))
    val archivoDirecto = File(str)
    if (archivoDirecto.exists() && archivoDirecto.isFile) {
        return Uri.fromFile(archivoDirecto)
    }

    // 4. Búsqueda en el directorio interno de evidencias de la app por nombre de archivo
    val nombre = archivoDirecto.name
    if (nombre.isNotBlank()) {
        val archivoEnEvidencias = File(File(context.filesDir, "evidencias"), nombre)
        if (archivoEnEvidencias.exists() && archivoEnEvidencias.isFile) {
            return Uri.fromFile(archivoEnEvidencias)
        }
        val archivoEnFiles = File(context.filesDir, nombre)
        if (archivoEnFiles.exists() && archivoEnFiles.isFile) {
            return Uri.fromFile(archivoEnFiles)
        }
    }

    // 5. Rutas relativas del servidor backend (como "/uploads/..." o "uploads/...")
    val cleanStr = str.replace('\\', '/')
    if (cleanStr.startsWith("/uploads/") || cleanStr.startsWith("uploads/")) {
        val path = if (cleanStr.startsWith("/")) cleanStr else "/$cleanStr"
        return "${RetrofitClient.SERVER_URL}$path"
    }

    // 6. Si empieza con "/" pero no es una ruta interna de Linux/Android (/data/ o /storage/)
    if (cleanStr.startsWith("/") && !cleanStr.startsWith("/data/") && !cleanStr.startsWith("/storage/")) {
        return "${RetrofitClient.SERVER_URL}$cleanStr"
    }

    // Fallback
    return if (archivoDirecto.exists()) Uri.fromFile(archivoDirecto) else str
}

@Composable
fun HuertoImagen(
    modelo: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    altura: Dp = 200.dp,
    forma: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val modeloFinal = remember(modelo) {
        resolverModeloImagen(context, modelo)
    }

    if (modeloFinal == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(altura)
                .clip(forma)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin fotografía",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(modeloFinal)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
                .fillMaxWidth()
                .height(altura)
                .clip(forma),
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fotografía no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            success = {
                SubcomposeAsyncImageContent(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        )
    }
}
