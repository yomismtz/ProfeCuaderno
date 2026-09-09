package com.profecuaderno.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val PROFILE_PREFS = "teacher_profile_extras"
private const val KEY_TEACHING_LEVEL = "teaching_level"
private const val KEY_PHOTO_URI = "photo_uri"

fun loadTeachingLevel(context: Context): String = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).getString(KEY_TEACHING_LEVEL, "").orEmpty()
fun saveTeachingLevel(context: Context, value: String) { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TEACHING_LEVEL, value).apply() }
fun loadTeacherPhotoUri(context: Context): String = context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).getString(KEY_PHOTO_URI, "").orEmpty()
private fun saveTeacherPhotoUri(context: Context, value: String) { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PHOTO_URI, value).apply() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingLevelSelector(value: String, onValueChange: (String) -> Unit) {
    val options = listOf("Preescolar", "Primaria", "Secundaria", "Telesecundaria", "Bachillerato / Preparatoria", "Escuela técnica", "Universidad / Licenciatura", "Maestría", "Doctorado", "Educación especial", "Taller / Capacitación", "Varios niveles", "Otro")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text("Nivel que imparte") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false }) }
        }
    }
}

@Composable
fun TeacherPhotoPicker() {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf(loadTeacherPhotoUri(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        ExternalActivityGuard.active = false
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val readable = runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } != null }.getOrDefault(false)
            if (readable) { photoUri = uri.toString(); saveTeacherPhotoUri(context, photoUri); message = null } else message = "No se pudo leer esa imagen. Elige otra."
        }
    }
    fun chooseAvatar() {
        ExternalActivityGuard.active = true
        runCatching { launcher.launch(arrayOf("image/*")) }.onFailure { ExternalActivityGuard.active = false; message = "No se pudo abrir el selector." }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Selecciona tu avatar", style = MaterialTheme.typography.titleMedium)
        Text("El avatar es opcional y podrás cambiarlo después.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        val bitmap = remember(photoUri) { if (photoUri.isBlank()) null else runCatching { context.contentResolver.openInputStream(Uri.parse(photoUri))?.use { BitmapFactory.decodeStream(it) } }.getOrNull() }
        Surface(shape = CircleShape, tonalElevation = 2.dp, modifier = Modifier.size(112.dp)) {
            if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Avatar del docente", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(58.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { chooseAvatar() }) {
            Icon(Icons.Default.Face, contentDescription = null); Spacer(Modifier.width(6.dp)); Text(if (photoUri.isBlank()) "Seleccionar avatar" else "Cambiar avatar")
        }
        if (photoUri.isNotBlank()) TextButton(onClick = { photoUri = ""; saveTeacherPhotoUri(context, "") }) { Text("Quitar avatar") }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
