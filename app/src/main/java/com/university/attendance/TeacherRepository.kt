package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val teachersRef = firestore.collection("teachers")
    private val classesRef = firestore.collection("classes")

    sealed class SaveResult {
        data class Success(val teacherId: String) : SaveResult()
        data class Error(
            val message: String,
            val exception: Exception? = null
        ) : SaveResult()
    }

    suspend fun addTeacher(teacher: Teacher): SaveResult {

        return try {

            // ---------------------------------------------------------
            // 1. Duplicate CNIC check
            // ---------------------------------------------------------

            val duplicateCnic = teachersRef
                .whereEqualTo(
                    "cnicNumber",
                    teacher.cnicNumber.trim()
                )
                .limit(1)
                .get()
                .await()

            if (!duplicateCnic.isEmpty) {

                return SaveResult.Error(
                    "A teacher with CNIC '${teacher.cnicNumber}' already exists."
                )
            }

            // ---------------------------------------------------------
            // 2. Generate our own Teacher ID
            // ---------------------------------------------------------

            val teacherId = TeacherIdGenerator.generate()

            // ---------------------------------------------------------
            // 3. Create Firestore document using teacherId
            // ---------------------------------------------------------

            val teacherRef = teachersRef
                .document(teacherId)

            // ---------------------------------------------------------
            // 4. Prepare teacher data
            // ---------------------------------------------------------

            teacher.teacherId = teacherId
            teacher.authUid = ""
            teacher.accountLinked = false

            // ---------------------------------------------------------
            // 5. Save teacher
            // ---------------------------------------------------------

            teacherRef
                .set(teacher.toMap())
                .await()

            // ---------------------------------------------------------
            // 6. Activity log
            // ---------------------------------------------------------

            ActivityLogHelper.log(
                type = Type.TEACHER_ADDED,
                title = "Teacher Added",
                description =
                    "${teacher.fullName} (${teacher.designation}) added to ${teacher.departmentName}"
            )

            SaveResult.Success(
                teacherId = teacherId
            )

        } catch (e: Exception) {

            SaveResult.Error(
                e.message ?: "Unknown error occurred while saving teacher.",
                e
            )
        }
    }

    suspend fun getDistinctDepartments(): List<String> {

        val snapshot = classesRef
            .get()
            .await()

        return snapshot.documents
            .mapNotNull {
                it.getString("departmentName")
            }
            .distinct()
            .sorted()
    }

    suspend fun getTeachersByDepartment(
        departmentName: String
    ): List<Teacher> {

        val snapshot = teachersRef
            .whereEqualTo(
                "departmentName",
                departmentName
            )
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(Teacher::class.java)
                    ?.apply {
                        teacherId = doc.id
                    }
            }
            .sortedBy {
                it.fullName
            }
    }

    suspend fun getAllTeachers(): List<Teacher> {

        val snapshot = teachersRef
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(Teacher::class.java)
                    ?.apply {
                        teacherId = doc.id
                    }
            }
            .sortedBy {
                it.fullName
            }
    }

    suspend fun getTeacherById(
        teacherId: String
    ): Teacher? {

        val document = teachersRef
            .document(teacherId)
            .get()
            .await()

        return if (document.exists()) {

            document
                .toObject(Teacher::class.java)
                ?.apply {
                    this.teacherId = document.id
                }

        } else {
            null
        }
    }

    suspend fun getTeacherByAuthUid(
        authUid: String
    ): Teacher? {

        val snapshot = teachersRef
            .whereEqualTo(
                "authUid",
                authUid
            )
            .limit(1)
            .get()
            .await()

        return snapshot.documents
            .firstOrNull()
            ?.let { document ->

                document
                    .toObject(Teacher::class.java)
                    ?.apply {
                        teacherId = document.id
                    }
            }
    }
}