package com.profecuaderno.app.network

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.profecuaderno.app.data.TeacherDbHelper
import java.util.concurrent.TimeUnit

class TeacherAutoSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val backend = CentralBackend(applicationContext)
        if (backend.tokenStore.accessToken.isNullOrBlank()) return Result.success()

        val db = TeacherDbHelper(applicationContext)
        val sync = TeacherOnlineSync(applicationContext, backend, db)
        val periods = db.getOpenGroups()
        if (periods.isEmpty()) return Result.success()

        var failed = false
        periods.forEach { period ->
            runCatching { sync.syncAttendanceAndGrades(period) }
                .onFailure { failed = true }
        }
        return if (failed) Result.retry() else Result.success()
    }
}

object TeacherSyncScheduler {
    private const val PERIODIC_NAME = "teacher-online-periodic-sync"
    private const val IMMEDIATE_NAME = "teacher-online-pending-sync"

    private fun connectedConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<TeacherAutoSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<TeacherAutoSyncWorker>()
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
