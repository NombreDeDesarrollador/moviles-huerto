package com.example.huertolindo.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.huertolindo.presentation.media.crearUriFotografia

@Composable
fun FotoScreen(
    nombreUsuario: String,
    fotoPerfil: Uri?,
    onFotoPerfilChange: (Uri?) -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    var uriPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }

    val selectorFotos = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) onFotoPerfilChange(uri)
    }

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

        HuertoImagen(
            modelo = fotoPerfil,
            contentDescription = "Foto de perfil de $nombreUsuario",
            modifier = Modifier.size(160.dp),
            altura = 160.dp,
            forma = CircleShape
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
                } catch (_: Exception) {
                    uriPendiente = null
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

        Button(
            onClick = onCerrarSesion,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Cerrar sesión")
        }
    }
}
