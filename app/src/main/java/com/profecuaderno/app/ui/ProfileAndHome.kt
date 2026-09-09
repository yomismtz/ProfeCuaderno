package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.Teacher
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
                DegreeSelector(degree, { degree = it }, "Grado profesional *")
                DatePickerField(birth, { birth = it }, "Fecha de nacimiento")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(teacher: Teacher, onSave: (Teacher) -> Unit) {
    var name by remember { mutableStateOf(teacher.name) }
    var birth by remember { mutableStateOf(teacher.birthDate) }
    var degree by remember { mutableStateOf(teacher.degree) }
    var institution by remember { mutableStateOf(teacher.institution) }
    var email by remember { mutableStateOf(teacher.email) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mi perfil docente", style = MaterialTheme.typography.titleLarge)
        Text("Estos datos se muestran en tu cuaderno y pueden editarse cuando lo necesites.")
        NotificationPermissionCard()
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        DegreeSelector(degree, { degree = it }, "Grado profesional")
        DatePickerField(birth, { birth = it }, "Fecha de nacimiento")
        OutlinedTextField(institution, { institution = it }, label = { Text("Institución") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onSave(teacher.copy(name = name, birthDate = birth, degree = degree, institution = institution, email = email)) },
            enabled = name.isNotBlank() && degree.isNotBlank()
        ) { Text("Guardar cambios") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DegreeSelector(value: String, onValueChange: (String) -> Unit, label: String) {
    val options = listOf(
        "Maestro(a)",
        "Doctor(a)",
        "Licenciatura",
        "Postdoctorado",
        "Ingeniería",
        "Profesor(a)",
        "Otro"
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private data class FolderAction(val title: String, val subtitle: String, val action: () -> Unit)

@Composable
fun HomeScreen(
    teacher: Teacher,
    period: AcademicPeriod?,
    onPrograms: () -> Unit,
    onCalendar: () -> Unit,
    onProfile: () -> Unit,
    db: TeacherDbHelper,
    refresh: Int
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val columns = if (landscape) 3 else 1
    val todayDate = LocalDate.now()
    val today = todayDate.toString()
    val todaySession = remember(refresh, period?.id) { period?.let { db.getAttendanceSession(it.id, today) } }
    val todayEvents = remember(refresh) { db.eventsOn(today) }
    val todayBirthdays = remember(refresh) { db.birthdaysOn(todayDate) }
    val groupsToday = remember(refresh) {
        db.getOpenGroups().filter { group ->
            val start = runCatching { LocalDate.parse(group.startDate) }.getOrNull()
            val end = runCatching { LocalDate.parse(group.endDate) }.getOrNull()
            (start == null || !todayDate.isBefore(start)) && (end == null || !todayDate.isAfter(end))
        }
    }
    val attendancePending = remember(refresh, groupsToday.map { it.id }) {
        groupsToday.filter { db.getAttendanceSession(it.id, today) == null }
    }

    val actions = listOf(
        FolderAction(
            "Grupos",
            if (period == null) "Crea tu primer grupo" else "Grupo actual: ${period.name}",
            onPrograms
        ),
        FolderAction(
            "Calendario",
            if (period == null) "Crea un programa para usar la agenda" else if (todaySession == null) "Fechas, entregas y cumpleaños" else "Hoy ya tiene registro de asistencia",
            onCalendar
        ),
        FolderAction(
            "Mi perfil docente",
            "${teacher.degree} ${teacher.name}",
            onProfile
        )
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("ProfeCuaderno", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Crea tus grupos y organiza cada uno por carpetas.")
                if (teacher.institution.isNotBlank()) Text(teacher.institution, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (todayEvents.isEmpty() && todayBirthdays.isEmpty() && attendancePending.isEmpty()) {
                    Text("No hay actividades ni avisos pendientes para hoy.", style = MaterialTheme.typography.bodySmall)
                } else {
                    todayEvents.take(4).forEach { (group, event) ->
                        Text("• ${eventTypeLabel(event.type)}: ${event.title} · ${group.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    todayBirthdays.take(4).forEach { (group, student) ->
                        Text("• 🎂 Cumpleaños de ${student.name} · ${group.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    attendancePending.take(4).forEach { group ->
                        Text("• Falta pasar asistencia · ${group.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    val extra = (todayEvents.size - 4).coerceAtLeast(0) + (todayBirthdays.size - 4).coerceAtLeast(0) + (attendancePending.size - 4).coerceAtLeast(0)
                    if (extra > 0) Text("+ $extra avisos más", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(actions) { item ->
                ElevatedCard(onClick = item.action, modifier = Modifier.height(145.dp)) {
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgramHomeScreen(
    period: AcademicPeriod,
    onStudents: () -> Unit,
    onAttendance: () -> Unit,
    onEvaluation: () -> Unit,
    onRubrics: () -> Unit,
    onGuide: () -> Unit,
    onReports: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    val columns = if (landscape) 3 else 2

    val folders = listOf(
        FolderAction("Alumnos", "Datos, correo, teléfono, grupo y cumpleaños", onStudents),
        FolderAction("Asistencia", "Pase de lista y porcentaje sobre días trabajados", onAttendance),
        FolderAction("Evaluación", "Captura calificaciones y calcula el 100%", onEvaluation),
        FolderAction("Rubros y rúbricas", "Rubros del 100%, criterios, ideas y porcentajes editables", onRubrics),
        FolderAction("Guía / planeación", "Sube y consulta el PDF del programa", onGuide),
        FolderAction("Reportes", "Concentrado de asistencia y calificación", onReports)
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(period.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${period.type} · ${period.startDate.ifBlank { "Sin fecha de inicio" }} → ${period.endDate.ifBlank { "Sin fecha de término" }}")
            }
        }
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(folders) { item ->
                ElevatedCard(onClick = item.action, modifier = Modifier.height(145.dp)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
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
