package com.university.attendance

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.university.attendance.Student
import com.university.attendance.StudentClass
import com.university.attendance.ClassUtils
import kotlinx.coroutines.tasks.await

/**
 * Handles all Firestore read/write operations related to Students and Classes.
 *
 * Collections used:
 *  - "students" : one document per student
 *  - "classes"  : one document per unique (university+department+program+session+section)
 *
 * The class-grouping logic (creating a new class OR incrementing studentCount
 * on an existing one) is done inside a Firestore TRANSACTION. This is
 * important: if two students are added at nearly the same time for the same
 * new class, a transaction guarantees we don't accidentally create two
 * separate class documents or lose a count increment (a "race condition").
 */
class StudentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val studentsRef = firestore.collection("students")
    private val classesRef = firestore.collection("classes")

    /**
     * Result wrapper so the UI layer can distinguish success/failure without
     * needing to catch exceptions itself.
     */
    sealed class SaveResult {
        data class Success(val studentId: String, val classId: String) : SaveResult()
        data class Error(val message: String, val exception: Exception? = null) : SaveResult()
    }

    /**
     * Adds a new student, and atomically creates or updates the matching
     * class document.
     *
     * Steps performed inside a single Firestore transaction:
     *  1. Build classId from the 5 grouping fields.
     *  2. Check if classes/{classId} exists.
     *     - If not, create it with studentCount = 1.
     *     - If yes, increment studentCount by 1.
     *  3. Create the new students/{auto-id} document with classId attached.
     *
     * All of this either fully succeeds or fully fails together -- so we
     * never end up with a student saved but the class count not updated,
     * or vice versa.
     */
    suspend fun addStudent(student: Student): SaveResult {
        return try {
            val classId = ClassUtils.buildClassId(
                universityName = student.universityName,
                departmentName = student.departmentName,
                programName = student.programName,
                session = student.session,
                section = student.section
            )

            // Pre-check for duplicate RegNo before opening the transaction,
            // since Firestore transactions require all reads before writes
            // and we want a clear, specific error message for this case.
            val duplicateRegNo = studentsRef
                .whereEqualTo("regNo", student.regNo.trim())
                .limit(1)
                .get()
                .await()

            if (!duplicateRegNo.isEmpty) {
                return SaveResult.Error("A student with Registration No. '${student.regNo}' already exists.")
            }

            val newStudentRef = studentsRef.document() // auto-generated ID
            val classRef = classesRef.document(classId)

            firestore.runTransaction { transaction ->
                val classSnapshot = transaction.get(classRef)

                if (classSnapshot.exists()) {
                    // Class already exists -> just bump the count
                    transaction.update(classRef, "studentCount", FieldValue.increment(1))
                } else {
                    // Brand new class -> create it with count = 1
                    val newClass = StudentClass(
                        universityName = student.universityName.trim(),
                        departmentName = student.departmentName.trim(),
                        programName = student.programName.trim(),
                        session = student.session.trim(),
                        section = student.section.trim(),
                        studentCount = 1
                    )
                    transaction.set(classRef, newClass.toMap())
                }

                // Attach the resolved classId to the student before saving
                val studentWithClassId = student.copy(classId = classId)
                transaction.set(newStudentRef, studentWithClassId.toMap())

                null // transaction lambda must return a value; not used here
            }.await()

            SaveResult.Success(studentId = newStudentRef.id, classId = classId)
        } catch (e: Exception) {
            SaveResult.Error(e.message ?: "Unknown error occurred while saving student.", e)
        }
    }

    /** Fetches all students belonging to a specific class, ordered by name. */
    suspend fun getStudentsByClass(classId: String): List<Student> {
        val snapshot = studentsRef
            .whereEqualTo("classId", classId)
            .orderBy("fullName")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Student::class.java)?.apply { studentId = doc.id }
        }
    }

    /** Fetches all class documents, useful for an Admin "All Classes" overview screen. */
    suspend fun getAllClasses(): List<StudentClass> {
        val snapshot = classesRef.orderBy("universityName").get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(StudentClass::class.java)?.apply { classId = doc.id }
        }
    }

    /** Fetches distinct list of existing universities, used to populate the dropdown. */
    suspend fun getDistinctUniversities(): List<String> {
        val snapshot = classesRef.get().await()
        return snapshot.documents
            .mapNotNull { it.getString("universityName") }
            .distinct()
            .sorted()
    }
}