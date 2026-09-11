package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.network.CentralBackend
import com.profecuaderno.app.network.ClassDto
import com.profecuaderno.app.network.TeacherOnlineSync
import com.profecuaderno.app.network.TeacherSyncSummary
import com.profecuaderno.app.network.UserDto
import kotlinx.coroutines.launch

@Composable
fun TeacherOnlineHost(
    db: TeacherDbHelper,
    period: AcademicPeriod?,
    backend: CentralBackend,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        content()
        if (period != null) {
            ExtendedFloatingActionButton(
                onClick = { open = true },
                icon = { Icon(Icons.Default.CloudSync, contentDescription = null) },
                text = { Text("Online") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            )
        }
    }

    if (open && period != null) {
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize()) {
                OnlineClassroomScreen(
                    db = db,
                    period = period,
                    backend = backend,
                    onClose = { open = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineClassroomScreen(
    db: TeacherDbHelper,
    period: AcademicPeriod,
    backend: CentralBackend,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val sync = remember(period.id, backend, db) { TeacherOnlineSync(context, backend, db) }
    val scope = rememberCoroutineScope()
    var classroom by remember { mutableStateOf<ClassDto?>(null) }
    var onlineStudents by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Conectando con el servidor…") }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeBody by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf<TeacherSyncSummary?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { sync.students(period) }
                .onSuccess { (serverClass, students) ->
                    classroom = serverClass
                    onlineStudents = students
                    status = "Grupo online conectado"
                }
                .onFailure { status = it.message ?: "No se pudo conectar con el grupo online" }
            loading = false
        }
    }

    LaunchedEffect(period.id) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sincronización online") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(period.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(status, color = MaterialTheme.colorScheme.primary)
                        classroom?.let {
                            Text("Código para estudiantes: ${it.classCode}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Los alumnos deben escribir este código en El Cuaderno del Estudiante.")
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Publicar aviso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = noticeTitle,
                            onValueChange = { noticeTitle = it },
                            label = { Text("Título") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = noticeBody,
                            onValueChange = { noticeBody = it },
                            label = { Text("Mensaje") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    runCatching { sync.publishNotice(period, noticeTitle, noticeBody) }
                                        .onSuccess {
                                            status = "Aviso publicado para este grupo"
                                            noticeTitle = ""
                                            noticeBody = ""
                                        }
                                        .onFailure { status = it.message ?: "No se pudo publicar el aviso" }
                                    loading = false
                                }
                            },
                            enabled = noticeTitle.isNotBlank() && noticeBody.isNotBlank() && !loading,
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Publicar")
                        }
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Enviar asistencia y calificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Por seguridad, un alumno local se vincula con su cuenta online únicamente cuando ambos tienen exactamente el mismo correo electrónico.")
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    runCatching { sync.syncAttendanceAndGrades(period) }
                                        .onSuccess {
                                            summary = it
                                            classroom = it.classroom
                                            status = "Sincronización terminada"
                                            refresh()
                                        }
                                        .onFailure { status = it.message ?: "No se pudo sincronizar" }
                                    loading = false
                                }
                            },
                            enabled = !loading,
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizar ahora")
                        }
                        summary?.let { result ->
                            Text("Coincidencias seguras: ${result.matchedStudents}/${db.getStudents(period.id).size}")
                            Text("Asistencias enviadas: ${result.attendanceSent} · Calificaciones enviadas: ${result.gradesSent}")
                            if (result.failedWrites > 0) {
                                Text("Operaciones con error: ${result.failedWrites}", color = MaterialTheme.colorScheme.error)
                            }
                            if (result.unmatchedStudents.isNotEmpty()) {
                                Text("Sin vincular:", fontWeight = FontWeight.SemiBold)
                                result.unmatchedStudents.take(8).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                if (result.unmatchedStudents.size > 8) Text("• …y ${result.unmatchedStudents.size - 8} más", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item {
                Text("Alumnos con cuenta online (${onlineStudents.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (onlineStudents.isEmpty()) {
                item { Text("Todavía no hay estudiantes vinculados con el código de este grupo.") }
            } else {
                items(onlineStudents, key = { it.id }) { student ->
                    ListItem(
                        headlineContent = { Text(student.fullName) },
                        supportingContent = { Text(student.email) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
