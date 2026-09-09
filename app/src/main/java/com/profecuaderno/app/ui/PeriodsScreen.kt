package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate

@Composable
fun PeriodsScreen(db: TeacherDbHelper, refresh: Int, onChanged: () -> Unit) {
    val periods = remember(refresh) { db.getPeriods() }
    val active = periods.firstOrNull { it.active }
    var showNew by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Cambiar de trimestre o semestre", style = MaterialTheme.typography.titleMedium)
                    Text("Al crear un nuevo periodo puedes iniciar vacío o copiar solo tus rubros y rúbricas. Los alumnos y asistencias anteriores quedan archivados.")
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(periods, key = { it.id }) { period ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(period.name, style = MaterialTheme.typography.titleMedium)
                                    if (period.active) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Text("${period.type} · ${period.startDate} → ${period.endDate}", style = MaterialTheme.typography.bodySmall)
                                if (period.archived) Text("Archivado", style = MaterialTheme.typography.labelSmall)
                            }
                            if (!period.active) {
                                TextButton(onClick = { db.activatePeriod(period.id); onChanged() }) { Text("Activar") }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
        FloatingActionButton(onClick = { showNew = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Nuevo periodo")
        }
    }

    if (showNew) NewPeriodDialog(periods = periods, active = active, onDismiss = { showNew = false }) { name, type, start, end, copyFrom ->
        active?.let { db.closePeriod(it.id) }
        db.createPeriod(name, type, start, end, copyFrom, true)
        showNew = false
        onChanged()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewPeriodDialog(
    periods: List<AcademicPeriod>,
    active: AcademicPeriod?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Trimestre") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf("") }
    var copyStructure by remember { mutableStateOf(active != null) }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo periodo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, placeholder = { Text("Ej. 26-O / 2027-1") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(type, {}, readOnly = true, label = { Text("Tipo") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("Trimestre", "Cuatrimestre", "Semestre", "Bimestre", "Otro").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                        }
                    }
                }
                OutlinedTextField(start, { start = it }, label = { Text("Inicio") }, placeholder = { Text("AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(end, { end = it }, label = { Text("Fin") }, placeholder = { Text("AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
                if (periods.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(copyStructure, { copyStructure = it })
                        Text("Copiar rubros y rúbricas del periodo actual")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, type, start, end, if (copyStructure) active?.id else null) }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
