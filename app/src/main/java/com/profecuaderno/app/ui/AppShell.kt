package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.TeacherDbHelper

private enum class Screen(val title: String) {
    HOME("ProfeCuaderno"),
    STUDENTS("Alumnos"),
    ATTENDANCE("Asistencia"),
    EVALUATION("Evaluación"),
    RUBRICS("Rubros y rúbricas"),
    CALENDAR("Calendario"),
    PERIODS("Periodos"),
    REPORTS("Reportes"),
    PROFILE("Mi perfil")
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

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screen.title)
                        if (screen != Screen.PERIODS && period != null) {
                            Text(period.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    if (screen != Screen.HOME) {
                        IconButton(onClick = { screen = Screen.HOME }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { screen = Screen.PROFILE }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    teacher = teacher,
                    period = period,
                    onStudents = { screen = Screen.STUDENTS },
                    onAttendance = { screen = Screen.ATTENDANCE },
                    onEvaluation = { screen = Screen.EVALUATION },
                    onRubrics = { screen = Screen.RUBRICS },
                    onCalendar = { screen = Screen.CALENDAR },
                    onPeriods = { screen = Screen.PERIODS },
                    onReports = { screen = Screen.REPORTS },
                    db = db,
                    refresh = tick
                )
                Screen.STUDENTS -> RequirePeriod(period) {
                    StudentsScreen(db, period!!, tick, refreshAll)
                }
                Screen.ATTENDANCE -> RequirePeriod(period) {
                    AttendanceScreen(db, period!!, tick, refreshAll)
                }
                Screen.EVALUATION -> RequirePeriod(period) {
                    EvaluationScreen(db, period!!, tick, refreshAll)
                }
                Screen.RUBRICS -> RequirePeriod(period) {
                    RubricsScreen(db, period!!, tick, refreshAll)
                }
                Screen.CALENDAR -> RequirePeriod(period) {
                    CalendarScreen(db, period!!, tick, refreshAll)
                }
                Screen.PERIODS -> PeriodsScreen(db, tick, refreshAll)
                Screen.REPORTS -> RequirePeriod(period) {
                    ReportsScreen(db, period!!, tick)
                }
                Screen.PROFILE -> ProfileScreen(teacher) {
                    db.saveTeacher(it)
                    refreshAll()
                    screen = Screen.HOME
                }
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
                    Text("Primero crea un periodo escolar", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Puede ser trimestre, cuatrimestre, semestre, bimestre u otro.")
                }
            }
        }
    } else content()
}
