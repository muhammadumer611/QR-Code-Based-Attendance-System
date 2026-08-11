package com.university.attendance

/**
 * NOT a Firestore model -- this is a computed summary built in-app by
 * combining:
 *   1. A Subject the student is enrolled in (from "subjects" collection,
 *      matched by the student's departmentId + programName + semester...
 *      for now matched by department+program, see AttendanceRepository)
 *   2. That subject's teacher (from "teachers" collection)
 *   3. All "present" AttendanceRecord documents for this student+subject
 *
 * Absent is never stored -- it's calculated here as:
 *   totalClassesHeld - presentCount
 *
 * totalClassesHeld = the number of DISTINCT dates any attendance session
 * existed for this subject+class (i.e. how many times class actually met).
 * If no sessions exist yet (QR system not built yet), totalClassesHeld is
 * 0 and the UI shows "No attendance recorded yet" instead of a misleading
 * 0% bar.
 */
data class SubjectAttendanceSummary(
    val subjectId: String,
    val subjectName: String,
    val courseCode: String,
    val teacherName: String,
    val totalClassesHeld: Int,
    val presentCount: Int,
    val presentDates: List<String>   // yyyy-MM-dd, sorted descending (most recent first)
) {
    val absentCount: Int
        get() = (totalClassesHeld - presentCount).coerceAtLeast(0)

    val percentage: Int
        get() = if (totalClassesHeld == 0) 0 else ((presentCount * 100) / totalClassesHeld)

    val hasAnyData: Boolean
        get() = totalClassesHeld > 0
}