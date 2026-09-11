package com.profecuaderno.app.network

import android.content.Context
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.AttendanceStatus
import com.profecuaderno.app.data.EvaluationMode
import com.profecuaderno.app.data.TeacherDbHelper

/**
 * Puente explícito entre el cuaderno local del docente y el backend central.
 * Los alumnos se relacionan únicamente por correo electrónico exacto para evitar
 * asignar asistencia o calificaciones a una persona equivocada.
 */
class TeacherOnlineSync(
    context: Context,
    private val backend: CentralBackend,
    private val db: TeacherDbHelper,
) {
    private val prefs = context.applicationContext.getSharedPreferences("central_class_links", Context.MODE_PRIVATE)

    suspend fun ensureClass(period: AcademicPeriod): ClassDto {
        val classes = backend.api.classes()
        val savedId = prefs.getInt("period_${period.id}", -1)
        val saved = classes.firstOrNull { it.id == savedId }
        if (saved != null) return saved

        val existing = classes.firstOrNull {
            it.name.trim().equals(period.name.trim(), ignoreCase = true) &&
                it.periodName.trim().equals(period.type.trim(), ignoreCase = true)
        }
        val classroom = existing ?: backend.api.createClass(
            CreateClassRequest(
                name = period.name,
                subject = period.name,
                periodName = period.type,
            )
        )
        prefs.edit().putInt("period_${period.id}", classroom.id).apply()
        return classroom
    }

    suspend fun students(period: AcademicPeriod): Pair<ClassDto, List<UserDto>> {
        val classroom = ensureClass(period)
        return classroom to backend.api.students(classroom.id)
    }

    suspend fun publishNotice(period: AcademicPeriod, title: String, body: String): ClassDto {
        val classroom = ensureClass(period)
        backend.api.createNotice(classroom.id, NoticeRequest(title.trim(), body.trim()))
        return classroom
    }

    suspend fun syncAttendanceAndGrades(period: AcademicPeriod): TeacherSyncSummary {
        val classroom = ensureClass(period)
        val onlineStudents = backend.api.students(classroom.id)
        val byEmail = onlineStudents.associateBy { it.email.trim().lowercase() }
        val localStudents = db.getStudents(period.id)
        val sessions = db.listAttendanceSessions(period.id).filter { it.worked }
        val categories = db.getCategories(period.id)
            .filter { db.effectiveEvaluationMode(it) != EvaluationMode.ATTENDANCE }

        var matched = 0
        var attendanceSent = 0
        var gradesSent = 0
        var failedWrites = 0
        val unmatched = mutableListOf<String>()

        localStudents.forEach localLoop@ { local ->
            val email = local.email.trim().lowercase()
            val online = email.takeIf { it.isNotBlank() }?.let(byEmail::get)
            if (online == null) {
                unmatched += if (local.email.isBlank()) "${local.name} (sin correo)" else "${local.name} (${local.email})"
                return@localLoop
            }
            matched++

            sessions.forEach sessionLoop@ { session ->
                val status = db.getAttendanceStatus(session.id, local.id) ?: return@sessionLoop
                val request = AttendanceRequest(
                    studentId = online.id,
                    date = session.date,
                    status = status.toServerValue(),
                )
                runCatching { backend.api.setAttendance(classroom.id, request) }
                    .onSuccess { attendanceSent++ }
                    .onFailure { failedWrites++ }
            }

            categories.forEach categoryLoop@ { category ->
                val score = db.categoryScoreOrNull(period.id, local.id, category) ?: return@categoryLoop
                val request = GradeRequest(
                    studentId = online.id,
                    category = category.name,
                    activityKey = "category-${category.id}",
                    activityName = category.name,
                    score = score,
                    maxScore = 100.0,
                )
                runCatching { backend.api.setGrade(classroom.id, request) }
                    .onSuccess { gradesSent++ }
                    .onFailure { failedWrites++ }
            }
        }

        return TeacherSyncSummary(
            classroom = classroom,
            onlineStudents = onlineStudents.size,
            matchedStudents = matched,
            attendanceSent = attendanceSent,
            gradesSent = gradesSent,
            failedWrites = failedWrites,
            unmatchedStudents = unmatched,
        )
    }

    private fun AttendanceStatus.toServerValue(): String = when (this) {
        AttendanceStatus.PRESENT -> "present"
        AttendanceStatus.ABSENT -> "absent"
        AttendanceStatus.LATE -> "late"
        AttendanceStatus.JUSTIFIED -> "excused"
    }
}

data class TeacherSyncSummary(
    val classroom: ClassDto,
    val onlineStudents: Int,
    val matchedStudents: Int,
    val attendanceSent: Int,
    val gradesSent: Int,
    val failedWrites: Int,
    val unmatchedStudents: List<String>,
)
