package com.profecuaderno.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.profecuaderno.app.security.AppSecurityManager
import com.profecuaderno.app.data.TeacherDbHelper
import com.profecuaderno.app.notifications.ReminderScheduler
import com.profecuaderno.app.ui.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfeCuadernoTheme {
                val context = LocalContext.current
                val db = remember { TeacherDbHelper(context) }
                LaunchedEffect(Unit) { ReminderScheduler.ensureDaily(context) }
                var refresh by remember { mutableIntStateOf(0) }
                val teacher = remember(refresh) { db.getTeacher() }
                var unlocked by remember { mutableStateOf(!AppSecurityManager.isLockEnabled(context)) }
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP && AppSecurityManager.isLockEnabled(context)) {
                            unlocked = false
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                NotebookBackground {
                    if (!unlocked && AppSecurityManager.isLockEnabled(context)) {
                        AppLockScreen(onUnlocked = { unlocked = true })
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
