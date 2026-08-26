package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Admin Attendance flow:
 *   Department -> Class -> Student -> Subject-wise Attendance
 *
 * Reuses existing collections (departments, classes, students, subjects,
 * teachers) and reads from "attendance_records" for actual present marks.
 * Absent is never read from Firestore -- it's calculated, per the
 * architecture plan.
 *
 * UPDATED: Subject.teacherId/teacherName (set via the Teacher-Subject
 * Assignment screen) is now used directly for "Taught by" -- the earlier
 * placeholder that guessed the first teacher found in the department has
 * been removed, since a university has a distinct teacher per subject,
 * not one teacher per department.
 */
class AttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val departmentsRef = firestore.collection("departments")
    private val classesRef = firestore.collection("classes")
    private val studentsRef = firestore.collection("students")
    private val subjectsRef = firestore.collection("subjects")
    private val attendanceRef = firestore.collection("attendance_records")

    // ---------------------- Screen 1: Departments ----------------------

    suspend fun getAllDepartments(): List<Department> {
        val snapshot = departmentsRef.orderBy("name").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Department::class.java)?.apply { departmentId = doc.id }
        }
    }

    // ------------------------ Screen 2: Classes -------------------------

    /** Fetches all classes belonging to a department, so Admin can drill into one section. */
    suspend fun getClassesByDepartment(departmentName: String): List<StudentClass> {
        val snapshot = classesRef
            .whereEqualTo("departmentName", departmentName)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(StudentClass::class.java)?.apply { classId = doc.id }
        }.sortedWith(compareBy({ it.programName }, { it.session }, { it.section }))
    }

    // ------------------------ Screen 3: Students -------------------------

    /** Reuses the same lookup Student Management already relies on. */
    suspend fun getStudentsByClass(classId: String): List<Student> {
        val snapshot = studentsRef
            .whereEqualTo("classId", classId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Student::class.java)?.apply { studentId = doc.id }
        }.sortedBy { it.fullName }
    }

    // ------------------ Screen 4: Subject-wise attendance ------------------

    /**
     * Builds the full subject-wise attendance summary for one student:
     *   1. Find every subject offered for this student's department+program.
     *   2. Read the teacher DIRECTLY from that subject's teacherName field
     *      (set via Teacher-Subject Assignment) -- shows "Not assigned"
     *      if no teacher has been assigned to that subject yet.
     *   3. Pull this student's "present" attendance_records for that subject.
     *   4. Determine totalClassesHeld = distinct dates ANY student in this
     *      class has a present record for that subject.
     */
    suspend fun getSubjectWiseAttendance(student: Student): List<SubjectAttendanceSummary> {
        val subjectsSnapshot = subjectsRef
            .whereEqualTo("departmentId", getDepartmentIdByName(student.departmentName))
            .whereEqualTo("programName", student.programName)
            .get()
            .await()

        val subjects = subjectsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Subject::class.java)?.apply { subjectId = doc.id }
        }

        return subjects.map { subject ->
            val teacherName = subject.teacherName.ifBlank { "Not assigned" }

            val studentRecordsSnapshot = attendanceRef
                .whereEqualTo("studentId", student.studentId)
                .whereEqualTo("subjectId", subject.subjectId)
                .get()
                .await()
            val presentDates = studentRecordsSnapshot.documents
                .mapNotNull { it.getString("date") }
                .sortedDescending()

            val classSessionsSnapshot = attendanceRef
                .whereEqualTo("classId", student.classId)
                .whereEqualTo("subjectId", subject.subjectId)
                .get()
                .await()
            val totalClassesHeld = classSessionsSnapshot.documents
                .mapNotNull { it.getString("date") }
                .distinct()
                .size

            SubjectAttendanceSummary(
                subjectId = subject.subjectId,
                subjectName = subject.subjectName,
                courseCode = subject.courseCode,
                teacherName = teacherName,
                totalClassesHeld = totalClassesHeld,
                presentCount = presentDates.size,
                presentDates = presentDates
            )
        }.sortedBy { it.courseCode }
    }

    // ---------------------------- Helpers ----------------------------

    private suspend fun getDepartmentIdByName(departmentName: String): String {
        val snapshot = departmentsRef
            .whereEqualTo("name", departmentName)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id ?: ""
    }
}