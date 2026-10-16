package com.example.huertolindo.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun FotoScreen(
    nombreUsuario: String,
    fotoPerfil: Uri?,
    onFotoPerfilChange: (Uri?) -> Unit,
    onVolver: () -> Unit
) {
    val selectorFotos = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) onFotoPerfilChange(uri)
    }

    val context = LocalContext.current
    var uriPendiente by rememberSaveable() { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakePicture()
    ) { fotoTomada ->
        if (fotoTomada) onFotoPerfilChange(uriPendiente)
        uriPendiente = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onVolver) {
                Text("Volver")
            }
        }

        Text(
            text = "Perfil de $nombreUsuario",
            style = MaterialTheme.typography.headlineMedium
        )

        AsyncImage(
            model = fotoPerfil,
            contentDescription = "Foto de perfil de $nombreUsuario",
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Button(
            onClick = {
                selectorFotos.launch(
                    PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                )
            }
        ) {
            Text("Elegir foto del teléfono")
        }

        Button(
            onClick = {
                try {
                    val nuevaUri = crearUriFotografia(context)
                    uriPendiente = nuevaUri
                    cameraLauncher.launch(nuevaUri)
                } catch (e: Exception) {
                    uriPendiente = null
                    Log.e("FotoScreen", "No se pudo iniciar la captura de foto", e)
                    Toast.makeText(
                        context,
                        "No se pudo abrir la cámara.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        ) {
            Text("Tomar una foto")
        }

        if (fotoPerfil != null) {
            OutlinedButton(onClick = { onFotoPerfilChange(null) }) {
                Text("Quitar foto")
            }
        }
    }
}