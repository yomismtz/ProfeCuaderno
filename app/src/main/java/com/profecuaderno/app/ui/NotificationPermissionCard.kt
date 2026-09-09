package com.profecuaderno.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.notifications.NotificationHelper
import com.profecuaderno.app.notifications.ReminderScheduler

@Composable
fun NotificationPermissionCard() {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(NotificationHelper.canNotify(context)) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        ExternalActivityGuard.active = false
        allowed = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (allowed) {
            runCatching { ReminderScheduler.ensureDaily(context) }
                .onSuccess { permissionMessage = "Recordatorios activados correctamente." }
                .onFailure { permissionMessage = "El permiso se concedió, pero no pude programar los recordatorios. Intenta de nuevo." }
        } else {
            permissionMessage = "No se concedió el permiso de notificaciones. Puedes activarlo más adelante."
        }
    }

    fun requestNotificationPermission() {
        permissionMessage = null
        ExternalActivityGuard.active = true
        runCatching {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }.onFailure {
            ExternalActivityGuard.active = false
            permissionMessage = "No se pudo abrir la solicitud de permiso de notificaciones."
        }
    }

    DisposableEffect(Unit) {
        onDispose { ExternalActivityGuard.active = false }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (allowed) "Notificaciones activadas" else "Activa los recordatorios", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (allowed) "Recibirás avisos de cumpleaños y actividades de la agenda."
                        else "Mi Agenda Docente puede avisarte de cumpleaños, exámenes, prácticas, entregas, visitas y otras fechas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(onClick = { requestNotificationPermission() }) {
                        Text("Permitir")
                    }
                }
            }
            permissionMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
