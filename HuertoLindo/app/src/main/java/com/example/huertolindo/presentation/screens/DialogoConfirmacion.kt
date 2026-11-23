package com.example.huertolindo.presentation.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DialogoConfirmacion(
    titulo: String = "Confirmar eliminación",
    mensaje: String,
    textoConfirmar: String = "Eliminar",
    textoCancelar: String = "Cancelar",
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(textoCancelar)
            }
        }
    )
}
