package com.example.huertolindo.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun FotoScreen(
    modifier: Modifier = Modifier
) {
    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    // LAUNCHER DE PHOTOPICKER
    val selectorFotos = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) imagenUri = uri
    }
    //Definimos un color cambiante
    val colorBoton by animateColorAsState(
        targetValue =
            if (imagenUri != null) {
                Color(0xFFFF0000)
            } else {
                Color(0xFF00DC0F)
            },
        animationSpec = tween(2000)
    )
    // CAMARA!!!
    val context = LocalContext.current
    var uriPendiente by rememberSaveable() { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = TakePicture()
    ) { fotoTomada ->
        if (fotoTomada) imagenUri = uriPendiente
        uriPendiente = null
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "FotoMotion",
            style = MaterialTheme.typography.headlineMedium
        )
        Crossfade(
            targetState = imagenUri != null
        ) { tieneFoto ->
            Text(
                text = if (tieneFoto) "FotoSeleccionada"
                else "Selecciona una foto"
            )
        }
        // BOTON PARA ALMACENAMIENTO
        Button(
            onClick = {
                selectorFotos.launch(
                    PickVisualMediaRequest(
                        PickVisualMedia.ImageOnly
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                colorBoton
            )
        ) {
            Text("Seleccionar fotografía")
        }

        // BOTON PARA CAMARA
        Button(
            onClick = {
                try {
                    val nuevaUri = crearUriFotografia(context)
                    uriPendiente = nuevaUri
                    cameraLauncher.launch(nuevaUri)
                } catch (e: Exception) {
                    uriPendiente = null
                    e.printStackTrace()
                }
            }
        ) {
            Text("Tomar fotografía")
        }


        // IMAGEN CARGADA
        AnimatedVisibility(
            visible = imagenUri != null,
            enter = fadeIn(
                animationSpec = tween(3000)
            ) + expandVertically(
                animationSpec = tween(3000)
            ),
            exit = fadeOut() + shrinkOut()
        )
        {
            AsyncImage(
                model = imagenUri,
                contentDescription = "Aqui hay una foto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        }
        AnimatedVisibility(
            visible = imagenUri != null,
            enter = fadeIn(
                animationSpec = tween(2000)
            ) + expandVertically(tween(2000))
        )
        {
            OutlinedButton(
                onClick = {
                    imagenUri = null
                }
            ) {
                Text("Quitar foto")
            }
        }


    }
}