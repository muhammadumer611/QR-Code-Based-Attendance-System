package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClassManagementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val classesRef = firestore.collection("classes")

    sealed class Result {
        data class Success(val classId: String) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun createOrUpdateClass(
        universityName: String,
        departmentName: String,
        programName: String,
        semester: Int,
        session: String,
        section: String
    ): Result {
        if (universityName.isBlank() || departmentName.isBlank() || programName.isBlank() || session.isBlank() || section.isBlank()) {
            return Result.Error("Please fill all class fields.")
        }
        if (semester !in 1..8) return Result.Error("Semester must be between 1 and 8.")
        val classId = ClassUtils.buildClassId(universityName, departmentName, programName, session, section, semester)
        if (classId.isBlank()) return Result.Error("Unable to create a valid class ID.")
        return try {
            val ref = classesRef.document(classId)
            val existing = ref.get().await()
            val existingCount = existing.getLong("studentCount") ?: 0L
            ref.set(
                mapOf(
                    "universityName" to universityName.trim(),
                    "departmentName" to departmentName.trim(),
                    "programName" to programName.trim(),
                    "semester" to semester,
                    "session" to session.trim(),
                    "section" to section.trim(),
                    "studentCount" to existingCount,
                    "createdAt" to (existing.getTimestamp("createdAt") ?: com.google.firebase.firestore.FieldValue.serverTimestamp())
                )
            ).await()
            Result.Success(classId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to save class.")
        }
    }

    suspend fun getClasses(): List<StudentClass> =
        classesRef.get().await().documents.mapNotNull { doc ->
            doc.toObject(StudentClass::class.java)?.apply { classId = doc.id }
        }.sortedWith(compareBy({ it.departmentName }, { it.programName }, { it.semester }, { it.section }))
}
