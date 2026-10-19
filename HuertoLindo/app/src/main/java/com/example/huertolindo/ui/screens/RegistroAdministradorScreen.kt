package com.example.huertolindo.ui.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.huertolindo.viewmodel.HuertoViewModel

@Composable
fun RegistroAdministradorScreen(
    viewModel: HuertoViewModel,
    onBack: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var intentoRegistro by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<String?>(null) }

    val correoNormalizado = correo.trim()
    val errorNombre = if (intentoRegistro && nombre.count { it.isLetter() } < 2) {
        "El nombre debe tener al menos 2 letras."
    } else null
    val errorApellido = if (intentoRegistro && apellido.count { it.isLetter() } < 2) {
        "El apellido debe tener al menos 2 letras."
    } else null
    val errorCorreo = when {
        !intentoRegistro -> null
        correoNormalizado.isEmpty() -> "El correo electrónico es obligatorio."
        !Patterns.EMAIL_ADDRESS.matcher(correoNormalizado).matches() ->
            "Ingresa un correo electrónico válido."
        else -> null
    }
    val errorPassword = if (intentoRegistro && password.isBlank()) {
        "La contraseña es obligatoria."
    } else null
    val errorConfirmarPassword = when {
        !intentoRegistro -> null
        confirmarPassword.isBlank() -> "Confirma tu contraseña."
        confirmarPassword != password -> "Las contraseñas no coinciden."
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Registrar administrador",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Esta cuenta tendrá acceso al panel de administración.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                errorServidor = null
            },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorNombre != null,
            supportingText = { errorNombre?.let { Text(it) } }
        )
        OutlinedTextField(
            value = apellido,
            onValueChange = {
                apellido = it
                errorServidor = null
            },
            label = { Text("Apellido") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorApellido != null,
            supportingText = { errorApellido?.let { Text(it) } }
        )
        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                errorServidor = null
            },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = errorCorreo != null || errorServidor != null,
            supportingText = {
                (errorCorreo ?: errorServidor)?.let { Text(it) }
            }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorPassword != null,
            supportingText = { errorPassword?.let { Text(it) } }
        )
        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorConfirmarPassword != null,
            supportingText = { errorConfirmarPassword?.let { Text(it) } }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                intentoRegistro = true
                errorServidor = null
                if (
                    nombre.count { it.isLetter() } >= 2 && apellido.count { it.isLetter() } >= 2 &&
                    Patterns.EMAIL_ADDRESS.matcher(correoNormalizado).matches() &&
                    password.isNotBlank() && confirmarPassword.isNotBlank() &&
                    confirmarPassword == password
                ) {
                    cargando = true
                    viewModel.registrarAdministrador(
                        nombre = nombre,
                        apellido = apellido,
                        correo = correoNormalizado,
                        password = password
                    ) { resultado ->
                        cargando = false
                        when (resultado) {
                            HuertoViewModel.ResultadoRegistroAdministrador.Exito ->
                                onRegistrationSuccess()
                            HuertoViewModel.ResultadoRegistroAdministrador.CorreoYaRegistrado ->
                                errorServidor = "Ya existe una cuenta con este correo."
                            HuertoViewModel.ResultadoRegistroAdministrador.NoAutorizado ->
                                errorServidor = "Solo un administrador puede registrar otra cuenta."
                            HuertoViewModel.ResultadoRegistroAdministrador.ErrorDeBaseDeDatos ->
                                errorServidor = "No se pudo crear la cuenta. Inténtalo nuevamente."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando
        ) {
            if (cargando) {
                CircularProgressIndicator()
            } else {
                Text("Registrar administrador")
            }
        }
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando
        ) {
            Text("Volver al panel")
        }
    }
}
