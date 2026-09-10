package com.profecuaderno.app.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class NotePoint(val x: Float, val y: Float)
private data class NotebookNote(
    val id: String,
    val title: String,
    val text: String,
    val strokes: List<List<NotePoint>>
)

@Composable
fun NotesScreen() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var selectedId by remember { mutableStateOf<String?>(null) }

    fun persist(updated: List<NotebookNote>) {
        notes = updated
        saveNotes(context, updated)
    }

    if (selectedId == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Bloc de notas", style = MaterialTheme.typography.headlineSmall)
                Text("Escribe con el teclado o dibuja con dedo, stylus o pen. Úsalo para preguntas de examen, ideas, recordatorios y borradores.")
            }
            item {
                Button(
                    onClick = {
                        val note = NotebookNote(UUID.randomUUID().toString(), "Nueva nota", "", emptyList())
                        persist(listOf(note) + notes)
                        selectedId = note.id
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva nota")
                }
            }
            if (notes.isEmpty()) {
                item { Text("Aún no hay notas guardadas.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(notes, key = { it.id }) { note ->
                ElevatedCard(onClick = { selectedId = note.id }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp)) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title.ifBlank { "Sin título" }, style = MaterialTheme.typography.titleMedium)
                            Text(note.text.lineSequence().firstOrNull()?.take(90).orEmpty().ifBlank { "Nota manuscrita o vacía" }, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { persist(notes.filterNot { it.id == note.id }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar nota")
                        }
                    }
                }
            }
        }
    } else {
        val note = notes.firstOrNull { it.id == selectedId }
        if (note == null) {
            selectedId = null
            return
        }
        NoteEditor(
            note = note,
            onSave = { edited -> persist(notes.map { if (it.id == edited.id) edited else it }) },
            onDelete = { persist(notes.filterNot { it.id == note.id }); selectedId = null }
        )
    }
}

@Composable
private fun NoteEditor(note: NotebookNote, onSave: (NotebookNote) -> Unit, onDelete: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var text by remember(note.id) { mutableStateOf(note.text) }
    var strokes by remember(note.id) { mutableStateOf(note.strokes) }
    var drawingMode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Escribe aquí") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = drawingMode,
                    onClick = { drawingMode = !drawingMode },
                    label = { Text(if (drawingMode) "Pen activo" else "Dibujar") },
                    leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null) }
                )
                TextButton(onClick = { strokes = emptyList() }) { Text("Borrar dibujo") }
            }
        }
        item {
            val ink = MaterialTheme.colorScheme.onSurface
            Surface(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(drawingMode, note.id) {
                        if (drawingMode) {
                            detectDragGestures(
                                onDragStart = { p ->
                                    val nx = if (size.width == 0) 0f else p.x / size.width
                                    val ny = if (size.height == 0) 0f else p.y / size.height
                                    strokes = strokes + listOf(listOf(NotePoint(nx, ny)))
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val p = change.position
                                    val nx = if (size.width == 0) 0f else p.x / size.width
                                    val ny = if (size.height == 0) 0f else p.y / size.height
                                    if (strokes.isNotEmpty()) {
                                        strokes = strokes.dropLast(1) + listOf(strokes.last() + NotePoint(nx, ny))
                                    }
                                }
                            )
                        }
                    }
                ) {
                    strokes.forEach { stroke ->
                        if (stroke.size == 1) {
                            val p = stroke.first()
                            drawCircle(ink, radius = 2.5f, center = Offset(p.x * size.width, p.y * size.height))
                        } else if (stroke.size > 1) {
                            val path = Path().apply {
                                moveTo(stroke.first().x * size.width, stroke.first().y * size.height)
                                stroke.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                            }
                            drawPath(path, ink, style = Stroke(width = 4f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSave(note.copy(title = title.trim(), text = text, strokes = strokes))
                        message = "Nota guardada."
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar")
                }
            }
        }
        message?.let { item { Text(it, style = MaterialTheme.typography.bodySmall) } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun loadNotes(context: Context): List<NotebookNote> = runCatching {
    val raw = context.getSharedPreferences("teacher_notes", Context.MODE_PRIVATE).getString("notes", "[]") ?: "[]"
    val array = JSONArray(raw)
    (0 until array.length()).map { index ->
        val o = array.getJSONObject(index)
        val strokeArray = o.optJSONArray("strokes") ?: JSONArray()
        val strokes = (0 until strokeArray.length()).map { si ->
            val points = strokeArray.getJSONArray(si)
            (0 until points.length()).map { pi ->
                val point = points.getJSONObject(pi)
                NotePoint(point.optDouble("x").toFloat(), point.optDouble("y").toFloat())
            }
        }
        NotebookNote(o.getString("id"), o.optString("title"), o.optString("text"), strokes)
    }
}.getOrDefault(emptyList())

private fun saveNotes(context: Context, notes: List<NotebookNote>) {
    val array = JSONArray()
    notes.forEach { note ->
        val o = JSONObject().put("id", note.id).put("title", note.title).put("text", note.text)
        val strokes = JSONArray()
        note.strokes.forEach { stroke ->
            val points = JSONArray()
            stroke.forEach { points.put(JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble())) }
            strokes.put(points)
        }
        o.put("strokes", strokes)
        array.put(o)
    }
    context.getSharedPreferences("teacher_notes", Context.MODE_PRIVATE).edit().putString("notes", array.toString()).apply()
}
