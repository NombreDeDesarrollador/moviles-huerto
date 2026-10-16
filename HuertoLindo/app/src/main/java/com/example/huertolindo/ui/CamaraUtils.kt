package com.example.huertolindo.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun crearUriFotografia(context: Context): Uri {
    val carpeta = File(
        context.cacheDir, "fotos"
    )
    carpeta.mkdirs()
    val archivo = File.createTempFile(
        "foto_", ".jpg", carpeta
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        archivo
    )
}