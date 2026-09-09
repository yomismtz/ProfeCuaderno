package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate

@Composable
fun PeriodsScreen(
    db: TeacherDbHelper,
    refresh: Int,
    onChanged: () -> Unit,
    onOpen: (AcademicPeriod) -> Unit
) {
    val periods = remember(refresh) { db.getPeriods() }
    val active = periods.firstOrNull { it.active }
    var showNew by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Mis grupos", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Cada grupo, materia o curso funciona como una carpeta principal. Dentro estarán estudiantes, asistencia, evaluación, rubros y rúbricas, guía/planeación y reportes.")
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (periods.isEmpty()) {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Text("Aún no tienes grupos.")
                                Text("Pulsa + para crear el primero.")
                            }
                        }
                    }
                }
                items(periods, key = { it.id }) { period ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (period.active) Icons.Default.FolderOpen else Icons.Default.Folder,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(period.name, style = MaterialTheme.typography.titleMedium)
                                Text("${period.type} · ${period.startDate.ifBlank { "Sin inicio" }} → ${period.endDate.ifBlank { "Sin término" }}", style = MaterialTheme.typography.bodySmall)
                                if (period.archived) Text("Archivado", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(onClick = { onOpen(period) }) { Text("Abrir") }
                        }
                    }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
        FloatingActionButton(onClick = { showNew = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Default.Add, "Nuevo grupo")
        }
    }

    if (showNew) NewProgramDialog(
        periods = periods,
        active = active,
        onDismiss = { showNew = false }
    ) { name, type, start, end, copyFrom ->
        db.createPeriod(name, type, start, end, copyFrom, true)
        showNew = false
        onChanged()
        db.getActivePeriod()?.let(onOpen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProgramDialog(
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
    var copyFromId by remember { mutableStateOf(active?.id) }
    var typeExpanded by remember { mutableStateOf(false) }
    var copyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Nombre del grupo / materia / curso") },
                    placeholder = { Text("Ej. Matemáticas 2B / Biología / Taller de diseño") },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        type,
                        {},
                        readOnly = true,
                        label = { Text("Tipo de periodo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("Bimestre", "Trimestre", "Cuatrimestre", "Semestre", "Anual", "Curso corto", "Otro").forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { type = option; typeExpanded = false })
                        }
                    }
                }
                DatePickerField(start, { start = it }, "Fecha de inicio")
                DatePickerField(end, { end = it }, "Fecha de término")
                if (periods.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(copyStructure, { copyStructure = it })
                        Text("Usar plantilla de otro grupo")
                    }
                    if (copyStructure) {
                        ExposedDropdownMenuBox(expanded = copyExpanded, onExpandedChange = { copyExpanded = !copyExpanded }) {
                            val source = periods.firstOrNull { it.id == copyFromId }
                            OutlinedTextField(
                                value = source?.name ?: "Selecciona un grupo",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Copiar rubros, rúbricas y actividades de") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(copyExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = copyExpanded, onDismissRequest = { copyExpanded = false }) {
                                periods.forEach { sourcePeriod ->
                                    DropdownMenuItem(
                                        text = { Text(sourcePeriod.name) },
                                        onClick = {
                                            copyFromId = sourcePeriod.id
                                            copyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text("Solo se copiará la estructura de evaluación. No se copian alumnos, asistencias ni calificaciones.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, type, start, end, if (copyStructure) copyFromId else null) }
            ) { Text("Crear grupo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
