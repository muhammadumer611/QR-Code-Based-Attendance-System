package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Powers the Reports dashboard: overall counts (students, teachers,
 * departments, subjects) plus today's attendance percentage overall and
 * broken down per department.
 *
 * "Today's attendance %" here means: of all students who have AT LEAST
 * ONE present record marked for today (any subject), how many are they
 * out of the total student count. This mirrors the definition already
 * used by the "Attendance Analytics" card on the main dashboard (today's
 * overview), just computed from real data instead of the hardcoded
 * placeholder numbers currently in that XML.
 */
class ReportsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val studentsRef = firestore.collection("students")
    private val teachersRef = firestore.collection("teachers")
    private val departmentsRef = firestore.collection("departments")
    private val subjectsRef = firestore.collection("subjects")
    private val attendanceRef = firestore.collection("attendance_records")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun getReportsSummary(): ReportsSummary {
        val today = dateFormat.format(Date())

        val studentsSnapshot = studentsRef.get().await()
        val teachersSnapshot = teachersRef.get().await()
        val departmentsSnapshot = departmentsRef.get().await()
        val subjectsSnapshot = subjectsRef.get().await()
        val todayAttendanceSnapshot = attendanceRef.whereEqualTo("date", today).get().await()

        val totalStudents = studentsSnapshot.size()
        val distinctPresentStudentIdsToday = todayAttendanceSnapshot.documents
            .mapNotNull { it.getString("studentId") }
            .distinct()
            .size

        return ReportsSummary(
            totalStudents = totalStudents,
            totalTeachers = teachersSnapshot.size(),
            totalDepartments = departmentsSnapshot.size(),
            totalSubjects = subjectsSnapshot.size(),
            todayPresentCount = distinctPresentStudentIdsToday,
            todayExpectedCount = totalStudents
        )
    }

    /**
     * Builds a per-department breakdown of today's attendance:
     *   For each department -> student count in that department, and how
     *   many of those students have at least one present record today.
     */
    suspend fun getDepartmentBreakdown(): List<DepartmentReportRow> {
        val today = dateFormat.format(Date())

        val departmentsSnapshot = departmentsRef.orderBy("name").get().await()
        val departments = departmentsSnapshot.documents.mapNotNull { it.getString("name") }

        val studentsSnapshot = studentsRef.get().await()
        val students = studentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Student::class.java)?.apply { studentId = doc.id }
        }

        val todayAttendanceSnapshot = attendanceRef.whereEqualTo("date", today).get().await()
        val presentStudentIdsToday = todayAttendanceSnapshot.documents
            .mapNotNull { it.getString("studentId") }
            .toSet()

        return departments.map { deptName ->
            val studentsInDept = students.filter { it.departmentName == deptName }
            val presentInDept = studentsInDept.count { presentStudentIdsToday.contains(it.studentId) }

            DepartmentReportRow(
                departmentName = deptName,
                studentCount = studentsInDept.size,
                todayPresentCount = presentInDept,
                todayExpectedCount = studentsInDept.size
            )
        }
    }
}