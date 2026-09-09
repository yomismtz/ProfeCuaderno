package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.CalendarEvent
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate

@Composable
fun CalendarScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    val customEvents = remember(refresh, period.id) { db.getEvents(period.id) }
    val birthdays = remember(refresh, period.id) { db.birthdaysForPeriod(period.id) }
    val events = remember(customEvents, birthdays) { (customEvents + birthdays).sortedBy { it.date } }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CalendarEvent?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Agenda del periodo", style = MaterialTheme.typography.titleMedium)
                    Text("Los cumpleaños se agregan automáticamente desde la fecha de nacimiento de cada alumno.")
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (events.isEmpty()) item { Text("No hay fechas registradas.") }
                items(events, key = { "${it.type}-${it.id}-${it.date}" }) { event ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(event.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(event.title, style = MaterialTheme.typography.titleSmall)
                                if (event.notes.isNotBlank()) Text(event.notes, style = MaterialTheme.typography.bodySmall)
                            }
                            if (event.id > 0) {
                                IconButton(onClick = { deleting = event }) { Icon(Icons.Default.Delete, "Eliminar") }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
        FloatingActionButton(onClick = { showNew = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Agregar fecha")
        }
    }

    if (showNew) EventDialog(period.id, onDismiss = { showNew = false }) {
        db.saveEvent(it)
        showNew = false
        onChanged()
    }

    deleting?.let { event ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar fecha") },
            text = { Text("¿Eliminar '${event.title}'?") },
            confirmButton = { TextButton(onClick = { db.deleteEvent(event.id); deleting = null; onChanged() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EventDialog(periodId: Long, onDismiss: () -> Unit, onSave: (CalendarEvent) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("IMPORTANTE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva fecha importante") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                DatePickerField(date, { date = it }, "Fecha")
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("IMPORTANTE", "EXAMEN", "ENTREGA", "CLASE").forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option) })
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")), onClick = { onSave(CalendarEvent(0, periodId, title, date, notes, type)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
