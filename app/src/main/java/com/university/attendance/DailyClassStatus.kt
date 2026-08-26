package com.university.attendance

/**
 * NOT a Firestore model -- computed in-app for the Daily Overview screen.
 * Represents ONE subject, for one class, on one selected date, combining:
 *   - The subject/class it belongs to (from "subjects" + "classes")
 *   - Whether attendance was actually marked for it that day (from
 *     "attendance_records")
 *
 * wasMarked = false means no attendance_records exist for this exact
 * classId + subjectId + date combination -- i.e. this class was NOT
 * taken/recorded that day (a "Not Marked" gap Admin can immediately spot).
 */
data class DailyClassStatus(
    val departmentName: String,
    val classId: String,
    val classTitle: String,       // e.g. "BSSE - Section A"
    val session: String,
    val subjectId: String,
    val subjectName: String,
    val courseCode: String,
    val wasMarked: Boolean,
    val presentCount: Int,
    val totalStudents: Int
) {
    val absentCount: Int
        get() = (totalStudents - presentCount).coerceAtLeast(0)
}



