package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.data.TrashEntry
import com.profecuaderno.app.data.TrashStore
import java.text.DateFormat
import java.util.Date

@Composable
fun TrashScreen(db: TeacherDbHelper, refresh: Int, onChanged: () -> Unit) {
    val entries = remember(refresh) { TrashStore.entries(db) }
    var deleting by remember { mutableStateOf<TrashEntry?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Papelera", style = MaterialTheme.typography.titleLarge)
                Text("Los elementos enviados aquí conservan sus datos. Puedes restaurarlos o eliminarlos definitivamente.")
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("La papelera está vacía.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { "${it.type}-${it.entityId}" }) { entry ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.label.ifBlank { TrashStore.typeLabel(entry.type) }, style = MaterialTheme.typography.titleSmall)
                                Text(TrashStore.typeLabel(entry.type), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Eliminado: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.deletedAt))}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            IconButton(onClick = {
                                TrashStore.restore(db, entry)
                                onChanged()
                            }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restaurar")
                            }
                            IconButton(onClick = { deleting = entry }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Eliminar definitivamente", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar definitivamente") },
            text = { Text("¿Eliminar '${entry.label}' de forma permanente? Esta acción ya no se podrá deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    TrashStore.deletePermanently(db, entry)
                    deleting = null
                    onChanged()
                }) { Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}
