package com.profecuaderno.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class HelpItem(val title: String, val example: String, val icon: ImageVector)

@Composable
fun HelpScreen() {
    val items = listOf(
        HelpItem("Crear un grupo", "Grupos → + → nombre, periodo, fecha de inicio y término.", Icons.Default.Folder),
        HelpItem("Configurar el 100%", "Rubros y rúbricas → agrega Exámenes, Prácticas, Tareas, Asistencia, Investigación, etc. hasta sumar 100%.", Icons.Default.Percent),
        HelpItem("Crear una rúbrica", "En un rubro elige “Rúbrica” y agrega criterios internos. Esos criterios también deben sumar 100%.", Icons.Default.Checklist),
        HelpItem("Registrar tareas o prácticas", "Elige “Promedio de actividades” y crea Tarea 1, Tarea 2… o Práctica 1, Práctica 2…", Icons.Default.EditNote),
        HelpItem("Pasar asistencia", "Asistencia → selecciona fecha → Presente, Falta, Retardo o Justificada.", Icons.Default.FactCheck),
        HelpItem("Usar la planeación", "Guía / planeación → guarda el PDF y registra exámenes, prácticas, exposiciones, visitas o entregas con fecha.", Icons.Default.CalendarMonth),
        HelpItem("Importar alumnos", "Alumnos → Importar CSV. Puedes cargar nombre, matrícula, correo, teléfono y fecha de nacimiento.", Icons.Default.UploadFile),
        HelpItem("Reportes", "Reportes → toca un alumno para ver su detalle y generar archivos de seguimiento.", Icons.Default.Assessment)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ayuda rápida", style = MaterialTheme.typography.titleLarge)
                    Text("Ejemplos cortos para usar ProfeCuaderno sin una explicación larga al iniciar.")
                }
            }
        }
        items.forEach { item ->
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp)) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(item.example, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
