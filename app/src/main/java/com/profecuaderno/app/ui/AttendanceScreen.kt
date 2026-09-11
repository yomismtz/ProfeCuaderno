package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class AttendanceView { MENU, TAKE, REVIEW }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    var view by remember(period.id) { mutableStateOf(AttendanceView.MENU) }
    var showPolicy by remember { mutableStateOf(false) }
    var historyStudent by remember { mutableStateOf<Student?>(null) }
    val policy = remember(refresh, period.id) { AttendancePolicyStore.policy(db, period.id) }

    when (view) {
        AttendanceView.MENU -> Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Asistencia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Elige cómo quieres trabajar con el pase de lista.")
            ElevatedCard(onClick = { view = AttendanceView.TAKE }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Pasar asistencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Abre o crea la sesión y registra a todo el grupo.")
                }
            }
            ElevatedCard(onClick = { view = AttendanceView.REVIEW }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Verificar asistencias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Consulta fechas anteriores y corrige cualquier registro.")
                }
            }
            OutlinedCard(onClick = { showPolicy = true }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reglas de asistencia", fontWeight = FontWeight.SemiBold)
                    }
                    Text("${policy.latePerAbsence} retardos = 1 falta · Justificada: ${policy.justifiedEffect.label}")
                }
            }
        }
        AttendanceView.TAKE -> AttendanceTakeView(
            db, period, refresh, onChanged,
            onBack = { view = AttendanceView.MENU },
            onPolicy = { showPolicy = true },
            onHistory = { historyStudent = it }
        )
        AttendanceView.REVIEW -> AttendanceReviewView(
            db, period, refresh, onChanged,
            onBack = { view = AttendanceView.MENU },
            onPolicy = { showPolicy = true },
            onHistory = { historyStudent = it }
        )
    }

    if (showPolicy) {
        AttendancePolicySheet(db, period, policy, onChanged) { showPolicy = false }
    }
    historyStudent?.let { student ->
        AttendanceHistorySheet(db, period, student, refresh, onChanged) { historyStudent = null }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceTakeView(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    refresh: Int,
    onChanged: () -> Unit,
    onBack: () -> Unit,
    onPolicy: () -> Unit,
    onHistory: (Student) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var title by remember { mutableStateOf("Clase") }
    var worked by remember { mutableStateOf(true) }
    var editSession by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val students = remember(refresh, period.id) { db.getStudents(period.id) }
    val session = remember(refresh, period.id, date) { db.getAttendanceSession(period.id, date) }

    LaunchedEffect(session?.id, session?.title, session?.worked) {
        if (session == null) {
            title = "Clase"
            worked = true
            editSession = true
        } else {
            title = session.title
            worked = session.worked
        }
    }

    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AttendanceTopActions(onBack, onPolicy)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$date · ${session?.title ?: title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (editSession) {
                    DatePickerField(date, { date = it }, "Fecha")
                    OutlinedTextField(title, { title = it }, label = { Text("Sesión") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(worked, { worked = it })
                        Spacer(Modifier.width(8.dp))
                        Text(if (worked) "Día trabajado" else "Clase suspendida")
                    }
                    Button(
                        onClick = {
                            db.createOrUpdateAttendanceSession(period.id, date, title, worked)
                            editSession = false
                            onChanged()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text(if (session == null) "Crear pase de lista" else "Guardar sesión") }
                } else {
                    OutlinedButton(onClick = { editSession = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Editar fecha o sesión") }
                }
            }
        }

        if (session == null) {
            EmptyAttendance("Crea el pase de lista para comenzar.")
        } else if (!session.worked) {
            EmptyAttendance("Esta sesión no se contabiliza como día trabajado.")
        } else {
            AttendanceRoster(db, period, session, students, refresh, query, { query = it }, onChanged, onHistory)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceReviewView(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    refresh: Int,
    onChanged: () -> Unit,
    onBack: () -> Unit,
    onPolicy: () -> Unit,
    onHistory: (Student) -> Unit
) {
    val sessions = remember(refresh, period.id) { db.listAttendanceSessions(period.id) }
    val students = remember(refresh, period.id) { db.getStudents(period.id) }
    var selectedId by remember(period.id) { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    val selected = sessions.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AttendanceTopActions(
            onBack = { if (selected == null) onBack() else selectedId = null },
            onPolicy = onPolicy,
            backLabel = if (selected == null) "Opciones" else "Fechas"
        )
        if (selected == null) {
            Text("Asistencias registradas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sessions.isEmpty()) EmptyAttendance("Todavía no hay pases de lista guardados.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = { it.id }) { item ->
                    val completed = students.count { db.getAttendanceStatus(item.id, it.id) != null }
                    ElevatedCard(onClick = { selectedId = item.id }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.date, fontWeight = FontWeight.Bold)
                            Text(item.title.ifBlank { "Clase" })
                            Text(
                                if (!item.worked) "Clase suspendida" else "$completed/${students.size} alumnos registrados",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else if (!selected.worked) {
            EmptyAttendance("Esta sesión fue marcada como no trabajada.")
        } else {
            Text("${selected.date} · ${selected.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AttendanceRoster(db, period, selected, students, refresh, query, { query = it }, onChanged, onHistory)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceRoster(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    session: AttendanceSession,
    students: List<Student>,
    refresh: Int,
    query: String,
    onQuery: (String) -> Unit,
    onChanged: () -> Unit,
    onHistory: (Student) -> Unit
) {
    val normalized = query.trim().lowercase()
    val filtered = students.filter {
        normalized.isBlank() || it.name.lowercase().contains(normalized) ||
            it.studentCode.lowercase().contains(normalized) || it.email.lowercase().contains(normalized)
    }
    val statuses = remember(refresh, session.id, students) { students.associateWith { db.getAttendanceStatus(session.id, it.id) } }
    val base = statuses.mapValues { AttendancePolicyStore.baseStatus(it.value) }
    val present = base.values.count { it == AttendanceStatus.PRESENT }
    val absent = base.values.count { it == AttendanceStatus.ABSENT }
    val late = base.values.count { it == AttendanceStatus.LATE }
    val justified = base.values.count { it == AttendanceStatus.JUSTIFIED }
    val pending = base.values.count { it == null }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Buscar alumno, matrícula o correo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = {}, label = { Text("P $present") })
            AssistChip(onClick = {}, label = { Text("F $absent") })
            AssistChip(onClick = {}, label = { Text("R $late") })
            AssistChip(onClick = {}, label = { Text("J $justified") })
            AssistChip(onClick = {}, label = { Text("Pendientes $pending") })
        }
        if (pending > 0) {
            OutlinedButton(
                onClick = {
                    students.filter { statuses[it] == null }.forEach {
                        AttendancePolicyStore.setStatus(db, period.id, session.id, it.id, AttendanceStatus.PRESENT)
                    }
                    onChanged()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Completar pendientes como presentes") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filtered, key = { it.id }) { student ->
                val current = AttendancePolicyStore.baseStatus(db.getAttendanceStatus(session.id, student.id))
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(student.name, fontWeight = FontWeight.SemiBold, maxLines = 4)
                        if (student.studentCode.isNotBlank()) Text(student.studentCode, style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            StatusChip("Presente", current == AttendanceStatus.PRESENT) {
                                AttendancePolicyStore.setStatus(db, period.id, session.id, student.id, AttendanceStatus.PRESENT); onChanged()
                            }
                            StatusChip("Falta", current == AttendanceStatus.ABSENT) {
                                AttendancePolicyStore.setStatus(db, period.id, session.id, student.id, AttendanceStatus.ABSENT); onChanged()
                            }
                            StatusChip("Retardo", current == AttendanceStatus.LATE) {
                                AttendancePolicyStore.setStatus(db, period.id, session.id, student.id, AttendanceStatus.LATE); onChanged()
                            }
                            StatusChip("Justificada", current == AttendanceStatus.JUSTIFIED) {
                                AttendancePolicyStore.setStatus(db, period.id, session.id, student.id, AttendanceStatus.JUSTIFIED); onChanged()
                            }
                            OutlinedButton(onClick = { onHistory(student) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Historial")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) }, modifier = Modifier.heightIn(min = 48.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceTopActions(onBack: () -> Unit, onPolicy: () -> Unit, backLabel: String = "Opciones") {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(5.dp))
            Text(backLabel)
        }
        OutlinedButton(onClick = onPolicy, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(5.dp))
            Text("Reglas")
        }
    }
}

@Composable
private fun EmptyAttendance(text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text(text) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendancePolicySheet(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    initial: AttendancePolicy,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var lateText by remember { mutableStateOf(initial.latePerAbsence.toString()) }
    var effect by remember { mutableStateOf(initial.justifiedEffect) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Reglas de asistencia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = lateText,
                onValueChange = { lateText = it.filter(Char::isDigit).take(2) },
                label = { Text("Retardos que equivalen a 1 falta") },
                supportingText = { Text("Valor permitido: 2 a 10") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("¿Cómo cuenta una falta justificada?", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                JustifiedEffect.entries.forEach { item ->
                    FilterChip(selected = effect == item, onClick = { effect = item }, label = { Text(item.label) })
                }
            }
            Button(
                onClick = {
                    AttendancePolicyStore.setJustifiedEffect(db, period.id, effect)
                    AttendancePolicyStore.setLatePerAbsence(db, period.id, lateText.toIntOrNull()?.coerceIn(2, 10) ?: 3)
                    onChanged(); onDismiss()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Guardar reglas") }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AttendanceHistorySheet(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    student: Student,
    refresh: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var localRefresh by remember { mutableIntStateOf(0) }
    val entries = remember(refresh, localRefresh, student.id) { AttendanceHistoryStore.entriesForStudent(db, period.id, student.id) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 4)
                Text("Asistencia actual: ${"%.1f".format(db.attendancePercentage(period.id, student.id))}%")
            }
            if (entries.isEmpty()) item { Text("No hay registros de asistencia para este alumno.") }
            items(entries, key = { "${it.sessionId}-${it.date}" }) { entry ->
                val current = AttendancePolicyStore.baseStatus(db.getAttendanceStatus(entry.sessionId, student.id))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(entry.date, fontWeight = FontWeight.Bold)
                        Text(entry.title.ifBlank { "Clase" })
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(
                                AttendanceStatus.PRESENT to "Presente",
                                AttendanceStatus.ABSENT to "Falta",
                                AttendanceStatus.LATE to "Retardo",
                                AttendanceStatus.JUSTIFIED to "Justificada"
                            ).forEach { (status, label) ->
                                FilterChip(
                                    selected = current == status,
                                    onClick = {
                                        AttendancePolicyStore.setStatus(db, period.id, entry.sessionId, student.id, status)
                                        localRefresh++; onChanged()
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
