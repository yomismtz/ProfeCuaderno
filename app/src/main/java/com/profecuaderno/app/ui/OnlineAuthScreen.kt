package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.network.CentralBackend
import com.profecuaderno.app.network.LoginRequest
import com.profecuaderno.app.network.RegisterRequest
import kotlinx.coroutines.launch

@Composable
fun OnlineAuthScreen(
    backend: CentralBackend,
    onAuthenticated: () -> Unit
) {
    var registerMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(Modifier.fillMaxWidth().widthIn(max = 460.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("La Carpeta del Docente", style = MaterialTheme.typography.headlineSmall)
                Text(if (registerMode) "Crear cuenta docente" else "Iniciar sesión")

                if (!backend.isConfigured) {
                    Text(
                        "El servidor central todavía no está configurado en esta compilación.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (registerMode) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                Button(
                    onClick = {
                        if (email.isBlank() || password.length < 6 || (registerMode && fullName.isBlank())) {
                            error = "Completa los datos. La contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }
                        scope.launch {
                            loading = true
                            error = null
                            runCatching {
                                if (registerMode) {
                                    backend.api.register(RegisterRequest(email.trim(), password, fullName.trim()))
                                } else {
                                    backend.api.login(LoginRequest(email.trim(), password))
                                }
                            }.onSuccess {
                                backend.saveSession(it)
                                onAuthenticated()
                            }.onFailure {
                                error = "No fue posible conectar o validar la cuenta."
                            }
                            loading = false
                        }
                    },
                    enabled = !loading && backend.isConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Conectando…" else if (registerMode) "Crear cuenta" else "Entrar")
                }

                TextButton(onClick = { registerMode = !registerMode; error = null }) {
                    Text(if (registerMode) "Ya tengo cuenta" else "Crear una cuenta docente")
                }
            }
        }
    }
}
