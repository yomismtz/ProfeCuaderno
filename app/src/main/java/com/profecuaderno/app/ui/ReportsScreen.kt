package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.*
import com.profecuaderno.app.util.PdfReportExporter
import com.profecuaderno.app.util.ReportExporter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int) {
    val rows = remember(refresh, period.id) { db.summaries(period.id) }
    val context = LocalContext.current
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Concentrado del grupo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Toca un alumno para ver fechas exactas de faltas, retardos y justificadas, y corregirlas desde aquí.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = rows.isNotEmpty(),
                            onClick = { ReportExporter.shareCsv(context, period, rows) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("CSV")
                        }
                        Button(
                            enabled = rows.isNotEmpty(),
                            onClick = { PdfReportExporter.shareGroup(context, db, period) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("PDF del grupo")
                        }
                    }
                }
            }
        }
        items(rows, key = { it.student.id }) { row ->
            ElevatedCard(onClick = { selectedStudent = row.student }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.student.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 5)
                    Text("Asistencia: ${"%.1f".format(row.attendancePercent)}%")
                    Text("Calificación final: ${"%.1f".format(row.finalPercent)}% · ${"%.2f".format(row.finalPercent / 10.0)}/10")
                }
            }
        }
    }

    selectedStudent?.let { student ->
        StudentReportSheet(db, period, student, refresh, onDismiss = { selectedStudent = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StudentReportSheet(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    student: Student,
    refresh: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localRefresh by remember { mutableIntStateOf(0) }
    val tick = refresh + localRefresh
    val counts = remember(tick, student.id) { AttendancePolicyStore.aggregatedCounts(db, period.id, student.id) }
    val entries = remember(tick, student.id) { AttendanceHistoryStore.entriesForStudent(db, period.id, student.id) }
    val categories = remember(tick, student.id) { db.getCategories(period.id) }
    val policy = remember(tick, period.id) { AttendancePolicyStore.policy(db, period.id) }

    val absences = entries.filter { AttendancePolicyStore.baseStatus(it.status) == AttendanceStatus.ABSENT }
    val lates = entries.filter { AttendancePolicyStore.baseStatus(it.status) == AttendanceStatus.LATE }
    val justified = entries.filter { AttendancePolicyStore.baseStatus(it.status) == AttendanceStatus.JUSTIFIED }

    fun change(entry: StudentAttendanceHistoryEntry, status: AttendanceStatus) {
        AttendancePolicyStore.setStatus(db, period.id, entry.sessionId, student.id, status)
        localRefresh++
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 5)
                if (student.studentCode.isNotBlank()) Text("Matrícula: ${student.studentCode}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("Asistencia: ${"%.1f".format(db.attendancePercentage(period.id, student.id))}%", fontWeight = FontWeight.SemiBold)
                Text("Presentes: ${counts[AttendanceStatus.PRESENT] ?: 0} · Faltas: ${counts[AttendanceStatus.ABSENT] ?: 0}")
                Text("Retardos: ${counts[AttendanceStatus.LATE] ?: 0} · Justificadas: ${counts[AttendanceStatus.JUSTIFIED] ?: 0}")
                Text(
                    "Regla: ${policy.latePerAbsence} retardos = 1 falta · Justificada: ${policy.justifiedEffect.label}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item { Text("Incidencias por fecha", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item {
                IncidentGroup("Faltas", absences, AttendanceStatus.ABSENT, ::change)
            }
            item {
                IncidentGroup("Retardos", lates, AttendanceStatus.LATE, ::change)
            }
            item {
                IncidentGroup("Justificadas", justified, AttendanceStatus.JUSTIFIED, ::change)
            }

            item {
                Text("Evaluación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(categories, key = { it.id }) { category ->
                val score = db.categoryScoreOrNull(period.id, student.id, category)
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (score == null) "${category.name}: Sin evaluar" else "${category.name}: ${"%.1f".format(score)}/100",
                            fontWeight = FontWeight.SemiBold
                        )
                        when (runCatching { EvaluationMode.valueOf(category.mode) }.getOrDefault(EvaluationMode.DIRECT)) {
                            EvaluationMode.AVERAGE -> {
                                val items = db.getAssessmentItems(category.id)
                                val evaluated = items.count { db.getAssessmentScore(student.id, it.id) != null }
                                val zeros = items.count { db.getAssessmentScore(student.id, it.id) == 0.0 }
                                Text("Actividades evaluadas: $evaluated/${items.size} · con 0: $zeros", style = MaterialTheme.typography.bodySmall)
                            }
                            EvaluationMode.RUBRIC -> Text("Rúbrica: ${db.getRubricCriteria(category.id).size} criterios", style = MaterialTheme.typography.bodySmall)
                            else -> Unit
                        }
                    }
                }
            }

            item {
                Text(
                    "Calificación final: ${"%.1f".format(db.finalPercentage(period.id, student.id))}% · ${"%.2f".format(db.finalPercentage(period.id, student.id) / 10.0)}/10",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { PdfReportExporter.shareStudent(context, db, period, student) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Generar PDF del alumno")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Cerrar") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncidentGroup(
    title: String,
    entries: List<StudentAttendanceHistoryEntry>,
    currentStatus: AttendanceStatus,
    onChange: (StudentAttendanceHistoryEntry, AttendanceStatus) -> Unit
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$title (${entries.size})", fontWeight = FontWeight.SemiBold)
            if (entries.isEmpty()) {
                Text("Sin registros.", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(entry.date, fontWeight = FontWeight.SemiBold)
                        Text(entry.title.ifBlank { "Clase" }, style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(
                                AttendanceStatus.PRESENT to "Presente",
                                AttendanceStatus.ABSENT to "Falta",
                                AttendanceStatus.LATE to "Retardo",
                                AttendanceStatus.JUSTIFIED to "Justificada"
                            ).forEach { (status, label) ->
                                FilterChip(
                                    selected = status == currentStatus,
                                    onClick = { onChange(entry, status) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
