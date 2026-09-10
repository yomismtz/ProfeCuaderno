package com.profecuaderno.app.data

/**
 * Contrato local preparado para conectar La Carpeta del Docente con
 * El Cuaderno del Estudiante y El Escritorio del Director.
 *
 * La sincronización real requerirá un backend común. Estos modelos evitan
 * que las tres apps usen conceptos incompatibles cuando se añada ese backend.
 */
object EcosystemLinkContract {
    const val SCHEMA_VERSION = 1

    enum class Audience {
        TEACHER_ONLY,
        CLASS_STUDENTS,
        TEACHER_AND_CLASS_STUDENTS
    }

    enum class SenderRole { DIRECTOR, TEACHER }

    data class ClassLink(
        val classId: String,
        val classCode: String,
        val teacherId: String,
        val groupLabel: String,
        val subjectLabel: String,
        val academicCycleId: String
    )

    data class Announcement(
        val announcementId: String,
        val senderRole: SenderRole,
        val senderId: String,
        val title: String,
        val message: String,
        val audience: Audience,
        val classIds: Set<String> = emptySet(),
        val teacherIds: Set<String> = emptySet(),
        val createdAt: Long,
        val expiresAt: Long? = null
    )

    /**
     * Dirección publica a docentes. El docente decide qué debe retransmitirse
     * a sus alumnos. Un aviso de clase creado por el docente sí puede ir directo
     * a todos los alumnos vinculados por el código de esa clase.
     */
    fun canStudentReceive(announcement: Announcement, classId: String): Boolean =
        announcement.senderRole == SenderRole.TEACHER &&
            announcement.audience != Audience.TEACHER_ONLY &&
            classId in announcement.classIds
}
