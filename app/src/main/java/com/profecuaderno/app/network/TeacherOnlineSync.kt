package com.profecuaderno.app.network

import android.content.Context
import com.profecuaderno.app.data.AcademicPeriod
import com.profecuaderno.app.data.AttendancePolicyStore
import com.profecuaderno.app.data.AttendanceStatus
import com.profecuaderno.app.data.EvaluationMode
import com.profecuaderno.app.data.JustifiedEffect
import com.profecuaderno.app.data.TeamFormationStore
import com.profecuaderno.app.data.TeacherDbHelper

/**
 * Puente explícito entre el cuaderno local del docente y el backend central.
 * Los alumnos se relacionan únicamente por correo electrónico exacto para evitar
 * asignar asistencia, calificaciones o equipos a una persona equivocada.
 */
class TeacherOnlineSync(
    context: Context,
    private val backend: CentralBackend,
    private val db: TeacherDbHelper,
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("central_class_links", Context.MODE_PRIVATE)

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
            CreateClassRequest(name = period.name, subject = period.name, periodName = period.type)
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
        val allSessions = db.listAttendanceSessions(period.id)
        val workedSessions = allSessions.filter { it.worked }
        val categories = db.getCategories(period.id).filter { db.effectiveEvaluationMode(it) != EvaluationMode.ATTENDANCE }

        var matched = 0
        var attendanceSent = 0
        var gradesSent = 0
        var failedWrites = 0
        val unmatched = mutableListOf<String>()

        val localPolicy = AttendancePolicyStore.policy(db, period.id)
        runCatching {
            backend.api.setAttendancePolicy(
                classroom.id,
                AttendancePolicyRequest(
                    latePerAbsence = localPolicy.latePerAbsence,
                    justifiedEffect = localPolicy.justifiedEffect.toServerValue(),
                )
            )
        }.onFailure { failedWrites++ }

        allSessions.forEach { session ->
            runCatching {
                backend.api.setAttendanceSession(
                    classroom.id,
                    AttendanceSessionRequest(
                        date = session.date,
                        title = session.title.ifBlank { "Clase" },
                        worked = session.worked,
                    )
                )
            }.onFailure { failedWrites++ }
        }

        localStudents.forEach localLoop@ { local ->
            val email = local.email.trim().lowercase()
            val online = email.takeIf { it.isNotBlank() }?.let(byEmail::get)
            if (online == null) {
                unmatched += if (local.email.isBlank()) "${local.name} (sin correo)" else "${local.name} (${local.email})"
                return@localLoop
            }
            matched++

            workedSessions.forEach sessionLoop@ { session ->
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

    suspend fun publishTeamFormations(period: AcademicPeriod): TeamPublishSummary {
        val classroom = ensureClass(period)
        val onlineStudents = backend.api.students(classroom.id)
        val onlineByEmail = onlineStudents.associateBy { it.email.trim().lowercase() }
        val localStudents = db.getStudents(period.id)
        val localById = localStudents.associateBy { it.id }
        val onlineIdByLocalId = localStudents.mapNotNull { local ->
            val email = local.email.trim().lowercase()
            val online = email.takeIf { it.isNotBlank() }?.let(onlineByEmail::get) ?: return@mapNotNull null
            local.id to online.id
        }.toMap()
        val formations = TeamFormationStore.load(appContext, period.id)

        var published = 0
        var failed = 0
        val skipped = mutableListOf<String>()

        formations.forEach { formation ->
            val allIds = formation.teams.flatMap { it.studentIds }.distinct()
            val missingIds = allIds.filterNot(onlineIdByLocalId::containsKey)
            if (missingIds.isNotEmpty()) {
                val names = missingIds.mapNotNull { localById[it]?.name }.ifEmpty { listOf("alumnos sin cuenta vinculada") }
                skipped += "${formation.activityName.ifBlank { formation.activityType }}: ${names.joinToString()}"
                return@forEach
            }

            val teams = formation.teams.map { team ->
                TeamDto(
                    name = team.name.take(100).ifBlank { "Equipo" },
                    studentIds = team.studentIds.mapNotNull(onlineIdByLocalId::get),
                )
            }.filter { it.studentIds.isNotEmpty() }

            if (teams.isEmpty()) {
                skipped += "${formation.activityName.ifBlank { formation.activityType }}: sin integrantes"
                return@forEach
            }

            val activityName = formation.activityName.trim().ifBlank { formation.activityType.trim().ifBlank { "Actividad en equipo" } }.take(200)
            val activityType = formation.activityType.trim().ifBlank { "Trabajo en equipo" }.take(80)
            val category = activityType.take(120)
            val request = TeamActivityRequest(
                name = activityName,
                activityType = activityType,
                category = category,
                activityKey = "team-${formation.id}".take(160),
                teams = teams,
            )
            runCatching { backend.api.createTeamActivity(classroom.id, request) }
                .onSuccess { published++ }
                .onFailure {
                    failed++
                    skipped += "$activityName: ${it.message ?: "error de servidor"}"
                }
        }

        return TeamPublishSummary(
            classroom = classroom,
            localFormations = formations.size,
            published = published,
            failed = failed,
            skipped = skipped,
        )
    }

    suspend fun teamActivities(period: AcademicPeriod): Pair<ClassDto, List<TeamActivityDto>> {
        val classroom = ensureClass(period)
        return classroom to backend.api.teamActivities(classroom.id)
    }

    suspend fun participationSummary(activityId: Int): List<ParticipationSummaryDto> = backend.api.participationSummary(activityId)

    suspend fun reviewParticipation(activityId: Int, studentId: Int, resolution: String, note: String = "") {
        backend.api.reviewParticipation(
            activityId = activityId,
            studentId = studentId,
            request = ParticipationReviewRequest(resolution = resolution, note = note.take(500)),
        )
    }

    suspend fun closeTeamActivity(activityId: Int) {
        backend.api.setTeamScores(
            activityId,
            TeamScoresRequest(baseScores = emptyMap(), individualScores = emptyMap(), closeAndConsolidate = true),
        )
    }

    private fun AttendanceStatus.toServerValue(): String = when (AttendancePolicyStore.baseStatus(this)) {
        AttendanceStatus.PRESENT -> "present"
        AttendanceStatus.ABSENT -> "absent"
        AttendanceStatus.LATE -> "late"
        AttendanceStatus.JUSTIFIED -> "justified"
        else -> "present"
    }

    private fun JustifiedEffect.toServerValue(): String = when (this) {
        JustifiedEffect.PRESENT -> "present"
        JustifiedEffect.LATE -> "late"
        JustifiedEffect.ABSENT -> "absent"
        JustifiedEffect.EXCLUDED -> "excluded"
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

data class TeamPublishSummary(
    val classroom: ClassDto,
    val localFormations: Int,
    val published: Int,
    val failed: Int,
    val skipped: List<String>,
)
