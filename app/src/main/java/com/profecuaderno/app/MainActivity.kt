package com.profecuaderno.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.notifications.ReminderScheduler
import com.profecuaderno.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfeCuadernoTheme {
                val context = LocalContext.current
                val db = remember { TeacherDbHelper(context) }
                LaunchedEffect(Unit) { ReminderScheduler.ensureDaily(context) }
                var refresh by remember { mutableIntStateOf(0) }
                val teacher = remember(refresh) { db.getTeacher() }

                NotebookBackground {
                    if (teacher == null) {
                        TeacherSetupScreen(
                            onSave = {
                                db.saveTeacher(it)
                                refresh++
                            }
                        )
                    } else {
                        ProfeCuadernoApp(
                            db = db,
                            onDataChanged = { refresh++ },
                            globalRefresh = refresh
                        )
                    }
                }
            }
        }
    }
}
