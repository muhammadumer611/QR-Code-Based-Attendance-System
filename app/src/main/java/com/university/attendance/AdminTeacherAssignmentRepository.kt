package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * ADMIN-ONLY repository for the Teacher-Subject Assignment screen.
 *
 * This is intentionally a separate class from TeacherSubjectsRepository
 * (the read-only, teacher-scoped repository used by the Teacher Dashboard /
 * My Classes screens). Keeping them separate means a teacher-side ViewModel
 * can never accidentally get access to getAllTeachers(), getAllSubjects(),
 * or saveAssignment() -- those are Admin operations only.
 */
class AdminTeacherAssignmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val teachersRef = firestore.collection("teachers")
    private val subjectsRef = firestore.collection("subjects")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String) : OpResult()
    }

    /**
     * All teachers, for the "pick a teacher" list.
     * Sorted by name so the list is stable and easy to scan.
     */
    suspend fun getAllTeachers(): List<Teacher> {
        val snapshot = teachersRef.get().await()

        return snapshot.documents
            .mapNotNull { doc ->
                doc.toObject(Teacher::class.java)?.apply {
                    teacherId = doc.id
                }
            }
            .sortedBy { it.fullName }
    }

    /**
     * Every subject in the system (not filtered by teacher), for the
     * assignment checklist. Same sort order as TeacherSubjectsRepository
     * so the two screens feel consistent.
     */
    suspend fun getAllSubjects(): List<Subject> {
        val snapshot = subjectsRef.get().await()

        return snapshot.documents
            .mapNotNull { doc ->
                doc.toObject(Subject::class.java)?.apply {
                    subjectId = doc.id
                }
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.semester },
                    { it.courseCode }
                )
            )
    }

    /**
     * Applies the admin's checklist selection as a single atomic batch write:
     *
     *  - Subjects that are checked AND not already assigned to this teacher
     *    -> get teacherId/teacherName set to this teacher.
     *  - Subjects that are unchecked AND currently assigned to this teacher
     *    -> get teacherId/teacherName cleared (unassigned).
     *  - Everything else is left untouched.
     *
     * Only the "teacherId" and "teacherName" fields are written, so this
     * never overwrites any other field on Subject, known or unknown.
     *
     * Note: checking a subject that's currently assigned to a DIFFERENT
     * teacher will reassign it to this teacher -- a subject can only belong
     * to one teacher at a time in the current data model.
     */
    suspend fun saveAssignment(
        teacherId: String,
        teacherName: String,
        allSubjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): OpResult {

        return try {
            val batch = firestore.batch()
            var hasChanges = false

            allSubjects.forEach { subject ->
                val isSelected = subject.subjectId in selectedSubjectIds
                val isCurrentlyAssignedToThisTeacher = subject.teacherId == teacherId

                if (isSelected && !isCurrentlyAssignedToThisTeacher) {
                    batch.update(
                        subjectsRef.document(subject.subjectId),
                        mapOf(
                            "teacherId" to teacherId,
                            "teacherName" to teacherName
                        )
                    )
                    hasChanges = true
                } else if (!isSelected && isCurrentlyAssignedToThisTeacher) {
                    batch.update(
                        subjectsRef.document(subject.subjectId),
                        mapOf(
                            "teacherId" to "",
                            "teacherName" to ""
                        )
                    )
                    hasChanges = true
                }
            }

            if (hasChanges) {
                batch.commit().await()
            }

            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to save subject assignment.")
        }
    }
}