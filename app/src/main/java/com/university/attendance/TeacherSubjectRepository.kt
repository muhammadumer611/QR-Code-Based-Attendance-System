package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Teacher-Subject Assignment screen.
 *
 * Reads/writes:
 *   - "teachers" collection (read only, to list teachers)
 *   - "subjects" collection (read to build the checklist, write to save
 *     which teacherId/teacherName is assigned to which subject)
 *
 * Assignment model: teacherId/teacherName live on the SUBJECT document
 * (see updated Subject.kt). A teacher can be assigned to many subjects;
 * each subject has exactly one teacher. Assigning a new teacher to a
 * subject that already has one simply overwrites it (per your requirement
 * -- no confirmation needed, straightforward reassignment).
 */
class TeacherSubjectRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val teachersRef = firestore.collection("teachers")
    private val subjectsRef = firestore.collection("subjects")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String, val exception: Exception? = null) : OpResult()
    }

    /** Fetches all teachers, for the initial teacher-picker list. */
    suspend fun getAllTeachers(): List<Teacher> {
        val snapshot = teachersRef.orderBy("fullName").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Teacher::class.java)?.apply { teacherId = doc.id }
        }
    }

    /**
     * Fetches ALL subjects in the system (across every department/program),
     * so Admin can assign a teacher to any subject regardless of the
     * teacher's own department -- some institutions have teachers covering
     * subjects outside their home department (e.g. a Math teacher
     * teaching a Statistics course in the CS department).
     */
    suspend fun getAllSubjects(): List<Subject> {
        val snapshot = subjectsRef.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Subject::class.java)?.apply { subjectId = doc.id }
        }.sortedWith(compareBy({ it.departmentName }, { it.programName }, { it.courseCode }))
    }

    /**
     * Saves the assignment for one teacher: every subject the Admin
     * checked gets this teacher's id/name written to it; every subject
     * that was PREVIOUSLY assigned to this teacher but is now UNCHECKED
     * gets cleared back to "unassigned" (teacherId = "").
     *
     * This runs as a single Firestore batch so a half-saved assignment
     * (some subjects updated, others not) can't happen.
     */
    suspend fun saveAssignment(
        teacherId: String,
        teacherName: String,
        allSubjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): OpResult {
        return try {
            val batch = firestore.batch()

            allSubjects.forEach { subject ->
                val wasAssignedToThisTeacher = subject.teacherId == teacherId
                val isNowSelected = selectedSubjectIds.contains(subject.subjectId)

                when {
                    // Newly checked -> assign this teacher (overwrites
                    // whoever was assigned before, per your requirement).
                    isNowSelected -> {
                        batch.update(
                            subjectsRef.document(subject.subjectId),
                            mapOf("teacherId" to teacherId, "teacherName" to teacherName)
                        )
                    }
                    // Was this teacher's, now unchecked -> clear assignment.
                    wasAssignedToThisTeacher && !isNowSelected -> {
                        batch.update(
                            subjectsRef.document(subject.subjectId),
                            mapOf("teacherId" to "", "teacherName" to "")
                        )
                    }
                    // Otherwise (belongs to a different teacher, stays
                    // unchecked) -> leave untouched.
                    else -> Unit
                }
            }

            batch.commit().await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to save assignment.", e)
        }
    }
}