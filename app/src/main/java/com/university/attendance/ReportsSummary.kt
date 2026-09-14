package com.university.attendance

/**
 * NOT a Firestore model -- computed in-app for the Reports screen by
 * combining counts from existing collections plus today's attendance.
 */
data class ReportsSummary(
    val totalStudents: Int,
    val totalTeachers: Int,
    val totalDepartments: Int,
    val totalSubjects: Int,
    val todayPresentCount: Int,
    val todayExpectedCount: Int
) {
    val todayAttendancePercentage: Int
        get() = if (todayExpectedCount == 0) 0 else (todayPresentCount * 100) / todayExpectedCount
}