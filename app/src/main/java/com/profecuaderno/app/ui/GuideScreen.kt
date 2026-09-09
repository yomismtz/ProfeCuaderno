package com.profecuaderno.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod

@Composable
fun GuideScreen(period: AcademicPeriod) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("program_documents", 0) }
    val key = "guide_uri_${period.id}"
    var uriString by remember { mutableStateOf(prefs.getString(key, null)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            prefs.edit().putString(key, uri.toString()).apply()
            uriString = uri.toString()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Guía modular / planeación", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text("Guarda aquí el PDF principal del programa para tenerlo siempre accesible desde esta carpeta.")
            }
        }

        if (uriString == null) {
            Button(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                Icon(Icons.Default.UploadFile, null)
                Spacer(Modifier.width(8.dp))
                Text("Seleccionar PDF")
            }
        } else {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Documento guardado ✓", style = MaterialTheme.typography.titleMedium)
                    Text("La app conserva el acceso al archivo elegido en este dispositivo.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val uri = Uri.parse(uriString)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(intent) }
                        }) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Visualizar")
                        }
                        OutlinedButton(onClick = { launcher.launch(arrayOf("application/pdf")) }) {
                            Text("Cambiar PDF")
                        }
                    }
                }
            }
        }
    }
}
