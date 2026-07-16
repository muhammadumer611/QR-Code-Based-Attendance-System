package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firestore read/write operations related to Teachers.
 *
 * Collection used: "teachers" -- one document per teacher.
 *
 * Department options shown in the Add Teacher form come from the existing
 * "classes" collection (the same one StudentRepository writes to), so the
 * department list always matches what's actually used by students/classes
 * instead of being a hand-typed, possibly-inconsistent value.
 */
class TeacherRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val teachersRef = firestore.collection("teachers")
    private val classesRef = firestore.collection("classes")

    sealed class SaveResult {
        data class Success(val teacherId: String) : SaveResult()
        data class Error(val message: String, val exception: Exception? = null) : SaveResult()
    }

    /**
     * Adds a new teacher to Firestore.
     *
     * Before saving, checks whether a teacher with the same CNIC already
     * exists -- CNIC is a unique national identifier, so this prevents the
     * same person being registered twice by mistake.
     */
    suspend fun addTeacher(teacher: Teacher): SaveResult {
        return try {
            val duplicateCnic = teachersRef
                .whereEqualTo("cnicNumber", teacher.cnicNumber.trim())
                .limit(1)
                .get()
                .await()

            if (!duplicateCnic.isEmpty) {
                return SaveResult.Error("A teacher with CNIC '${teacher.cnicNumber}' already exists.")
            }

            val newTeacherRef = teachersRef.document() // auto-generated ID
            newTeacherRef.set(teacher.toMap()).await()

            SaveResult.Success(teacherId = newTeacherRef.id)
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Unknown error occurred while saving teacher.", e)
        }
    }

    /**
     * Fetches the distinct list of department names already used across
     * existing classes, so the Add Teacher form's department dropdown always
     * reflects real departments (kept consistent with Student data).
     */
    suspend fun getDistinctDepartments(): List<String> {
        val snapshot = classesRef.get().await()
        return snapshot.documents
            .mapNotNull { it.getString("departmentName") }
            .distinct()
            .sorted()
    }

    /** Fetches all teachers belonging to a specific department. */
    suspend fun getTeachersByDepartment(departmentName: String): List<Teacher> {
        val snapshot = teachersRef
            .whereEqualTo("departmentName", departmentName)
            .orderBy("fullName")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Teacher::class.java)?.apply { teacherId = doc.id }
        }
    }

    /** Fetches all teachers, useful for an Admin "All Teachers" overview screen. */
    suspend fun getAllTeachers(): List<Teacher> {
        val snapshot = teachersRef.orderBy("fullName").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Teacher::class.java)?.apply { teacherId = doc.id }
        }
    }
}