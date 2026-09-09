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
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.EvaluationCategory
import com.profecuaderno.app.data.RubricCriterion
import com.profecuaderno.app.data.TeacherDbHelper
import kotlin.math.abs

@Composable
fun RubricsScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    val categories = remember(refresh, period.id) { db.getCategories(period.id) }
    val total = categories.sumOf { it.weight }
    var editingCategory by remember { mutableStateOf<EvaluationCategory?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    var rubricCategory by remember { mutableStateOf<EvaluationCategory?>(null) }
    var deletingCategory by remember { mutableStateOf<EvaluationCategory?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Calificación final", style = MaterialTheme.typography.titleMedium)
                Text("Todos los rubros deben sumar exactamente 100%.")
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
                val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
                val rubricTotal = criteria.sumOf { it.weight }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(category.name, style = MaterialTheme.typography.titleMedium)
                                Text("Peso final: ${"%.1f".format(category.weight)}%")
                                if (criteria.isNotEmpty()) {
                                    Text("Rúbrica: ${criteria.size} criterios · ${"%.1f".format(rubricTotal)}%", style = MaterialTheme.typography.bodySmall)
                                } else Text("Sin rúbrica interna", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { editingCategory = category }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { deletingCategory = category }) { Icon(Icons.Default.Delete, "Eliminar") }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { rubricCategory = category }) {
                            Text(if (criteria.isEmpty()) "Crear rúbrica" else "Editar rúbrica")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showNewCategory) CategoryDialog(
        title = "Nuevo rubro",
        initial = EvaluationCategory(0, period.id, "", 0.0, categories.size),
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
        RubricDialog(db = db, category = cat, refresh = refresh, onDismiss = { rubricCategory = null }, onChanged = onChanged)
    }

    deletingCategory?.let { cat ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Eliminar rubro") },
            text = { Text("¿Eliminar '${cat.name}'? Se eliminarán su rúbrica y calificaciones asociadas.") },
            confirmButton = { TextButton(onClick = { db.deleteCategory(cat.id); deletingCategory = null; onChanged() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CategoryDialog(title: String, initial: EvaluationCategory, onDismiss: () -> Unit, onSave: (EvaluationCategory) -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var weightText by remember(initial.id) { mutableStateOf(if (initial.weight == 0.0) "" else initial.weight.toString()) }
    val weight = weightText.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre del rubro") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weightText, { weightText = it }, label = { Text("Valor dentro de la calificación final (%)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && weight != null && weight in 0.0..100.0, onClick = { onSave(initial.copy(name = name, weight = weight!!)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RubricDialog(db: TeacherDbHelper, category: EvaluationCategory, refresh: Int, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val criteria = remember(refresh, category.id) { db.getRubricCriteria(category.id) }
    val total = criteria.sumOf { it.weight }
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RubricCriterion?>(null) }
    var deleting by remember { mutableStateOf<RubricCriterion?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rúbrica · ${category.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                Text("Los criterios internos también deben sumar 100%.")
                Text("Total: ${"%.1f".format(total)}%", color = if (abs(total - 100.0) < 0.001 || criteria.isEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
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
                Button(onClick = { showNew = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Agregar criterio") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = criteria.isEmpty() || abs(total - 100.0) < 0.001,
                onClick = onDismiss
            ) { Text("Listo") }
        }
    )

    if (showNew) CriterionDialog(
        initial = RubricCriterion(0, category.id, "", 0.0, criteria.size),
        onDismiss = { showNew = false },
        onSave = { db.saveRubricCriterion(it); showNew = false; onChanged() }
    )
    editing?.let { criterion ->
        CriterionDialog(
            initial = criterion,
            onDismiss = { editing = null },
            onSave = { db.saveRubricCriterion(it); editing = null; onChanged() }
        )
    }
    deleting?.let { criterion ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar criterio") },
            text = { Text("¿Eliminar '${criterion.name}'?") },
            confirmButton = { TextButton(onClick = { db.deleteRubricCriterion(criterion.id); deleting = null; onChanged() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CriterionDialog(initial: RubricCriterion, onDismiss: () -> Unit, onSave: (RubricCriterion) -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var weightText by remember(initial.id) { mutableStateOf(if (initial.weight == 0.0) "" else initial.weight.toString()) }
    val weight = weightText.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Nuevo criterio" else "Editar criterio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Criterio") })
                OutlinedTextField(weightText, { weightText = it }, label = { Text("Valor dentro de la rúbrica (%)") })
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && weight != null && weight in 0.0..100.0, onClick = { onSave(initial.copy(name = name, weight = weight!!)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
