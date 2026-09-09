package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper

private enum class Screen(val title: String) {
    HOME("Inicio"),
    PROGRAMS("Grupos"),
    PROGRAM_HOME("Grupo"),
    STUDENTS("Alumnos"),
    ATTENDANCE("Asistencia"),
    EVALUATION("Evaluación"),
    RUBRICS("Rubros y rúbricas"),
    GUIDE("Guía / planeación"),
    CALENDAR("Calendario"),
    REPORTS("Reportes"),
    PROFILE("Mi perfil docente"),
    HELP("Ayuda"),
    SECURITY("Seguridad y respaldo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfeCuadernoApp(db: TeacherDbHelper, onDataChanged: () -> Unit, globalRefresh: Int) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var localRefresh by remember { mutableIntStateOf(0) }
    val tick = globalRefresh + localRefresh
    val teacher = remember(tick) { db.getTeacher()!! }
    val period = remember(tick) { db.getActivePeriod() }

    val refreshAll = {
        localRefresh++
        onDataChanged()
    }

    fun goBack() {
        screen = when (screen) {
            Screen.PROGRAM_HOME -> Screen.PROGRAMS
            Screen.STUDENTS, Screen.ATTENDANCE, Screen.EVALUATION,
            Screen.RUBRICS, Screen.GUIDE, Screen.REPORTS -> Screen.PROGRAM_HOME
            Screen.PROGRAMS, Screen.CALENDAR, Screen.PROFILE, Screen.HELP -> Screen.HOME
            Screen.SECURITY -> Screen.PROFILE
            Screen.HOME -> Screen.HOME
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screen.title)
                        if (screen != Screen.HOME && screen != Screen.PROFILE && screen != Screen.PROGRAMS && period != null) {
                            Text(period.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    if (screen != Screen.HOME) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                },
                actions = {
                    if (screen != Screen.HELP) {
                        IconButton(onClick = { screen = Screen.HELP }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Ayuda")
                        }
                    }
                    if (screen != Screen.PROFILE) {
                        IconButton(onClick = { screen = Screen.PROFILE }) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil docente")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    teacher = teacher,
                    period = period,
                    db = db,
                    refresh = tick,
                    onPrograms = { screen = Screen.PROGRAMS },
                    onCalendar = { screen = Screen.CALENDAR },
                    onProfile = { screen = Screen.PROFILE }
                )
                Screen.PROGRAMS -> PeriodsScreen(
                    db = db,
                    refresh = tick,
                    onChanged = refreshAll,
                    onOpen = {
                        db.activatePeriod(it.id)
                        refreshAll()
                        screen = Screen.PROGRAM_HOME
                    }
                )
                Screen.PROGRAM_HOME -> RequirePeriod(period) {
                    ProgramHomeScreen(
                        period = period!!,
                        onStudents = { screen = Screen.STUDENTS },
                        onAttendance = { screen = Screen.ATTENDANCE },
                        onEvaluation = { screen = Screen.EVALUATION },
                        onRubrics = { screen = Screen.RUBRICS },
                        onGuide = { screen = Screen.GUIDE },
                        onReports = { screen = Screen.REPORTS }
                    )
                }
                Screen.STUDENTS -> RequirePeriod(period) { StudentsScreen(db, period!!, tick, refreshAll) }
                Screen.ATTENDANCE -> RequirePeriod(period) { AttendanceScreen(db, period!!, tick, refreshAll) }
                Screen.EVALUATION -> RequirePeriod(period) { EvaluationScreen(db, period!!, tick, refreshAll) }
                Screen.RUBRICS -> RequirePeriod(period) { RubricsScreen(db, period!!, tick, refreshAll) }
                Screen.GUIDE -> RequirePeriod(period) { GuideScreen(db, period!!, tick, refreshAll) }
                Screen.CALENDAR -> CalendarScreen(db, tick, refreshAll)
                Screen.REPORTS -> RequirePeriod(period) { ReportsScreen(db, period!!, tick) }
                Screen.PROFILE -> ProfileScreen(
                    teacher = teacher,
                    onSecurity = { screen = Screen.SECURITY },
                    onSave = {
                        db.saveTeacher(it)
                        refreshAll()
                        screen = Screen.HOME
                    }
                )
                Screen.HELP -> HelpScreen()
                Screen.SECURITY -> SecurityBackupScreen(db) { refreshAll() }
            }
        }
    }
}

@Composable
private fun RequirePeriod(period: AcademicPeriod?, content: @Composable () -> Unit) {
    if (period == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ElevatedCard {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Primero crea un grupo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Después podrás agregar alumnos, asistencia, evaluación, rúbricas y demás carpetas.")
                }
            }
        }
    } else content()
}
