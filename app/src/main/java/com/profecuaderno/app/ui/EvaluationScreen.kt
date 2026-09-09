package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    val students = remember(refresh, period.id) { db.getStudents(period.id) }
    val categories = remember(refresh, period.id) { db.getCategories(period.id) }
    val totalWeight = categories.sumOf { it.weight }
    var selectedId by remember(categories.map { it.id }) { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    val selected = categories.firstOrNull { it.id == selectedId }
    var expanded by remember { mutableStateOf(false) }
    var rubricStudent by remember { mutableStateOf<Student?>(null) }
    var averageStudent by remember { mutableStateOf<Student?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (abs(totalWeight - 100.0) > 0.001) {
            AssistChip(
                onClick = {},
                label = { Text("Los rubros suman ${"%.1f".format(totalWeight)}%. Deben sumar 100%.") }
            )
        }

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Primero crea los rubros de evaluación.")
            }
            return
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected?.let { "${it.name} · ${"%.1f".format(it.weight)}%" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Rubro a calificar") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text("${cat.name} · ${"%.1f".format(cat.weight)}%") },
                        onClick = { selectedId = cat.id; expanded = false }
                    )
                }
            }
        }

        selected?.let { category ->
            val mode = runCatching { EvaluationMode.valueOf(category.mode) }.getOrDefault(EvaluationMode.DIRECT)
            val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
            val assessmentItems = remember(refresh, category.id) { db.getAssessmentItems(category.id) }

            Text(
                when (mode) {
                    EvaluationMode.AVERAGE -> if (assessmentItems.isEmpty()) "Primero agrega exámenes, prácticas, tareas o actividades dentro de este rubro." else "Cada alumno tendrá el promedio de las actividades registradas."
                    EvaluationMode.RUBRIC -> if (criteria.isEmpty()) "Primero configura la rúbrica interna." else "Evalúa criterio por criterio; la rúbrica interna suma 100%."
                    EvaluationMode.ATTENDANCE -> "Este rubro se calcula automáticamente a partir de la asistencia."
                    EvaluationMode.DIRECT -> "Captura una calificación directa de 0 a 100."
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (students.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Agrega alumnos al grupo para comenzar a evaluar.")
                }
                return@let
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    val categoryScore = remember(refresh, student.id, category.id, mode) { db.categoryScore(period.id, student.id, category) }
                    val finalScore = remember(refresh, student.id) { db.finalPercentage(period.id, student.id) }

                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(student.name, style = MaterialTheme.typography.titleSmall)
                                Text("${category.name}: ${"%.1f".format(categoryScore)}/100", style = MaterialTheme.typography.bodySmall)
                                Text("Calificación final acumulada: ${"%.1f".format(finalScore)}%", style = MaterialTheme.typography.bodySmall)

                                if (mode == EvaluationMode.AVERAGE && assessmentItems.isNotEmpty()) {
                                    val scores = assessmentItems.map { db.getAssessmentScore(student.id, it.id) }
                                    val registered = scores.count { it != null }
                                    val missed = scores.count { it != null && it == 0.0 }
                                    Text(
                                        "Registradas: $registered/${assessmentItems.size} · Perdidas/no entregadas: $missed",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            when (mode) {
                                EvaluationMode.DIRECT -> InlineGradeEditor(categoryScore) { value ->
                                    db.setGrade(period.id, student.id, category.id, value)
                                    onChanged()
                                }
                                EvaluationMode.RUBRIC -> Button(
                                    enabled = criteria.isNotEmpty() && abs(criteria.sumOf { it.weight } - 100.0) < 0.001,
                                    onClick = { rubricStudent = student }
                                ) {
                                    Icon(Icons.Default.EditNote, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rúbrica")
                                }
                                EvaluationMode.AVERAGE -> Button(
                                    enabled = assessmentItems.isNotEmpty(),
                                    onClick = { averageStudent = student }
                                ) {
                                    Icon(Icons.Default.EditNote, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Calificar")
                                }
                                EvaluationMode.ATTENDANCE -> AssistChip(
                                    onClick = {},
                                    label = { Text("Automático") }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    val category = selected
    val rubricTarget = rubricStudent
    if (category != null && rubricTarget != null) {
        RubricGradingDialog(
            db = db,
            period = period,
            category = category,
            student = rubricTarget,
            refresh = refresh,
            onDismiss = { rubricStudent = null },
            onSaved = {
                rubricStudent = null
                onChanged()
            }
        )
    }

    val averageTarget = averageStudent
    if (category != null && averageTarget != null) {
        ActivityGradingDialog(
            db = db,
            period = period,
            category = category,
            student = averageTarget,
            refresh = refresh,
            onDismiss = { averageStudent = null },
            onSaved = {
                averageStudent = null
                onChanged()
            }
        )
    }
}

@Composable
private fun InlineGradeEditor(initial: Double, onSave: (Double) -> Unit) {
    var text by remember(initial) { mutableStateOf(if (initial == 0.0) "" else "%.1f".format(initial)) }
    val value = text.replace(',', '.').toDoubleOrNull()
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("0-100") },
            modifier = Modifier.width(110.dp),
            singleLine = true
        )
        Spacer(Modifier.width(6.dp))
        FilledTonalButton(enabled = value != null && value in 0.0..100.0, onClick = { onSave(value!!) }) {
            Text("✓")
        }
    }
}

@Composable
private fun ActivityGradingDialog(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    category: EvaluationCategory,
    student: Student,
    refresh: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val items = remember(refresh, category.id) { db.getAssessmentItems(category.id) }
    val initial = remember(items, student.id, refresh) {
        items.associate { item ->
            item.id to (db.getAssessmentScore(student.id, item.id)?.let { "%.1f".format(it) } ?: "")
        }
    }
    var values by remember(items, student.id, refresh) { mutableStateOf(initial) }

    val numericScores = values.values.mapNotNull { it.replace(',', '.').toDoubleOrNull() }
    val average = if (numericScores.isEmpty()) 0.0 else numericScores.average()
    val missed = numericScores.count { it == 0.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.name} · ${student.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Text("Escribe 0 si la actividad se perdió o no se entregó. Deja vacío si todavía no se ha evaluado.")
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { item ->
                        OutlinedTextField(
                            value = values[item.id] ?: "",
                            onValueChange = { raw ->
                                values = values.toMutableMap().also { it[item.id] = raw }
                            },
                            label = { Text(item.name) },
                            placeholder = { Text("0-100 o vacío") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Promedio actual: ${"%.1f".format(average)}/100", style = MaterialTheme.typography.titleMedium)
                Text("Actividades registradas: ${numericScores.size}/${items.size} · Perdidas/no entregadas: $missed")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                items.forEach { item ->
                    val text = values[item.id].orEmpty().trim()
                    val score = text.replace(',', '.').toDoubleOrNull()
                    if (text.isBlank()) db.setAssessmentScore(student.id, item.id, null)
                    else if (score != null && score in 0.0..100.0) db.setAssessmentScore(student.id, item.id, score)
                }
                db.calculateAndStoreAverageGrade(period.id, student.id, category.id)
                onSaved()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RubricGradingDialog(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    category: EvaluationCategory,
    student: Student,
    refresh: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
    val marks = remember(criteria, student.id, refresh) {
        criteria.associate { it.id to db.getRubricMark(student.id, it.id) }.toMutableMap()
    }
    var localMarks by remember(criteria, student.id, refresh) { mutableStateOf(marks) }
    val calculated = criteria.sumOf { (localMarks[it.id] ?: 0.0) * it.weight / 100.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.name} · ${student.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Text("Cada criterio se califica de 0 a 100. Los porcentajes internos calculan el resultado del rubro.")
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(criteria, key = { it.id }) { criterion ->
                        var text by remember(criterion.id, localMarks[criterion.id]) {
                            mutableStateOf(if ((localMarks[criterion.id] ?: 0.0) == 0.0) "" else "%.1f".format(localMarks[criterion.id]))
                        }
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${criterion.name} · ${"%.1f".format(criterion.weight)}%")
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { raw ->
                                        text = raw
                                        val parsed = raw.replace(',', '.').toDoubleOrNull()
                                        if (parsed != null && parsed in 0.0..100.0) {
                                            localMarks = localMarks.toMutableMap().also { it[criterion.id] = parsed }
                                        }
                                    },
                                    label = { Text("Calificación 0-100") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Resultado: ${"%.1f".format(calculated)}/100", style = MaterialTheme.typography.titleMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                criteria.forEach { criterion ->
                    db.setRubricMark(student.id, criterion.id, localMarks[criterion.id] ?: 0.0)
                }
                db.calculateAndStoreRubricGrade(period.id, student.id, category.id)
                onSaved()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
