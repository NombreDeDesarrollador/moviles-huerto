package com.example.huertolindo.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

fun crearUriFotografia(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos")
    if (!carpeta.exists() && !carpeta.mkdirs()) {
        throw IOException("No se pudo preparar el archivo temporal de la fotografía.")
    }
    val archivo = File.createTempFile(
        "foto_", ".jpg", carpeta
    )
    return try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    } catch (exception: IllegalArgumentException) {
        archivo.delete()
        throw exception
    }
}

fun eliminarFotografiaTemporal(context: Context, uri: Uri) {
    if (uri.authority != "${context.packageName}.fileprovider") return
    val nombreArchivo = uri.lastPathSegment ?: return
    if (File(nombreArchivo).name != nombreArchivo) return
    File(File(context.cacheDir, "fotos"), nombreArchivo).delete()
}