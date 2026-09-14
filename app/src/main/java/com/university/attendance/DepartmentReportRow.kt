package com.university.attendance

data class DepartmentReportRow(
    val departmentName: String,
    val studentCount: Int,
    val todayPresentCount: Int,
    val todayExpectedCount: Int
) {
    val todayAttendancePercentage: Int
        get() = if (todayExpectedCount == 0) 0 else (todayPresentCount * 100) / todayExpectedCount
}