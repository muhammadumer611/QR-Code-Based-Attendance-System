package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherClassesRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val subjectsRef =
        firestore.collection("subjects")

    private val studentsRef =
        firestore.collection("students")

    // ============================================================
    // GET TEACHER SUBJECTS
    // ============================================================

    suspend fun getTeacherClasses(
        teacherId: String
    ): List<Subject> {

        if (teacherId.isBlank()) {
            return emptyList()
        }

        val snapshot =
            subjectsRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { document ->

                document
                    .toObject(Subject::class.java)
                    ?.apply {

                        subjectId =
                            document.id
                    }
            }
            .sortedWith(
                compareBy<Subject> {

                    it.programName

                }.thenBy {

                    it.semester

                }.thenBy {

                    it.courseCode
                }
            )
    }

    // ============================================================
    // GET STUDENT COUNT FOR SUBJECT
    //
    // Current Student model does not contain semester,
    // therefore department + program are used.
    // ============================================================

    suspend fun getStudentCount(
        subject: Subject
    ): Int {

        val snapshot =
            studentsRef
                .whereEqualTo(
                    "departmentName",
                    subject.departmentName
                )
                .whereEqualTo(
                    "programName",
                    subject.programName
                )
                .get()
                .await()

        return snapshot.documents.count {

            it.getBoolean("isActive") != false
        }
    }

    // ============================================================
    // GET STUDENTS FOR SUBJECT
    // ============================================================

    suspend fun getStudentsForSubject(
        subject: Subject
    ): List<Student> {

        val snapshot =
            studentsRef
                .whereEqualTo(
                    "departmentName",
                    subject.departmentName
                )
                .whereEqualTo(
                    "programName",
                    subject.programName
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { document ->

                document
                    .toObject(Student::class.java)
                    ?.apply {

                        studentId =
                            document.id
                    }
            }
            .filter {

                it.isActive
            }
            .sortedBy {

                it.fullName
            }
    }

    // ============================================================
    // GET SUBJECT BY ID
    // ============================================================

    suspend fun getSubjectById(
        subjectId: String
    ): Subject? {

        if (subjectId.isBlank()) {
            return null
        }

        val document =
            subjectsRef
                .document(subjectId)
                .get()
                .await()

        return document
            .toObject(Subject::class.java)
            ?.apply {

                this.subjectId =
                    document.id
            }
    }
}

