package com.profecuaderno.app.data

/**
 * Contrato v2 del ecosistema El Cuaderno.
 * La app Maestro administra únicamente sus grupos asignados.
 * El backend deberá aplicar estas reglas cuando se habilite la sincronización real.
 */
object EcosystemContract {
    const val PROTOCOL_VERSION = 2

    const val TEACHER_APP_ID = "com.profecuaderno.app"
    const val STUDENT_APP_ID = "com.profecuaderno.student"
    const val DIRECTOR_APP_ID = "com.profecuaderno.director"

    enum class Role { TEACHER, STUDENT, DIRECTOR }
    enum class NoticeSource { TEACHER, DIRECTOR }
    enum class NoticeTarget { STUDENTS, GROUP, TEACHERS, TEACHERS_AND_STUDENTS }

    object TeacherCapabilities {
        const val READ_ASSIGNED_STUDENTS = true
        const val EDIT_ASSIGNED_STUDENT_GRADES = true
        const val EDIT_ASSIGNED_STUDENT_ATTENDANCE = true
        const val MANAGE_ASSIGNED_GROUP_CALENDAR = true
        const val PUBLISH_GROUP_NOTICES = true
        const val READ_DIRECTOR_NOTICES = true
        const val READ_OWN_SCHEDULE = true

        const val READ_UNASSIGNED_STUDENTS = false
        const val MANAGE_INSTITUTION_SCHEDULE = false
        const val READ_OTHER_TEACHERS_STUDENT_DATA = false
    }
}
