package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.Teacher
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TeacherSetupScreen(onSave: (Teacher) -> Unit) {
    var name by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf("") }
    var degree by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ElevatedCard(Modifier.widthIn(max = 620.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ProfeCuaderno", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Tu cuaderno docente digital")
                    }
                }
                HorizontalDivider()
                Text("Configura tu perfil", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Nombre completo *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(degree, { degree = it }, label = { Text("Grado profesional *") }, placeholder = { Text("Mtra., Dr., Lic., Prof., etc.") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(birth, { birth = it }, label = { Text("Fecha de nacimiento") }, placeholder = { Text("AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(institution, { institution = it }, label = { Text("Institución") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { onSave(Teacher(name = name, birthDate = birth, degree = degree, institution = institution, email = email)) },
                    enabled = name.isNotBlank() && degree.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Entrar a mi cuaderno") }
            }
        }
    }
}

@Composable
fun ProfileScreen(teacher: Teacher, onSave: (Teacher) -> Unit) {
    var name by remember { mutableStateOf(teacher.name) }
    var birth by remember { mutableStateOf(teacher.birthDate) }
    var degree by remember { mutableStateOf(teacher.degree) }
    var institution by remember { mutableStateOf(teacher.institution) }
    var email by remember { mutableStateOf(teacher.email) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Datos del docente", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(degree, { degree = it }, label = { Text("Grado profesional") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(birth, { birth = it }, label = { Text("Fecha de nacimiento") }, placeholder = { Text("AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(institution, { institution = it }, label = { Text("Institución") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSave(teacher.copy(name = name, birthDate = birth, degree = degree, institution = institution, email = email)) }, enabled = name.isNotBlank() && degree.isNotBlank()) {
            Text("Guardar cambios")
        }
    }
}

private data class HomeAction(val title: String, val subtitle: String, val icon: ImageVector, val action: () -> Unit)

@Composable
fun HomeScreen(
    teacher: Teacher,
    period: AcademicPeriod?,
    onStudents: () -> Unit,
    onAttendance: () -> Unit,
    onEvaluation: () -> Unit,
    onRubrics: () -> Unit,
    onCalendar: () -> Unit,
    onPeriods: () -> Unit,
    onReports: () -> Unit,
    db: TeacherDbHelper,
    refresh: Int
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val columns = if (landscape) 4 else 2
    val students = remember(refresh, period?.id) { period?.let { db.getStudents(it.id) }.orEmpty() }
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    val attendanceSession = remember(refresh, period?.id) { period?.let { db.getAttendanceSession(it.id, today) } }
    val categoryTotal = remember(refresh, period?.id) { period?.let { db.categoryWeightTotal(it.id) } ?: 0.0 }

    val actions = listOf(
        HomeAction("Alumnos", "Base de datos del grupo", Icons.Default.Groups, onStudents),
        HomeAction("Asistencia", "Pase de lista y porcentaje", Icons.Default.FactCheck, onAttendance),
        HomeAction("Evaluación", "Calificaciones y 100% final", Icons.Default.Assessment, onEvaluation),
        HomeAction("Rubros", "Rubros y rúbricas editables", Icons.Default.Checklist, onRubrics),
        HomeAction("Calendario", "Fechas y cumpleaños", Icons.Default.CalendarMonth, onCalendar),
        HomeAction("Periodos", "Trimestre, semestre y archivo", Icons.Default.FolderCopy, onPeriods),
        HomeAction("Reportes", "Resumen y exportación CSV", Icons.Default.FileDownload, onReports)
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("${teacher.degree} ${teacher.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(period?.let { "${it.type}: ${it.name}" } ?: "Sin periodo activo")
                if (period != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        AssistChip(onClick = onStudents, label = { Text("${students.size} alumnos") }, leadingIcon = { Icon(Icons.Default.People, null) })
                        AssistChip(onClick = onAttendance, label = { Text(if (attendanceSession == null) "Lista pendiente" else "Lista registrada") }, leadingIcon = { Icon(Icons.Default.EventAvailable, null) })
                        AssistChip(onClick = onRubrics, label = { Text("Rubros: ${"%.0f".format(categoryTotal)}%") }, leadingIcon = { Icon(Icons.Default.Percent, null) })
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(columns), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(actions) { item ->
                ElevatedCard(onClick = item.action, modifier = Modifier.height(132.dp)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
