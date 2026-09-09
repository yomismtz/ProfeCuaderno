package com.profecuaderno.app.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.profecuaderno.app.R

private const val PROFILE_PREFS = "teacher_profile_extras"
private const val KEY_TEACHING_LEVEL = "teaching_level"
private const val KEY_AVATAR = "avatar_index"

fun loadTeachingLevel(context: Context): String =
    context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).getString(KEY_TEACHING_LEVEL, "").orEmpty()

fun saveTeachingLevel(context: Context, value: String) {
    context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TEACHING_LEVEL, value).apply()
}

fun loadTeacherAvatar(context: Context): Int =
    context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).getInt(KEY_AVATAR, 0)

private fun saveTeacherAvatar(context: Context, value: Int) {
    context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_AVATAR, value).apply()
}

private val teacherAvatars = listOf(
    R.drawable.avatar_01,
    R.drawable.avatar_02,
    R.drawable.avatar_03,
    R.drawable.avatar_04,
    R.drawable.avatar_05,
    R.drawable.avatar_06,
    R.drawable.avatar_07,
    R.drawable.avatar_08,
    R.drawable.avatar_09,
    R.drawable.avatar_10,
    R.drawable.avatar_11,
    R.drawable.avatar_12
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingLevelSelector(value: String, onValueChange: (String) -> Unit) {
    val options = listOf(
        "Preescolar", "Primaria", "Secundaria", "Telesecundaria",
        "Bachillerato / Preparatoria", "Escuela técnica", "Universidad / Licenciatura",
        "Maestría", "Doctorado", "Educación especial", "Taller / Capacitación",
        "Varios niveles", "Otro"
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Nivel que imparte") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

@Composable
fun TeacherPhotoPicker() {
    val context = LocalContext.current
    var selected by remember { mutableIntStateOf(loadTeacherAvatar(context)) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Selecciona tu avatar", style = MaterialTheme.typography.titleMedium)
        Text("Es opcional y podrás cambiarlo después desde Mi perfil.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(teacherAvatars) { index, resId ->
                val number = index + 1
                val chosen = selected == number
                Surface(
                    modifier = Modifier
                        .aspectRatio(1.45f)
                        .then(if (chosen) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selected = number
                            saveTeacherAvatar(context, number)
                        },
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = if (chosen) 4.dp else 1.dp
                ) {
                    Image(
                        painter = painterResource(resId),
                        contentDescription = "Avatar $number",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (selected > 0) {
            Spacer(Modifier.height(6.dp))
            Text("Avatar $selected seleccionado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = { selected = 0; saveTeacherAvatar(context, 0) }) { Text("Usar avatar genérico") }
        }
    }
}

@Composable
fun TeacherAvatar(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val context = LocalContext.current
    val selected = loadTeacherAvatar(context)
    Surface(shape = CircleShape, tonalElevation = 2.dp, modifier = modifier.size(size)) {
        if (selected in 1..teacherAvatars.size) {
            Image(
                painter = painterResource(teacherAvatars[selected - 1]),
                contentDescription = "Avatar del docente",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.55f))
            }
        }
    }
}
