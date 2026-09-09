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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (allowed) ReminderScheduler.ensureDaily(context)
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (allowed) "Notificaciones activadas" else "Activa los recordatorios", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (allowed) "Recibirás avisos de cumpleaños y actividades de la agenda."
                    else "ProfeCuaderno puede avisarte de cumpleaños, exámenes, prácticas, entregas, visitas y otras fechas.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Permitir")
                }
            }
        }
    }
}
