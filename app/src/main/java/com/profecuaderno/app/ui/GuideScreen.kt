package com.profecuaderno.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.CalendarEvent
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.util.PlanningGuideAnalyzer
import com.profecuaderno.app.util.PlanningSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GuideScreen(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    refresh: Int,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("program_documents", 0) }
    val key = "guide_uri_${period.id}"
    var uriString by remember { mutableStateOf(prefs.getString(key, null)) }
    val events = remember(refresh, period.id) { db.getEvents(period.id) }
    var eventTypeToCreate by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<CalendarEvent?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<PlanningSuggestion>>(emptyList()) }
    var selectedSuggestions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var analysisMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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

    LazyColumn(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Guía / planeación", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Aquí puedes conservar la guía, programa o planeación del curso y convertir sus fechas en actividades del calendario.")
                }
            }
        }

        item {
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
                        Button(
                            enabled = !analyzing,
                            onClick = {
                                val currentUri = uriString ?: return@Button
                                analyzing = true
                                analysisMessage = null
                                scope.launch {
                                    val found = withContext(Dispatchers.IO) {
                                        runCatching {
                                            PlanningGuideAnalyzer.analyze(context, Uri.parse(currentUri), period)
                                        }.getOrElse { emptyList() }
                                    }
                                    suggestions = found
                                    selectedSuggestions = found.indices.toSet()
                                    analyzing = false
                                    analysisMessage = if (found.isEmpty()) {
                                        "No encontré fechas legibles automáticamente. Si el PDF es una imagen escaneada, esta versión no puede extraer su texto."
                                    } else "Encontré ${found.size} posibles fechas. Revisa cuáles quieres agregar."
                                }
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (analyzing) "Analizando…" else "Analizar guía")
                        }
                        analysisMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fechas de la planeación", style = MaterialTheme.typography.titleMedium)
                    Text("Selecciona el tipo de actividad y agrega su fecha. Al guardarla aparecerá automáticamente en el calendario principal.")
                    teacherEventTypes.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { option ->
                                OutlinedButton(
                                    onClick = { eventTypeToCreate = option.code },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(option.label)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (events.isNotEmpty()) {
            item {
                Text("Fechas registradas para ${period.name}", style = MaterialTheme.typography.titleMedium)
            }
            items(events, key = { it.id }) { event ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(event.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(event.title, style = MaterialTheme.typography.titleSmall)
                            Text(eventTypeLabel(event.type), style = MaterialTheme.typography.bodySmall)
                            if (event.notes.isNotBlank()) Text(event.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { deleting = event }) {
                            Icon(Icons.Default.Delete, "Eliminar")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(30.dp)) }
    }

    eventTypeToCreate?.let { selectedType ->
        EventDialog(
            groups = listOf(period),
            initialGroupId = period.id,
            initialType = selectedType,
            onDismiss = { eventTypeToCreate = null },
            onSave = {
                db.saveEvent(it)
                eventTypeToCreate = null
                onChanged()
            }
        )
    }

    if (suggestions.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                suggestions = emptyList()
                selectedSuggestions = emptySet()
            },
            title = { Text("Fechas detectadas") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                    Text("Confirma antes de agregar. La app no guarda ninguna fecha automáticamente.")
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suggestions.indices.toList()) { index ->
                            val suggestion = suggestions[index]
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                    Checkbox(
                                        checked = index in selectedSuggestions,
                                        onCheckedChange = { checked ->
                                            selectedSuggestions = if (checked) selectedSuggestions + index else selectedSuggestions - index
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text("${suggestion.date} · ${eventTypeLabel(suggestion.type)}", style = MaterialTheme.typography.labelLarge)
                                        Text(suggestion.title, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedSuggestions.isNotEmpty(),
                    onClick = {
                        selectedSuggestions.sorted().forEach { index ->
                            val suggestion = suggestions[index]
                            db.saveEvent(
                                CalendarEvent(
                                    id = 0,
                                    periodId = period.id,
                                    title = suggestion.title,
                                    date = suggestion.date,
                                    notes = "Detectado desde la guía / planeación",
                                    type = suggestion.type
                                )
                            )
                        }
                        analysisMessage = "Se agregaron ${selectedSuggestions.size} fechas al calendario."
                        suggestions = emptyList()
                        selectedSuggestions = emptySet()
                        onChanged()
                    }
                ) { Text("Agregar seleccionadas") }
            },
            dismissButton = {
                TextButton(onClick = {
                    suggestions = emptyList()
                    selectedSuggestions = emptySet()
                }) { Text("Cancelar") }
            }
        )
    }

    deleting?.let { event ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar fecha") },
            text = { Text("¿Eliminar '${event.title}' de la planeación?") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteEvent(event.id)
                    deleting = null
                    onChanged()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}
