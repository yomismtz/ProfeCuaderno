package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.Student
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.util.CsvStudentImporter

@Composable
fun StudentsScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    val students = remember(refresh, period.id) { db.getStudents(period.id) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Student?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val result = CsvStudentImporter.read(context, uri, period.id)
            result.students.forEach { db.saveStudent(it) }
            importMessage = result.error ?: "Importados: ${result.students.size}${if (result.skipped > 0) " · omitidos: ${result.skipped}" else ""}"
            onChanged()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/plain", "application/vnd.ms-excel")) }) {
                Icon(Icons.Default.UploadFile, null)
                Spacer(Modifier.width(6.dp))
                Text("Importar CSV")
            }
            FilledTonalButton(onClick = { showNew = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Nuevo alumno")
            }
        }

        importMessage?.let {
            AssistChip(
                onClick = { importMessage = null },
                label = { Text(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Box(Modifier.fillMaxSize()) {
        if (students.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no hay alumnos en este periodo.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(students, key = { it.id }) { student ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(student.name, style = MaterialTheme.typography.titleMedium)
                                val second = listOf(student.groupName, student.clinic, student.teamName).filter { it.isNotBlank() }.joinToString(" • ")
                                if (second.isNotBlank()) Text(second, style = MaterialTheme.typography.bodySmall)
                                if (student.email.isNotBlank()) Text(student.email, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { editing = student }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { deleting = student }) { Icon(Icons.Default.Delete, "Eliminar") }
                        }
                    }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
        }
    }

    if (showNew) StudentDialog(
        title = "Nuevo alumno",
        initial = Student(periodId = period.id, name = ""),
        onDismiss = { showNew = false },
        onSave = {
            db.saveStudent(it)
            showNew = false
            onChanged()
        }
    )

    editing?.let { student ->
        StudentDialog(
            title = "Editar alumno",
            initial = student,
            onDismiss = { editing = null },
            onSave = {
                db.saveStudent(it)
                editing = null
                onChanged()
            }
        )
    }

    deleting?.let { student ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar alumno") },
            text = { Text("¿Eliminar a ${student.name}? También se eliminarán sus asistencias y calificaciones de este periodo.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteStudent(student.id)
                    deleting = null
                    onChanged()
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun StudentDialog(title: String, initial: Student, onDismiss: () -> Unit, onSave: (Student) -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var code by remember(initial.id) { mutableStateOf(initial.studentCode) }
    var email by remember(initial.id) { mutableStateOf(initial.email) }
    var phone by remember(initial.id) { mutableStateOf(initial.phone) }
    var birth by remember(initial.id) { mutableStateOf(initial.birthDate) }
    var group by remember(initial.id) { mutableStateOf(initial.groupName) }
    var clinic by remember(initial.id) { mutableStateOf(initial.clinic) }
    var team by remember(initial.id) { mutableStateOf(initial.teamName) }
    var notes by remember(initial.id) { mutableStateOf(initial.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(code, { code = it }, label = { Text("Matrícula / ID") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                DatePickerField(birth, { birth = it }, "Fecha de nacimiento")
                OutlinedTextField(group, { group = it }, label = { Text("Grupo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(clinic, { clinic = it }, label = { Text("Sección / salón / laboratorio / clínica") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(team, { team = it }, label = { Text("Equipo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(initial.copy(name = name, studentCode = code, email = email, phone = phone, birthDate = birth, groupName = group, clinic = clinic, teamName = team, notes = notes))
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
