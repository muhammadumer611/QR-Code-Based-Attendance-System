package com.university.attendance

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudentRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentsRef =
        firestore.collection("students")

    private val classesRef =
        firestore.collection("classes")

    sealed class SaveResult {

        data class Success(
            val studentId: String,
            val generatedStudentId: String,
            val classId: String
        ) : SaveResult()

        data class Error(
            val message: String,
            val exception: Exception? = null
        ) : SaveResult()
    }

    // ============================================================
    // ADD STUDENT
    // ============================================================

    suspend fun addStudent(
        student: Student
    ): SaveResult {

        return try {

            val program =
                student.programName
                    .trim()
                    .uppercase()

            val section =
                student.section
                    .trim()
                    .uppercase()

            val session =
                student.session
                    .trim()

            val semester =
                student.semester

            // ----------------------------------------------------
            // CLASS ID
            // ----------------------------------------------------

            val classId =
                ClassUtils.buildClassId(
                    universityName = student.universityName,
                    departmentName = student.departmentName,
                    programName = program,
                    session = session,
                    section = section
                )

            // ----------------------------------------------------
            // DUPLICATE REGISTRATION
            // ----------------------------------------------------

            if (student.regNo.isNotBlank()) {

                val duplicate =
                    studentsRef
                        .whereEqualTo(
                            "regNo",
                            student.regNo.trim()
                        )
                        .limit(1)
                        .get()
                        .await()

                if (!duplicate.isEmpty) {

                    return SaveResult.Error(
                        "A student with Registration No. '${student.regNo}' already exists."
                    )
                }
            }

            // ----------------------------------------------------
            // STUDENT NUMBER
            //
            // Example:
            // BSSE-B2022-001
            //
            // ----------------------------------------------------

            val studentPrefix =
                "${program}-${section}${session}-"

            val existingStudents =
                studentsRef
                    .whereEqualTo(
                        "programName",
                        program
                    )
                    .whereEqualTo(
                        "section",
                        section
                    )
                    .whereEqualTo(
                        "session",
                        session
                    )
                    .get()
                    .await()

            var maxNumber = 0

            existingStudents.documents.forEach { doc ->

                val oldId =
                    doc.getString(
                        "studentGeneratedId"
                    )
                        ?: return@forEach

                if (
                    oldId.startsWith(
                        studentPrefix
                    )
                ) {

                    val number =
                        oldId
                            .removePrefix(
                                studentPrefix
                            )
                            .toIntOrNull()
                            ?: 0

                    if (number > maxNumber) {
                        maxNumber = number
                    }
                }
            }

            val nextNumber =
                maxNumber + 1

            val generatedStudentId =
                "$studentPrefix${nextNumber.toString().padStart(3, '0')}"

            // ----------------------------------------------------
            // NEW STUDENT DOC
            // ----------------------------------------------------

            val newStudentRef =
                studentsRef.document()

            val classRef =
                classesRef.document(
                    classId
                )

            // ----------------------------------------------------
            // TRANSACTION
            // ----------------------------------------------------

            firestore.runTransaction { transaction ->

                val classSnapshot =
                    transaction.get(
                        classRef
                    )

                if (classSnapshot.exists()) {

                    transaction.update(
                        classRef,
                        "studentCount",
                        FieldValue.increment(1)
                    )

                } else {

                    val newClass =
                        StudentClass(

                            universityName =
                                student.universityName.trim(),

                            departmentName =
                                student.departmentName.trim(),

                            programName =
                                program,

                            session =
                                session,

                            section =
                                section,

                            studentCount =
                                1
                        )

                    transaction.set(
                        classRef,
                        newClass.toMap()
                    )
                }

                val finalStudent =
                    student.copy(

                        programName =
                            program,

                        session =
                            session,

                        section =
                            section,

                        semester =
                            semester,

                        classId =
                            classId,

                        studentGeneratedId =
                            generatedStudentId
                    )

                transaction.set(
                    newStudentRef,
                    finalStudent.toMap()
                )

                null
            }.await()

            SaveResult.Success(
                studentId =
                    newStudentRef.id,

                generatedStudentId =
                    generatedStudentId,

                classId =
                    classId
            )

        } catch (e: Exception) {

            SaveResult.Error(
                message =
                    e.message
                        ?: "Failed to save student.",

                exception =
                    e
            )
        }
    }

    // ============================================================
    // STUDENTS BY CLASS
    // ============================================================

    suspend fun getStudentsByClass(
        classId: String
    ): List<Student> {

        if (classId.isBlank()) {
            return emptyList()
        }

        val snapshot =
            studentsRef
                .whereEqualTo(
                    "classId",
                    classId
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(
                    Student::class.java
                )?.apply {

                    studentId =
                        doc.id
                }
            }
            .sortedBy {
                it.fullName.lowercase()
            }
    }

    // ============================================================
    // ALL CLASSES
    // ============================================================

    suspend fun getAllClasses():
            List<StudentClass> {

        val snapshot =
            classesRef
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(
                    StudentClass::class.java
                )?.apply {

                    classId =
                        doc.id
                }
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.session },
                    { it.section }
                )
            )
    }
}