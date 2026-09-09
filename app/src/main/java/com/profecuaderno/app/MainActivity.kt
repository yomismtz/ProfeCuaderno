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
                val introPrefs = remember { context.getSharedPreferences("onboarding", 0) }
                var introSeen by remember { mutableStateOf(introPrefs.getBoolean("intro_seen_v1", false)) }

                NotebookBackground {
                    if (!introSeen) {
                        OnboardingScreen(
                            onStart = {
                                introPrefs.edit().putBoolean("intro_seen_v1", true).apply()
                                introSeen = true
                            }
                        )
                    } else if (teacher == null) {
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
