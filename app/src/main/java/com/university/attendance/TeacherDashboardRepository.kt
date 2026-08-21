//package com.university.attendance
//
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.tasks.await
//
//class TeacherDashboardRepository(
//    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
//    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
//) {
//
//    private val teachersRef = firestore.collection("teachers")
//    private val subjectsRef = firestore.collection("subjects")
//    private val studentsRef = firestore.collection("students")
//    private val attendanceRef = firestore.collection("attendance_records")
//
//    data class DashboardData(
//        val teacher: Teacher,
//        val assignedSubjects: List<Subject>,
//        val totalStudents: Int,
//        val attendancePercentage: Int
//    )
//
//    /**
//     * Main dashboard loader.
//     */
//    suspend fun loadDashboard(): DashboardData {
//        val firebaseUser = auth.currentUser
//            ?: throw IllegalStateException("No teacher is currently logged in.")
//
//        val teacher = findTeacher(firebaseUser.uid, firebaseUser.email)
//
//        if (!teacher.isActive) {
//            throw IllegalStateException("Your teacher account has been deactivated.")
//        }
//
//        val subjects = getAssignedSubjects(teacher.teacherId)
//        val totalStudents = getTotalStudents(subjects)
//        val attendancePercentage = getAttendancePercentage(subjects)
//
//        return DashboardData(
//            teacher = teacher,
//            assignedSubjects = subjects,
//            totalStudents = totalStudents,
//            attendancePercentage = attendancePercentage
//        )
//    }
//
//    /**
//     * Finds teacher using Firebase Auth UID first.
//     * Fallback: email
//     */
//    private suspend fun findTeacher(authUid: String, email: String?): Teacher {
//
//        // 1. Best and safest lookup
//        val uidSnapshot = teachersRef
//            .whereEqualTo("authUid", authUid)
//            .limit(1)
//            .get()
//            .await()
//
//        if (!uidSnapshot.isEmpty) {
//            val doc = uidSnapshot.documents.first()
//
//            return doc.toObject(Teacher::class.java)?.apply {
//                teacherId = doc.id
//            } ?: throw IllegalStateException("Teacher profile could not be read.")
//        }
//
//        // 2. Email fallback
//        if (!email.isNullOrBlank()) {
//            val emailSnapshot = teachersRef
//                .whereEqualTo("email", email)
//                .limit(1)
//                .get()
//                .await()
//
//            if (!emailSnapshot.isEmpty) {
//                val doc = emailSnapshot.documents.first()
//
//                doc.reference.update(mapOf("authUid" to authUid)).await()
//
//                return doc.toObject(Teacher::class.java)?.apply {
//                    teacherId = doc.id
//                    this.authUid = authUid
//                    this.email = email
//                } ?: throw IllegalStateException("Teacher profile could not be read.")
//            }
//        }
//
//        throw IllegalStateException("Teacher profile not found. Please contact Admin.")
//    }
//
//    /**
//     * Gets only subjects assigned to this teacher.
//     *
//     * Admin's TeacherSubjectRepository already stores:
//     * teacherId + teacherName on Subject.
//     */
//    private suspend fun getAssignedSubjects(teacherId: String): List<Subject> {
//
//        val snapshot = subjectsRef
//            .whereEqualTo("teacherId", teacherId)
//            .get()
//            .await()
//
//        return snapshot.documents
//            .mapNotNull { doc ->
//                doc.toObject(Subject::class.java)?.apply {
//                    subjectId = doc.id
//                }
//            }
//            .sortedWith(
//                compareBy(
//                    { it.programName },
//                    { it.semester },
//                    { it.courseCode }
//                )
//            )
//    }
//
//    /**
//     * Counts unique students belonging to the programs
//     * taught by this teacher.
//     *
//     * Because Student currently has no semester field,
//     * we intentionally don't pretend that semester-level
//     * filtering is possible here.
//     */
//    private suspend fun getTotalStudents(subjects: List<Subject>): Int {
//
//        val studentIds = mutableSetOf<String>()
//
//        subjects.forEach { subject ->
//            val snapshot = studentsRef
//                .whereEqualTo("departmentName", subject.departmentName)
//                .whereEqualTo("programName", subject.programName)
//                .get()
//                .await()
//
//            snapshot.documents.forEach { doc ->
//                if (doc.getBoolean("isActive") != false) {
//                    studentIds.add(doc.id)
//                }
//            }
//        }
//
//        return studentIds.size
//    }
//
//    /**
//     * Calculates overall attendance percentage for the teacher.
//     *
//     * Only "present" attendance records exist in the current
//     * architecture.
//     *
//     * For each assigned subject:
//     *
//     * attendance =
//     *     present student marks /
//     *     (classes held × enrolled students)
//     *
//     * Classes held are derived from distinct attendance dates.
//     */
//    private suspend fun getAttendancePercentage(subjects: List<Subject>): Int {
//
//        if (subjects.isEmpty()) {
//            return 0
//        }
//
//        var totalPresent = 0
//        var totalPossible = 0
//
//        subjects.forEach { subject ->
//            val studentSnapshot = studentsRef
//                .whereEqualTo("departmentName", subject.departmentName)
//                .whereEqualTo("programName", subject.programName)
//                .get()
//                .await()
//
//            val enrolledStudents = studentSnapshot.documents.count {
//                it.getBoolean("isActive") != false
//            }
//
//            if (enrolledStudents == 0) {
//                return@forEach
//            }
//
//            val attendanceSnapshot = attendanceRef
//                .whereEqualTo("teacherId", subject.teacherId)
//                .whereEqualTo("subjectId", subject.subjectId)
//                .whereEqualTo("status", "present")
//                .get()
//                .await()
//
//            val dates = attendanceSnapshot.documents
//                .mapNotNull { it.getString("date") }
//                .distinct()
//
//            val classesHeld = dates.size
//
//            if (classesHeld > 0) {
//                val presentCount = attendanceSnapshot.size()
//
//                totalPresent += presentCount
//                totalPossible += classesHeld * enrolledStudents
//            }
//        }
//
//        if (totalPossible == 0) {
//            return 0
//        }
//
//        return ((totalPresent * 100L) / totalPossible)
//            .toInt()
//            .coerceIn(0, 100)
//    }
//}
package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherDashboardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val teacherSubjectsRepository: TeacherSubjectsRepository = TeacherSubjectsRepository()
) {

    private val studentsRef = firestore.collection("students")
    private val attendanceRef = firestore.collection("attendance_records")

    data class DashboardData(
        val teacher: Teacher,
        val assignedSubjects: List<Subject>,
        val totalStudents: Int,
        val attendancePercentage: Int
    )

    /**
     * Main dashboard loader.
     *
     * Teacher resolution and subject assignment now live in
     * TeacherSubjectsRepository, shared with My Classes / Class Detail /
     * every other teacher-side screen, so this repository only computes
     * dashboard-specific aggregates.
     */
    suspend fun loadDashboard(): DashboardData {
        val teacher = teacherSubjectsRepository.getCurrentTeacher()
        val subjects = teacherSubjectsRepository.getAssignedSubjects(teacher.teacherId)

        val totalStudents = getTotalStudents(subjects)
        val attendancePercentage = getAttendancePercentage(subjects)

        return DashboardData(
            teacher = teacher,
            assignedSubjects = subjects,
            totalStudents = totalStudents,
            attendancePercentage = attendancePercentage
        )
    }

    /**
     * Counts unique students belonging to the programs
     * taught by this teacher.
     *
     * Because Student currently has no semester field,
     * we intentionally don't pretend that semester-level
     * filtering is possible here.
     */
    private suspend fun getTotalStudents(subjects: List<Subject>): Int {

        val studentIds = mutableSetOf<String>()

        subjects.forEach { subject ->
            val snapshot = studentsRef
                .whereEqualTo("departmentName", subject.departmentName)
                .whereEqualTo("programName", subject.programName)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                if (doc.getBoolean("isActive") != false) {
                    studentIds.add(doc.id)
                }
            }
        }

        return studentIds.size
    }

    /**
     * Calculates overall attendance percentage for the teacher.
     *
     * Only "present" attendance records exist in the current
     * architecture.
     *
     * For each assigned subject:
     *
     * attendance =
     *     present student marks /
     *     (classes held × enrolled students)
     *
     * Classes held are derived from distinct attendance dates.
     */
    private suspend fun getAttendancePercentage(subjects: List<Subject>): Int {

        if (subjects.isEmpty()) {
            return 0
        }

        var totalPresent = 0
        var totalPossible = 0

        subjects.forEach { subject ->
            val studentSnapshot = studentsRef
                .whereEqualTo("departmentName", subject.departmentName)
                .whereEqualTo("programName", subject.programName)
                .get()
                .await()

            val enrolledStudents = studentSnapshot.documents.count {
                it.getBoolean("isActive") != false
            }

            if (enrolledStudents == 0) {
                return@forEach
            }

            val attendanceSnapshot = attendanceRef
                .whereEqualTo("teacherId", subject.teacherId)
                .whereEqualTo("subjectId", subject.subjectId)
                .whereEqualTo("status", "present")
                .get()
                .await()

            val dates = attendanceSnapshot.documents
                .mapNotNull { it.getString("date") }
                .distinct()

            val classesHeld = dates.size

            if (classesHeld > 0) {
                val presentCount = attendanceSnapshot.size()

                totalPresent += presentCount
                totalPossible += classesHeld * enrolledStudents
            }
        }

        if (totalPossible == 0) {
            return 0
        }

        return ((totalPresent * 100L) / totalPossible)
            .toInt()
            .coerceIn(0, 100)
    }
}