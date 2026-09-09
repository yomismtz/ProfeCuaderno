package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class IntroStep(
    val title: String,
    val text: String,
    val example: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onStart: () -> Unit
) {
    val steps = listOf(
        IntroStep(
            "1. Crea tu perfil y tus grupos",
            "Registra tus datos docentes y crea uno o varios grupos por trimestre, semestre, cuatrimestre o el periodo que uses.",
            "Ejemplo: Grupo 26-O · Estomatología · 17 sep–4 dic.",
            Icons.Default.Folder
        ),
        IntroStep(
            "2. Agrega alumnos y pasa asistencia",
            "En cada grupo guarda alumnos, matrícula, correo, teléfono y fecha de nacimiento. La asistencia se calcula sobre los días realmente trabajados.",
            "Ejemplo: Presente, falta, retardo o justificada. Los cumpleaños aparecen solos en el calendario.",
            Icons.Default.FactCheck
        ),
        IntroStep(
            "3. Organiza la evaluación hasta 100%",
            "Crea rubros como exámenes, prácticas, laboratorio, tareas, asistencia, investigación y exposiciones. Tú decides el porcentaje de cada uno y el total debe ser 100%.",
            "Ejemplo: Exámenes 25% + Prácticas 15% + Tareas 10% + Asistencia 10% + Investigación 20% + Exposiciones 20% = 100%.",
            Icons.Default.Assessment
        ),
        IntroStep(
            "4. Usa actividades o rúbricas dentro de cada rubro",
            "Un rubro puede calcularse por promedio de actividades, por rúbrica interna, por asistencia automática o con una calificación directa.",
            "Ejemplo: Exámenes → Examen 1, 2, 3 y final. Investigación → marco teórico, metodología, resultados, discusión, conclusión y redacción; esos criterios internos suman 100%.",
            Icons.Default.Checklist
        ),
        IntroStep(
            "5. Planea desde la guía y el calendario",
            "Guarda la guía o planeación del grupo y registra fechas de temas, prácticas, laboratorios, exámenes, evaluaciones, exposiciones, entregas, visitas y actividades externas.",
            "Ejemplo: 12 oct · Examen parcial. 20 oct · Práctica 4. 4 nov · Exposición modular. La misma fecha aparece en el calendario general.",
            Icons.Default.CalendarMonth
        ),
        IntroStep(
            "6. Activa recordatorios y revisa reportes",
            "Cuando quieras, activa las notificaciones para recibir avisos de cumpleaños y actividades programadas. En Reportes podrás revisar asistencia y calificación final.",
            "Ejemplo: “Hoy es el cumpleaños de Ana” o “Evaluación parcial · Grupo A”.",
            Icons.Default.NotificationsActive
        )
    )

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Bienvenido a ProfeCuaderno",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tu cuaderno docente digital para organizar grupos, alumnos, asistencia, evaluación, planeación, calendario y reportes.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("¿Para qué sirve?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("La idea es que concentres en una sola app lo que normalmente tienes repartido entre listas, hojas de cálculo, rúbricas, calendarios y documentos.")
                    }
                }
            }

            steps.forEach { step ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(step.text)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    step.example,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Importante", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text("Las notificaciones se solicitan después, cuando actives los recordatorios. Puedes usar ProfeCuaderno aunque no les des permiso.")
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Comenzar a usar ProfeCuaderno")
            }
        }
    }
}
