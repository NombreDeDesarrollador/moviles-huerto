package com.example.huertolindo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.huertolindo.presentation.validation.ValidacionRegistro
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel

@Composable
fun RegistroScreen(
    viewModel: HuertoViewModel,
    onBackToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    collectHuertoUiState(viewModel)
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var intentoRegistro by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<String?>(null) }

    val validacion = ValidacionRegistro.validar(
        nombre = nombre,
        apellido = apellido,
        correo = correo,
        password = password,
        confirmarPassword = confirmarPassword,
        mostrarErrores = intentoRegistro
    )

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
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
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
            isError = validacion.errorNombre != null,
            supportingText = { validacion.errorNombre?.let { Text(it) } }
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
            isError = validacion.errorApellido != null,
            supportingText = { validacion.errorApellido?.let { Text(it) } }
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
            isError = validacion.errorCorreo != null || errorServidor != null,
            supportingText = {
                (validacion.errorCorreo ?: errorServidor)?.let { Text(it) }
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
            isError = validacion.errorPassword != null,
            supportingText = { validacion.errorPassword?.let { Text(it) } }
        )
        OutlinedTextField(
            value = confirmarPassword,
            onValueChange = { confirmarPassword = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = validacion.errorConfirmarPassword != null,
            supportingText = { validacion.errorConfirmarPassword?.let { Text(it) } }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                intentoRegistro = true
                errorServidor = null
                if (validacion.esValido) {
                    cargando = true
                    viewModel.registrarUsuario(
                        nombre = nombre,
                        apellido = apellido,
                        correo = validacion.correoNormalizado,
                        password = password
                    ) { resultado ->
                        cargando = false
                        when (resultado) {
                            HuertoViewModel.ResultadoRegistro.Exito -> onRegistrationSuccess()
                            HuertoViewModel.ResultadoRegistro.CorreoYaRegistrado ->
                                errorServidor = "Ya existe una cuenta con este correo."
                            HuertoViewModel.ResultadoRegistro.ErrorDeBaseDeDatos ->
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
                Text("Registrarme")
            }
        }
        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !cargando
        ) {
            Text("Volver al inicio de sesión")
        }
    }
}
