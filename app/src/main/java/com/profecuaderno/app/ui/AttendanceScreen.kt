package com.profecuaderno.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.AttendanceHistoryStore
import com.profecuaderno.app.data.AttendancePolicyStore
import com.profecuaderno.app.data.AttendanceStatus
import com.profecuaderno.app.data.Student
import com.profecuaderno.app.data.TeacherDbHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AttendanceScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int, onChanged: () -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var title by remember { mutableStateOf("Clase") }
    val students = remember(refresh, period.id) { db.getStudents(period.id) }
    val session = remember(refresh, period.id, date) { db.getAttendanceSession(period.id, date) }
    var worked by remember(session?.id, session?.worked) { mutableStateOf(session?.worked ?: true) }
    var historyStudent by remember { mutableStateOf<Student?>(null) }
    var justifiedCounts by remember(refresh, period.id) {
        mutableStateOf(AttendancePolicyStore.justifiedCounts(db, period.id))
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pase de lista", style = MaterialTheme.typography.titleMedium)
                DatePickerField(date, { date = it }, "Fecha de la sesión")
                OutlinedTextField(title, { title = it }, label = { Text("Sesión") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = worked, onCheckedChange = { worked = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (worked) "Día trabajado (sí cuenta para el porcentaje)" else "Clase suspendida / no trabajada")
                }
                Button(
                    onClick = {
                        db.createOrUpdateAttendanceSession(period.id, date, title, worked)
                        onChanged()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (session == null) "Crear pase de lista" else "Guardar datos de la sesión") }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Faltas justificadas", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (justifiedCounts) "La justificada cuenta como asistencia" else "La justificada no afecta el porcentaje")
                        Text(
                            if (justifiedCounts)
                                "Se conserva como 'Justificada' en el registro y aporta 100% para esa sesión."
                            else
                                "La sesión justificada se excluye del cálculo únicamente para ese estudiante.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = justifiedCounts,
                        onCheckedChange = { value ->
                            justifiedCounts = value
                            AttendancePolicyStore.setJustifiedCounts(db, period.id, value)
                            onChanged()
                        }
                    )
                }
            }
        }

        if (session == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Crea la sesión para comenzar a pasar lista.")
            }
        } else if (!session.worked) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Esta fecha está marcada como no trabajada y no afecta el porcentaje de asistencia.")
            }
        } else if (students.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Agrega estudiantes primero.") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students, key = { it.id }) { student ->
                    val current = remember(refresh, session.id, student.id) { db.getAttendanceStatus(session.id, student.id) }
                    val incidents = remember(refresh, period.id, student.id) {
                        AttendanceHistoryStore.incidentsForStudent(db, period.id, student.id)
                    }
                    val absenceCount = incidents.count { it.status == AttendanceStatus.ABSENT }
                    val justifiedCount = incidents.count { it.status == AttendanceStatus.JUSTIFIED }

                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(student.name, style = MaterialTheme.typography.titleSmall)
                                    Text("Asistencia acumulada: ${"%.1f".format(db.attendancePercentage(period.id, student.id))}%", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Faltas: $absenceCount · Justificadas: $justifiedCount",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = { historyStudent = student },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Historial")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AttendanceStatus.entries.forEach { status ->
                                    FilterChip(
                                        selected = current == status,
                                        onClick = {
                                            db.setAttendanceStatus(session.id, student.id, status)
                                            onChanged()
                                        },
                                        label = { Text(shortStatus(status)) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    historyStudent?.let { student ->
        AttendanceHistoryDialog(
            db = db,
            period = period,
            student = student,
            refresh = refresh,
            onChanged = onChanged,
            onDismiss = { historyStudent = null }
        )
    }
}

@Composable
private fun AttendanceHistoryDialog(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    student: Student,
    refresh: Int,
    onChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    var localRefresh by remember { mutableIntStateOf(0) }
    val tick = refresh + localRefresh
    val incidents = remember(tick, period.id, student.id) {
        AttendanceHistoryStore.incidentsForStudent(db, period.id, student.id)
    }
    val absences = incidents.count { it.status == AttendanceStatus.ABSENT }
    val justified = incidents.count { it.status == AttendanceStatus.JUSTIFIED }
    val late = incidents.count { it.status == AttendanceStatus.LATE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Historial de asistencia")
                Text(student.name, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Asistencia actual: ${"%.1f".format(db.attendancePercentage(period.id, student.id))}%",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Faltas: $absences · Justificadas: $justified · Retardos: $late",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Aquí puedes revisar qué días faltó el estudiante. Cuando entregue un justificante, cambia esa fecha a Justificada; el porcentaje se recalcula de inmediato.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (incidents.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Este estudiante no tiene faltas, retardos ni justificadas registradas.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(incidents, key = { "${it.sessionId}-${it.status.name}" }) { entry ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.date, style = MaterialTheme.typography.titleSmall)
                                            Text(entry.title.ifBlank { "Clase" }, style = MaterialTheme.typography.bodySmall)
                                        }
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(shortStatus(entry.status)) }
                                        )
                                    }

                                    when (entry.status) {
                                        AttendanceStatus.ABSENT -> {
                                            Button(
                                                onClick = {
                                                    db.setAttendanceStatus(entry.sessionId, student.id, AttendanceStatus.JUSTIFIED)
                                                    localRefresh++
                                                    onChanged()
                                                },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Text("Cambiar falta a Justificada")
                                            }
                                        }
                                        AttendanceStatus.JUSTIFIED -> {
                                            OutlinedButton(
                                                onClick = {
                                                    db.setAttendanceStatus(entry.sessionId, student.id, AttendanceStatus.ABSENT)
                                                    localRefresh++
                                                    onChanged()
                                                },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Volver a marcar como Falta")
                                            }
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cerrar")
            }
        }
    )
}

private fun shortStatus(status: AttendanceStatus): String = when (status) {
    AttendanceStatus.PRESENT -> "✓ Presente"
    AttendanceStatus.ABSENT -> "✕ Falta"
    AttendanceStatus.LATE -> "◷ Retardo"
    AttendanceStatus.JUSTIFIED -> "J Justificada"
}
