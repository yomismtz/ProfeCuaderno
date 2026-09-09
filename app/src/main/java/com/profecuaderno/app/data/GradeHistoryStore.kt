package com.profecuaderno.app.data

data class GradeHistoryEntry(
    val id: Long,
    val periodId: Long,
    val studentId: Long,
    val categoryId: Long,
    val studentName: String,
    val categoryName: String,
    val oldScore: Double?,
    val newScore: Double,
    val changedAt: Long
)

object GradeHistoryStore {
    private const val TABLE = "grade_history"
    private const val PENDING = "grade_history_pending"

    fun ensure(db: TeacherDbHelper) {
        val sqlDb = db.writableDatabase
        sqlDb.execSQL("PRAGMA recursive_triggers=ON")
        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                period_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                student_name TEXT NOT NULL DEFAULT '',
                category_name TEXT NOT NULL DEFAULT '',
                old_score REAL,
                new_score REAL NOT NULL,
                changed_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        sqlDb.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $PENDING(
                student_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                old_score REAL NOT NULL,
                PRIMARY KEY(student_id, category_id)
            )
            """.trimIndent()
        )

        sqlDb.execSQL("DROP TRIGGER IF EXISTS trg_grade_history_before_delete")
        sqlDb.execSQL("DROP TRIGGER IF EXISTS trg_grade_history_insert")
        sqlDb.execSQL("DROP TRIGGER IF EXISTS trg_grade_history_update")

        // Existing code stores grades using SQLite CONFLICT_REPLACE. With recursive
        // triggers enabled, REPLACE deletes the old row before inserting the new
        // one. Save that old value temporarily so the following INSERT can log
        // an actual old -> new change instead of losing the previous score.
        sqlDb.execSQL(
            """
            CREATE TRIGGER trg_grade_history_before_delete
            BEFORE DELETE ON grades
            BEGIN
                INSERT OR REPLACE INTO $PENDING(student_id, category_id, old_score)
                VALUES(OLD.student_id, OLD.category_id, OLD.score);
            END
            """.trimIndent()
        )
        sqlDb.execSQL(
            """
            CREATE TRIGGER trg_grade_history_insert
            AFTER INSERT ON grades
            BEGIN
                INSERT INTO $TABLE(period_id, student_id, category_id, student_name, category_name, old_score, new_score, changed_at)
                SELECT
                    NEW.period_id,
                    NEW.student_id,
                    NEW.category_id,
                    COALESCE((SELECT name FROM students WHERE id=NEW.student_id), ''),
                    COALESCE((SELECT name FROM evaluation_categories WHERE id=NEW.category_id), ''),
                    (SELECT old_score FROM $PENDING WHERE student_id=NEW.student_id AND category_id=NEW.category_id),
                    NEW.score,
                    CAST(strftime('%s','now') AS INTEGER) * 1000
                WHERE
                    (SELECT old_score FROM $PENDING WHERE student_id=NEW.student_id AND category_id=NEW.category_id) IS NULL
                    OR ABS(NEW.score - (SELECT old_score FROM $PENDING WHERE student_id=NEW.student_id AND category_id=NEW.category_id)) > 0.0001;

                DELETE FROM $PENDING
                WHERE student_id=NEW.student_id AND category_id=NEW.category_id;
            END
            """.trimIndent()
        )
        sqlDb.execSQL(
            """
            CREATE TRIGGER trg_grade_history_update
            AFTER UPDATE OF score ON grades
            WHEN ABS(NEW.score - OLD.score) > 0.0001
            BEGIN
                INSERT INTO $TABLE(period_id, student_id, category_id, student_name, category_name, old_score, new_score, changed_at)
                VALUES(
                    NEW.period_id,
                    NEW.student_id,
                    NEW.category_id,
                    COALESCE((SELECT name FROM students WHERE id=NEW.student_id), ''),
                    COALESCE((SELECT name FROM evaluation_categories WHERE id=NEW.category_id), ''),
                    OLD.score,
                    NEW.score,
                    CAST(strftime('%s','now') AS INTEGER) * 1000
                );
            END
            """.trimIndent()
        )
    }

    fun entries(db: TeacherDbHelper, limit: Int = 300): List<GradeHistoryEntry> {
        ensure(db)
        val out = mutableListOf<GradeHistoryEntry>()
        db.readableDatabase.query(TABLE, null, null, null, null, null, "changed_at DESC, id DESC", limit.toString()).use { c ->
            while (c.moveToNext()) out += GradeHistoryEntry(
                id = c.getLong(c.getColumnIndexOrThrow("id")),
                periodId = c.getLong(c.getColumnIndexOrThrow("period_id")),
                studentId = c.getLong(c.getColumnIndexOrThrow("student_id")),
                categoryId = c.getLong(c.getColumnIndexOrThrow("category_id")),
                studentName = c.getString(c.getColumnIndexOrThrow("student_name")),
                categoryName = c.getString(c.getColumnIndexOrThrow("category_name")),
                oldScore = if (c.isNull(c.getColumnIndexOrThrow("old_score"))) null else c.getDouble(c.getColumnIndexOrThrow("old_score")),
                newScore = c.getDouble(c.getColumnIndexOrThrow("new_score")),
                changedAt = c.getLong(c.getColumnIndexOrThrow("changed_at"))
            )
        }
        return out
    }

    fun restorePrevious(db: TeacherDbHelper, entry: GradeHistoryEntry): Boolean {
        val previous = entry.oldScore ?: return false
        db.setGrade(entry.periodId, entry.studentId, entry.categoryId, previous)
        return true
    }

    fun clear(db: TeacherDbHelper) {
        ensure(db)
        db.writableDatabase.delete(TABLE, null, null)
        db.writableDatabase.delete(PENDING, null, null)
    }
}
