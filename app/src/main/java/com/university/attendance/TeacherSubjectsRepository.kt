package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TeacherSubjectsRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val teachersRef =
        firestore.collection("teachers")

    private val subjectsRef =
        firestore.collection("subjects")

    suspend fun getAllTeachers(): List<Teacher> {

        val snapshot =
            teachersRef
                .orderBy("fullName")
                .get()
                .await()

        return snapshot.documents.mapNotNull { doc ->

            doc.toObject(Teacher::class.java)
                ?.apply {
                    teacherId = doc.id
                }
        }
    }

    suspend fun getSubjectsForTeacher(
        teacherId: String,
        programName: String,
        semester: String
    ): List<Subject> {

        val snapshot =
            subjectsRef
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("programName", programName)
                .whereEqualTo("semester", semester)
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(Subject::class.java)
                    ?.apply {
                        subjectId = doc.id
                    }
            }
            .sortedBy {
                it.courseCode
            }
    }

    suspend fun getAllSubjects(): List<Subject> {

        val snapshot =
            subjectsRef
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(Subject::class.java)
                    ?.apply {
                        subjectId = doc.id
                    }
            }
            .sortedWith(
                compareBy(
                    { it.departmentName },
                    { it.programName },
                    { it.semester },
                    { it.courseCode }
                )
            )
    }

    suspend fun saveAssignment(
        teacherId: String,
        teacherName: String,
        allSubjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): Boolean {

        return try {

            val batch =
                firestore.batch()

            allSubjects.forEach { subject ->

                val wasAssigned =
                    subject.teacherId == teacherId

                val selected =
                    selectedSubjectIds.contains(
                        subject.subjectId
                    )

                when {

                    selected -> {

                        batch.update(
                            subjectsRef.document(
                                subject.subjectId
                            ),
                            mapOf(
                                "teacherId" to teacherId,
                                "teacherName" to teacherName
                            )
                        )
                    }

                    wasAssigned && !selected -> {

                        batch.update(
                            subjectsRef.document(
                                subject.subjectId
                            ),
                            mapOf(
                                "teacherId" to "",
                                "teacherName" to ""
                            )
                        )
                    }
                }
            }

            batch.commit().await()

            true

        } catch (e: Exception) {

            false
        }
    }
}