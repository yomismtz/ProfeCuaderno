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
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.EvaluationCategory
import com.profecuaderno.app.data.Student
import com.profecuaderno.app.data.TeacherDbHelper
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

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (abs(totalWeight - 100.0) > 0.001) {
            AssistChip(
                onClick = {},
                label = { Text("Los rubros suman ${"%.1f".format(totalWeight)}%. Deben sumar 100% para una calificación final completa.") }
            )
        }

        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Primero crea tus rubros de evaluación en 'Rubros y rúbricas'.")
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
            val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
            Text(
                if (criteria.isEmpty()) "Captura una calificación de 0 a 100." else "Este rubro tiene rúbrica. Evalúa criterio por criterio.",
                style = MaterialTheme.typography.bodySmall
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    val grade = remember(refresh, student.id, category.id) { db.getGrade(student.id, category.id) }
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(student.name, style = MaterialTheme.typography.titleSmall)
                                Text("Rubro: ${"%.1f".format(grade)}/100 · Final acumulado: ${"%.1f".format(db.finalPercentage(period.id, student.id))}%", style = MaterialTheme.typography.bodySmall)
                            }
                            if (criteria.isEmpty()) {
                                InlineGradeEditor(grade) { value ->
                                    db.setGrade(period.id, student.id, category.id, value)
                                    onChanged()
                                }
                            } else {
                                Button(onClick = { rubricStudent = student }) {
                                    Icon(Icons.Default.EditNote, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rúbrica")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }

    val category = selected
    val student = rubricStudent
    if (category != null && student != null) {
        RubricGradingDialog(
            db = db,
            period = period,
            category = category,
            student = student,
            refresh = refresh,
            onDismiss = { rubricStudent = null },
            onSaved = {
                rubricStudent = null
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
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                Text("Cada criterio se califica de 0 a 100. Su peso interno calcula el resultado del rubro.")
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(criteria, key = { it.id }) { criterion ->
                        var text by remember(criterion.id, localMarks[criterion.id]) { mutableStateOf(if ((localMarks[criterion.id] ?: 0.0) == 0.0) "" else "%.1f".format(localMarks[criterion.id])) }
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
                Text("Resultado calculado: ${"%.1f".format(calculated)}/100", style = MaterialTheme.typography.titleMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                criteria.forEach { criterion -> db.setRubricMark(student.id, criterion.id, localMarks[criterion.id] ?: 0.0) }
                db.calculateAndStoreRubricGrade(period.id, student.id, category.id)
                onSaved()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
