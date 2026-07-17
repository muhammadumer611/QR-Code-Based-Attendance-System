package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firestore CRUD operations for the "subjects" collection.
 *
 * Collection: subjects/{subjectId}
 *   - departmentId, departmentName, programName, semester
 *   - courseCode, subjectName, creditHours
 *
 * IMPORTANT FIX (previous bug):
 * The real-time listener previously combined 3 whereEqualTo() filters with
 * an orderBy() on a 4th field. Firestore requires a manually-created
 * "composite index" for that exact combination -- without it, the query
 * fails silently/returns an error that's easy to miss, which is why newly
 * added subjects appeared to save (the Toast fired) but never showed up
 * in the list (the read query was failing in the background).
 *
 * Fix: the real-time query now ONLY uses whereEqualTo() filters (no
 * orderBy), which Firestore can always run without needing any index to
 * be created. Sorting by courseCode is instead done on the client side,
 * in the ViewModel, after the data arrives.
 */
class SubjectRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val subjectsRef = firestore.collection("subjects")
    private val departmentsRef = firestore.collection("departments")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String, val exception: Exception? = null) : OpResult()
    }

    // ---------------------------- CREATE ----------------------------

    suspend fun addSubject(subject: Subject): OpResult {
        return try {
            subjectsRef.document().set(subject.toMap()).await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to add subject.", e)
        }
    }

    // ------------------------------ READ ------------------------------

    /** One-time fetch of all departments, used to populate the Department filter dropdown. */
    suspend fun getAllDepartments(): List<Department> {
        val snapshot = departmentsRef.orderBy("name").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Department::class.java)?.apply { departmentId = doc.id }
        }
    }

    /**
     * Attaches a REAL-TIME listener scoped to one exact
     * Department + Program + Semester combination.
     *
     * NOTE: No orderBy() here on purpose -- see class-level comment above.
     * Sorting is done by the caller (ViewModel) after data arrives.
     *
     * Call remove() on the returned ListenerRegistration when the filter
     * changes or the screen closes, to avoid stacking multiple listeners.
     */
    fun listenToSubjects(
        departmentId: String,
        programName: String,
        semester: Int,
        onUpdate: (List<Subject>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return subjectsRef
            .whereEqualTo("departmentId", departmentId)
            .whereEqualTo("programName", programName)
            .whereEqualTo("semester", semester)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val subjects = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Subject::class.java)?.apply { subjectId = doc.id }
                } ?: emptyList()
                onUpdate(subjects)
            }
    }

    // ---------------------------- UPDATE ----------------------------

    suspend fun updateSubject(
        subjectId: String,
        courseCode: String,
        subjectName: String,
        creditHours: Int
    ): OpResult {
        return try {
            subjectsRef.document(subjectId)
                .update(
                    mapOf(
                        "courseCode" to courseCode.trim(),
                        "subjectName" to subjectName.trim(),
                        "creditHours" to creditHours
                    )
                )
                .await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to update subject.", e)
        }
    }

    // ---------------------------- DELETE ----------------------------

    suspend fun deleteSubject(subjectId: String): OpResult {
        return try {
            subjectsRef.document(subjectId).delete().await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to delete subject.", e)
        }
    }
}