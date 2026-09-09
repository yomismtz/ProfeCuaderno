package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.*
import kotlin.math.abs

private data class RubricPreset(
    val name: String,
    val criteria: List<Pair<String, Double>>
)

private data class ActivityPreset(
    val name: String,
    val items: List<String>
)

private val rubricPresets = listOf(
    RubricPreset(
        "Investigación",
        listOf(
            "Título y planteamiento bien definidos" to 10.0,
            "Objetivos claros" to 10.0,
            "Marco teórico / fundamentación" to 15.0,
            "Metodología" to 15.0,
            "Resultados" to 15.0,
            "Discusión" to 15.0,
            "Conclusiones" to 10.0,
            "Ortografía, redacción y referencias" to 10.0
        )
    ),
    RubricPreset(
        "Exposiciones",
        listOf(
            "Conocimiento y dominio del tema" to 25.0,
            "Organización de la exposición" to 15.0,
            "Claridad al explicar" to 15.0,
            "Manejo del material visual" to 10.0,
            "Expresión oral" to 10.0,
            "Uso del tiempo" to 10.0,
            "Respuesta a preguntas" to 15.0
        )
    ),
    RubricPreset(
        "Maquetas",
        listOf(
            "Exactitud del contenido" to 20.0,
            "Diseño y organización" to 15.0,
            "Calidad de elaboración" to 20.0,
            "Funcionalidad / pertinencia" to 15.0,
            "Presentación" to 10.0,
            "Explicación del trabajo" to 10.0,
            "Creatividad y cuidado" to 10.0
        )
    ),
    RubricPreset(
        "Laboratorios",
        listOf(
            "Preparación previa" to 15.0,
            "Cumplimiento del procedimiento" to 20.0,
            "Manejo de materiales e instrumental" to 15.0,
            "Orden, limpieza y seguridad" to 15.0,
            "Calidad de los resultados" to 20.0,
            "Registro / reporte de resultados" to 10.0,
            "Responsabilidad" to 5.0
        )
    ),
    RubricPreset(
        "Actividades prácticas",
        listOf(
            "Preparación previa" to 15.0,
            "Ejecución de la actividad" to 25.0,
            "Aplicación de conocimientos" to 20.0,
            "Calidad del resultado" to 20.0,
            "Organización y manejo del material" to 10.0,
            "Responsabilidad y participación" to 10.0
        )
    ),
    RubricPreset(
        "Teoría",
        listOf(
            "Conocimiento del tema" to 25.0,
            "Comprensión de conceptos" to 20.0,
            "Aplicación de conocimientos" to 20.0,
            "Argumentación / razonamiento" to 15.0,
            "Uso correcto de términos" to 10.0,
            "Organización y claridad" to 10.0
        )
    ),
    RubricPreset(
        "Trabajos",
        listOf(
            "Cumplimiento de indicaciones" to 20.0,
            "Contenido completo" to 25.0,
            "Organización y estructura" to 15.0,
            "Calidad de la información" to 15.0,
            "Presentación" to 10.0,
            "Ortografía y redacción" to 10.0,
            "Entrega en tiempo" to 5.0
        )
    )
)

private val activityPresets = listOf(
    ActivityPreset("Exámenes", listOf("Examen diagnóstico", "Examen 1", "Examen 2", "Examen 3", "Examen 4", "Examen final")),
    ActivityPreset("Prácticas", (1..6).map { "Práctica $it" }),
    ActivityPreset("Laboratorio", (1..6).map { "Laboratorio $it" }),
    ActivityPreset("Tareas", (1..10).map { "Tarea $it" })
)

@Composable
fun RubricsScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    val categories = remember(refresh, period.id) { db.getCategories(period.id) }
    val total = categories.sumOf { it.weight }
    var editingCategory by remember { mutableStateOf<EvaluationCategory?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    var rubricCategory by remember { mutableStateOf<EvaluationCategory?>(null) }
    var activityCategory by remember { mutableStateOf<EvaluationCategory?>(null) }
    var deletingCategory by remember { mutableStateOf<EvaluationCategory?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Rubros de la calificación final", style = MaterialTheme.typography.titleMedium)
                Text("Puedes combinar exámenes, investigación, exposiciones, maquetas, laboratorios, actividades prácticas, teoría, trabajos, tareas y asistencia hasta completar el 100%.")
                Spacer(Modifier.height(6.dp))
                val ok = abs(total - 100.0) < 0.001
                Text(
                    "Total actual: ${"%.1f".format(total)}%${if (ok) " ✓" else if (total < 100) " — faltan ${"%.1f".format(100-total)}%" else " — excede ${"%.1f".format(total-100)}%"}",
                    color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { showNewCategory = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Agregar rubro")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { category ->
                val mode = runCatching { EvaluationMode.valueOf(category.mode) }.getOrDefault(EvaluationMode.DIRECT)
                val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
                val activities = remember(refresh, category.id) { db.getAssessmentItems(category.id) }
                val rubricTotal = criteria.sumOf { it.weight }

                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text("Vale ${"%.1f".format(category.weight)}% de la calificación final")
                                Text("Forma de evaluar: ${mode.label}", style = MaterialTheme.typography.bodySmall)
                                when (mode) {
                                    EvaluationMode.RUBRIC -> Text(
                                        "Rúbrica: ${criteria.size} criterios · ${"%.1f".format(rubricTotal)}%${if (abs(rubricTotal - 100.0) < 0.001) " ✓" else " ⚠"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    EvaluationMode.AVERAGE -> Text("${activities.size} actividades para promediar", style = MaterialTheme.typography.bodySmall)
                                    EvaluationMode.ATTENDANCE -> Text("Se toma automáticamente del porcentaje de asistencia", style = MaterialTheme.typography.bodySmall)
                                    EvaluationMode.DIRECT -> Text("Se captura una sola calificación", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = { editingCategory = category }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { deletingCategory = category }) { Icon(Icons.Default.Delete, "Eliminar") }
                        }

                        Spacer(Modifier.height(8.dp))
                        when (mode) {
                            EvaluationMode.RUBRIC -> OutlinedButton(onClick = { rubricCategory = category }) {
                                Text(if (criteria.isEmpty()) "Crear rúbrica" else "Editar rúbrica")
                            }
                            EvaluationMode.AVERAGE -> OutlinedButton(onClick = { activityCategory = category }) {
                                Text(if (activities.isEmpty()) "Agregar actividades" else "Editar actividades")
                            }
                            else -> {}
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showNewCategory) CategoryDialog(
        title = "Nuevo rubro",
        initial = EvaluationCategory(0, period.id, "", 0.0, categories.size, EvaluationMode.AVERAGE.name),
        onDismiss = { showNewCategory = false },
        onSave = {
            db.saveCategory(it)
            showNewCategory = false
            onChanged()
        }
    )

    editingCategory?.let { cat ->
        CategoryDialog(
            title = "Editar rubro",
            initial = cat,
            onDismiss = { editingCategory = null },
            onSave = {
                db.saveCategory(it)
                editingCategory = null
                onChanged()
            }
        )
    }

    rubricCategory?.let { cat ->
        RubricDialog(db, cat, refresh, onDismiss = { rubricCategory = null }, onChanged = onChanged)
    }

    activityCategory?.let { cat ->
        ActivitiesDialog(db, cat, refresh, onDismiss = { activityCategory = null }, onChanged = onChanged)
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Eliminar rubro") },
            text = { Text("¿Eliminar '${cat.name}'? También se eliminarán sus actividades, rúbrica y calificaciones.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteCategory(cat.id)
                    deletingCategory = null
                    onChanged()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDialog(
    title: String,
    initial: EvaluationCategory,
    onDismiss: () -> Unit,
    onSave: (EvaluationCategory) -> Unit
) {
    val suggestions = listOf(
        "Exámenes",
        "Investigación",
        "Exposiciones",
        "Maquetas",
        "Laboratorios",
        "Actividades prácticas",
        "Teoría",
        "Trabajos",
        "Tareas",
        "Asistencias"
    )
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var weightText by remember(initial.id) { mutableStateOf(if (initial.weight == 0.0) "" else initial.weight.toString()) }
    var mode by remember(initial.id) { mutableStateOf(runCatching { EvaluationMode.valueOf(initial.mode) }.getOrDefault(EvaluationMode.AVERAGE)) }
    var modeExpanded by remember { mutableStateOf(false) }
    val weight = weightText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 600.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ideas de rubros", style = MaterialTheme.typography.labelLarge)
                suggestions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { suggestion ->
                            AssistChip(
                                onClick = {
                                    name = suggestion
                                    mode = when (suggestion) {
                                        "Exámenes", "Tareas" -> EvaluationMode.AVERAGE
                                        "Asistencias" -> EvaluationMode.ATTENDANCE
                                        "Investigación", "Exposiciones", "Maquetas",
                                        "Laboratorios", "Actividades prácticas", "Teoría", "Trabajos" -> EvaluationMode.RUBRIC
                                        else -> mode
                                    }
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }

                OutlinedTextField(name, { name = it }, label = { Text("Nombre del rubro") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weightText, { weightText = it }, label = { Text("Porcentaje dentro de la calificación final") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = !modeExpanded }) {
                    OutlinedTextField(
                        value = mode.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("¿Cómo se va a evaluar?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        EvaluationMode.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { mode = option; modeExpanded = false }
                            )
                        }
                    }
                }

                Text(
                    when (mode) {
                        EvaluationMode.AVERAGE -> "Ejemplo: Examen 1 + Examen 2 + Examen final; o Práctica 1 a 6. La app obtiene el promedio."
                        EvaluationMode.RUBRIC -> "Ejemplo: exposición o investigación escrita. Los criterios internos deben sumar 100%."
                        EvaluationMode.ATTENDANCE -> "La app utiliza automáticamente el porcentaje de asistencia del alumno."
                        EvaluationMode.DIRECT -> "Capturas directamente una sola calificación de 0 a 100."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && weight != null && weight in 0.0..100.0,
                onClick = { onSave(initial.copy(name = name, weight = weight!!, mode = mode.name)) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ActivitiesDialog(
    db: TeacherDbHelper,
    category: EvaluationCategory,
    refresh: Int,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val items = remember(refresh, category.id) { db.getAssessmentItems(category.id) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<AssessmentItem?>(null) }
    var preset by remember { mutableStateOf<ActivityPreset?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${category.name} · actividades") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Text("La calificación del rubro será el promedio de las actividades que ya tengan calificación registrada.")
                Text("Si un alumno perdió una tarea o práctica, registra 0. Si todavía no la evalúas, déjala en blanco.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                if (items.isEmpty()) {
                    Text("Plantillas rápidas", style = MaterialTheme.typography.titleSmall)
                    activityPresets.forEach { p ->
                        OutlinedButton(onClick = { preset = p }, modifier = Modifier.fillMaxWidth()) { Text(p.name) }
                        Spacer(Modifier.height(4.dp))
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                }

                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items, key = { it.id }) { item ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { deleting = item }) { Icon(Icons.Default.Delete, "Eliminar") }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = { showNew = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar actividad")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )

    preset?.let { p ->
        AlertDialog(
            onDismissRequest = { preset = null },
            title = { Text("Agregar plantilla ${p.name}") },
            text = { Text(p.items.joinToString("\n")) },
            confirmButton = {
                TextButton(onClick = {
                    p.items.forEachIndexed { index, name ->
                        db.saveAssessmentItem(AssessmentItem(0, category.id, name, index))
                    }
                    preset = null
                    onChanged()
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { preset = null }) { Text("Cancelar") } }
        )
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("Nueva actividad") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, placeholder = { Text("Ej. Examen 1 / Tarea 4 / Práctica 2") }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    db.saveAssessmentItem(AssessmentItem(0, category.id, name, items.size))
                    showNew = false
                    onChanged()
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancelar") } }
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar actividad") },
            text = { Text("¿Eliminar '${item.name}' y las calificaciones registradas en ella?") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteAssessmentItem(item.id)
                    deleting = null
                    onChanged()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun RubricDialog(
    db: TeacherDbHelper,
    category: EvaluationCategory,
    refresh: Int,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
    val total = criteria.sumOf { it.weight }
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RubricCriterion?>(null) }
    var deleting by remember { mutableStateOf<RubricCriterion?>(null) }
    var selectedPreset by remember { mutableStateOf<RubricPreset?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rúbrica · ${category.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
                Text("Los criterios internos deben sumar exactamente 100%.")
                Text(
                    "Total: ${"%.1f".format(total)}%",
                    color = if (abs(total - 100.0) < 0.001 || criteria.isEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))

                if (criteria.isEmpty()) {
                    Text("Ideas para empezar", style = MaterialTheme.typography.titleSmall)
                    rubricPresets.forEach { preset ->
                        OutlinedButton(onClick = { selectedPreset = preset }, modifier = Modifier.fillMaxWidth()) {
                            Text(preset.name)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(criteria, key = { it.id }) { criterion ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${criterion.name} · ${"%.1f".format(criterion.weight)}%", modifier = Modifier.weight(1f))
                                IconButton(onClick = { editing = criterion }) { Icon(Icons.Default.Edit, null) }
                                IconButton(onClick = { deleting = criterion }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = { showNew = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar criterio")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = criteria.isEmpty() || abs(total - 100.0) < 0.001,
                onClick = onDismiss
            ) { Text("Listo") }
        }
    )

    selectedPreset?.let { preset ->
        AlertDialog(
            onDismissRequest = { selectedPreset = null },
            title = { Text("Usar idea: ${preset.name}") },
            text = {
                Column {
                    preset.criteria.forEach { (name, weight) -> Text("• $name — ${"%.0f".format(weight)}%") }
                    Spacer(Modifier.height(8.dp))
                    Text("Total: 100%. Después puedes modificar los criterios.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preset.criteria.forEachIndexed { index, pair ->
                        db.saveRubricCriterion(RubricCriterion(0, category.id, pair.first, pair.second, index))
                    }
                    selectedPreset = null
                    onChanged()
                }) { Text("Usar esta rúbrica") }
            },
            dismissButton = { TextButton(onClick = { selectedPreset = null }) { Text("Cancelar") } }
        )
    }

    if (showNew) CriterionDialog(
        RubricCriterion(0, category.id, "", 0.0, criteria.size),
        onDismiss = { showNew = false },
        onSave = { db.saveRubricCriterion(it); showNew = false; onChanged() }
    )

    editing?.let { criterion ->
        CriterionDialog(
            criterion,
            onDismiss = { editing = null },
            onSave = { db.saveRubricCriterion(it); editing = null; onChanged() }
        )
    }

    deleting?.let { criterion ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar criterio") },
            text = { Text("¿Eliminar '${criterion.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteRubricCriterion(criterion.id)
                    deleting = null
                    onChanged()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CriterionDialog(
    initial: RubricCriterion,
    onDismiss: () -> Unit,
    onSave: (RubricCriterion) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var weightText by remember(initial.id) { mutableStateOf(if (initial.weight == 0.0) "" else initial.weight.toString()) }
    val weight = weightText.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nuevo criterio" else "Editar criterio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("¿Qué vas a evaluar?") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weightText, { weightText = it }, label = { Text("Porcentaje dentro de la rúbrica") }, modifier = Modifier.fillMaxWidth())
                Text("Ejemplos: conocimiento del tema, metodología, resultados, discusión, ortografía, claridad, redacción, uso del tiempo.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && weight != null && weight in 0.0..100.0,
                onClick = { onSave(initial.copy(name = name, weight = weight!!)) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
