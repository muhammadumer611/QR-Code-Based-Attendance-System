package com.university.attendance

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminTeacherAssignmentRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val teachersRef =
        firestore.collection("teachers")

    private val subjectsRef =
        firestore.collection("subjects")

    private val assignmentsRef =
        firestore.collection("teacherSubjectAssignments")

    sealed class OpResult {

        object Success :
            OpResult()

        data class Error(
            val message: String
        ) : OpResult()
    }

    // ============================================================
    // TEACHERS
    // ============================================================

    suspend fun getAllTeachers():
            List<Teacher> {

        val snapshot =
            teachersRef
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(
                    Teacher::class.java
                )?.apply {

                    teacherId =
                        doc.id
                }
            }
            .sortedBy {
                it.fullName
            }
    }

    // ============================================================
    // SUBJECTS BY SEMESTER
    // ============================================================

    suspend fun getSubjectsBySemester(
        semester: Int
    ): List<Subject> {

        val snapshot =
            subjectsRef
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(
                    Subject::class.java
                )?.apply {

                    subjectId =
                        doc.id
                }
            }
            .filter {
                it.semester == semester
                    .toString()
            }
            .sortedBy {
                it.courseCode
            }
    }

    // ============================================================
    // SELECTED ASSIGNMENTS
    // ============================================================

    suspend fun getAssignedSubjectIds(
        teacherId: String,
        semester: Int,
        session: String
    ): Set<String> {

        if (
            teacherId.isBlank() ||
            session.isBlank()
        ) {
            return emptySet()
        }

        val snapshot =
            assignmentsRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .whereEqualTo(
                    "semester",
                    semester
                )
                .whereEqualTo(
                    "session",
                    session.trim()
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull {
                it.getString(
                    "subjectId"
                )
            }
            .toSet()
    }

    // ============================================================
    // SAVE ASSIGNMENT
    // ============================================================

    suspend fun saveAssignment(
        teacher: Teacher,
        semester: Int,
        session: String,
        subjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): OpResult {

        return try {

            if (teacher.teacherId.isBlank()) {
                return OpResult.Error(
                    "Teacher ID is missing."
                )
            }

            if (session.isBlank()) {
                return OpResult.Error(
                    "Session is required."
                )
            }

            val cleanSession =
                session.trim()

            val oldSnapshot =
                assignmentsRef
                    .whereEqualTo(
                        "teacherId",
                        teacher.teacherId
                    )
                    .whereEqualTo(
                        "semester",
                        semester
                    )
                    .whereEqualTo(
                        "session",
                        cleanSession
                    )
                    .get()
                    .await()

            val oldIds =
                oldSnapshot.documents
                    .mapNotNull {
                        it.getString(
                            "subjectId"
                        )
                    }
                    .toSet()

            val batch =
                firestore.batch()

            // ----------------------------------------------------
            // DELETE OLD
            // ----------------------------------------------------

            oldSnapshot.documents
                .filter {
                    it.getString("subjectId") !in selectedSubjectIds
                }
                .forEach { doc ->

                    batch.delete(
                        doc.reference
                    )
                }

            // ----------------------------------------------------
            // ADD NEW
            // ----------------------------------------------------

            subjects
                .filter {
                    it.subjectId in
                            selectedSubjectIds
                }
                .forEach { subject ->

                    val assignmentId =
                        "${teacher.teacherId}_${cleanSession}_${semester}_${subject.subjectId}"
                            .replace(
                                "/",
                                "_"
                            )

                    val assignmentRef =
                        assignmentsRef
                            .document(
                                assignmentId
                            )

                    val data =
                        mapOf(

                            "teacherId" to
                                    teacher.teacherId,

                            "teacherName" to
                                    teacher.fullName,

                            "subjectId" to
                                    subject.subjectId,

                            "subjectName" to
                                    subject.subjectName,

                            "courseCode" to
                                    subject.courseCode,

                            "programName" to
                                    subject.programName,

                            "departmentName" to
                                    subject.departmentName,

                            "semester" to
                                    semester,

                            "session" to
                                    cleanSession,

                            "updatedAt" to
                                    FieldValue.serverTimestamp()
                        )

                    batch.set(
                        assignmentRef,
                        data
                    )

                    // ------------------------------------------------
                    // Keep old subject.teacherId compatible
                    // ------------------------------------------------

                    batch.update(
                        subjectsRef.document(
                            subject.subjectId
                        ),
                        mapOf(
                            "teacherId" to
                                    teacher.teacherId,

                            "teacherName" to
                                    teacher.fullName
                        )
                    )
                }

            batch.commit()
                .await()

            OpResult.Success

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save teacher subject assignment."
            )
        }
    }
}