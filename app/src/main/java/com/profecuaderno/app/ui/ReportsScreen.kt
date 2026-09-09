package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.util.ReportExporter

@Composable
fun ReportsScreen(db: TeacherDbHelper, period: AcademicPeriod, refresh: Int) {
    val rows = remember(refresh, period.id) { db.summaries(period.id) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Concentrado del grupo", style = MaterialTheme.typography.titleMedium)
                    Text("Asistencia sobre días trabajados y calificación final ponderada.")
                }
                Button(enabled = rows.isNotEmpty(), onClick = { ReportExporter.shareCsv(context, period, rows) }) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("CSV")
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.student.id }) { row ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.student.name, style = MaterialTheme.typography.titleSmall)
                        Text("Asistencia: ${"%.1f".format(row.attendancePercent)}%")
                        Text("Calificación final: ${"%.1f".format(row.finalPercent)}%  ·  ${"%.2f".format(row.finalPercent / 10.0)}/10")
                    }
                }
            }
        }
    }
}
