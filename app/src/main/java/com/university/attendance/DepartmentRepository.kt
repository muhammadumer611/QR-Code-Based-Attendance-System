package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firestore CRUD operations for the "departments" collection.
 *
 * Collection: departments/{departmentId}
 *   - name: String
 *   - createdAt: Timestamp
 *
 * Note: As requested, duplicate department names are NOT blocked -- Admin
 * can add "Computer Science" more than once if they choose to.
 */
class DepartmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val departmentsRef = firestore.collection("departments")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String, val exception: Exception? = null) : OpResult()
    }

    // ---------------------------- CREATE ----------------------------

    /** Adds a new department with the given name. */
    suspend fun addDepartment(name: String): OpResult {
        return try {
            val department = Department(name = name.trim())
            departmentsRef.document().set(department.toMap()).await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to add department.", e)
        }
    }

    // ------------------------------ READ ------------------------------

    /**
     * Attaches a real-time listener to the departments collection, ordered
     * alphabetically by name. The onUpdate callback fires immediately with
     * the current list, and again every time data changes on the server.
     *
     * Call remove() on the returned ListenerRegistration (e.g. in
     * onDestroy) to stop listening and avoid memory leaks.
     */
    fun listenToDepartments(
        onUpdate: (List<Department>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return departmentsRef
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val departments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Department::class.java)?.apply { departmentId = doc.id }
                } ?: emptyList()
                onUpdate(departments)
            }
    }

    /** One-time fetch of all departments (non-realtime), useful for dropdowns elsewhere. */
    suspend fun getAllDepartmentsOnce(): List<Department> {
        val snapshot = departmentsRef.orderBy("name").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Department::class.java)?.apply { departmentId = doc.id }
        }
    }

    // ---------------------------- UPDATE ----------------------------

    /** Updates the name of an existing department. */
    suspend fun updateDepartment(departmentId: String, newName: String): OpResult {
        return try {
            departmentsRef.document(departmentId)
                .update("name", newName.trim())
                .await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to update department.", e)
        }
    }

    // ---------------------------- DELETE ----------------------------

    /** Deletes a department. Allowed even if students/teachers reference it, per requirements. */
    suspend fun deleteDepartment(departmentId: String): OpResult {
        return try {
            departmentsRef.document(departmentId).delete().await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to delete department.", e)
        }
    }
}