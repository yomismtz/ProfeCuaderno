package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.GradeHistoryEntry
import com.profecuaderno.app.data.GradeHistoryStore
import com.profecuaderno.app.data.TeacherDbHelper
import java.text.DateFormat
import java.util.Date

@Composable
fun GradeHistoryScreen(db: TeacherDbHelper, refresh: Int, onChanged: () -> Unit) {
    val entries = remember(refresh) { GradeHistoryStore.entries(db) }
    var restoring by remember { mutableStateOf<GradeHistoryEntry?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Historial de calificaciones", style = MaterialTheme.typography.titleLarge)
                Text("Registra automáticamente los cambios guardados en calificaciones directas, actividades, exámenes y rúbricas.")
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no hay cambios de calificación registrados.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.studentName.ifBlank { "Estudiante" }, style = MaterialTheme.typography.titleSmall)
                                Text(entry.categoryName.ifBlank { "Rubro" }, style = MaterialTheme.typography.bodySmall)
                                val before = entry.oldScore?.let { "%.1f".format(it) } ?: "Sin registro"
                                Text("$before → ${"%.1f".format(entry.newScore)}", color = MaterialTheme.colorScheme.primary)
                                Text(
                                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.changedAt)),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (entry.oldScore != null) {
                                IconButton(onClick = { restoring = entry }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restaurar calificación anterior")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    restoring?.let { entry ->
        AlertDialog(
            onDismissRequest = { restoring = null },
            title = { Text("Restaurar calificación") },
            text = { Text("¿Volver de ${"%.1f".format(entry.newScore)} a ${"%.1f".format(entry.oldScore)} para ${entry.studentName}?") },
            confirmButton = {
                TextButton(onClick = {
                    GradeHistoryStore.restorePrevious(db, entry)
                    restoring = null
                    onChanged()
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { restoring = null }) { Text("Cancelar") } }
        )
    }
}
