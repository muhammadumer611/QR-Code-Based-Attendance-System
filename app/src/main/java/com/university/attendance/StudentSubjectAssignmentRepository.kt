package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Admin source of truth for Student -> Subject enrollment. */
class StudentSubjectAssignmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val studentsRef = firestore.collection("students")
    private val subjectsRef = firestore.collection("subjects")
    private val studentAssignmentsRef =
        firestore.collection("studentSubjectAssignments")

    sealed class Result {
        data class Success(val count: Int) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun getStudents(): List<Student> =
        studentsRef.get().await().documents.mapNotNull { d ->
            d.toObject(Student::class.java)?.apply {
                studentId = d.id
            }
        }
            .filter {
                it.studentId.isNotBlank() &&
                        it.fullName.isNotBlank() &&
                        it.isActive &&
                        it.classId.isNotBlank()
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.semester },
                    { it.session },
                    { it.section },
                    { it.fullName }
                )
            )

    /**
     * Gets subjects directly from Subject Management.
     *
     * Exact filter:
     * Department + Program + Semester
     *
     * Teacher assignment is NOT required here.
     */
    suspend fun getSubjectsForStudent(student: Student): List<Subject> {

        if (
            student.departmentName.isBlank() ||
            student.programName.isBlank()
        ) {
            return emptyList()
        }

        return subjectsRef.get().await().documents.mapNotNull { d ->

            val subjectSemester =
                numberField(d.get("semester"))
                    ?: return@mapNotNull null

            if (subjectSemester != student.semester) {
                return@mapNotNull null
            }

            val department =
                d.getString("departmentName")
                    .orEmpty()
                    .trim()

            val program =
                d.getString("programName")
                    .orEmpty()
                    .trim()

            if (
                !department.equals(
                    student.departmentName.trim(),
                    true
                )
            ) {
                return@mapNotNull null
            }

            if (
                !program.equals(
                    student.programName.trim(),
                    true
                )
            ) {
                return@mapNotNull null
            }

            Subject(
                subjectId = d.id,
                departmentId =
                    d.getString("departmentId").orEmpty(),

                departmentName =
                    department,

                programName =
                    program,

                semester =
                    subjectSemester.toString(),

                courseCode =
                    d.getString("courseCode").orEmpty(),

                subjectName =
                    d.getString("subjectName").orEmpty(),

                creditHours =
                    when (val value = d.get("creditHours")) {
                        is Number -> value.toString()
                        is String -> value
                        else -> ""
                    }
            )

        }.sortedWith(
            compareBy(
                { it.courseCode },
                { it.subjectName }
            )
        )
    }

    suspend fun getAssignedSubjectIds(
        studentId: String
    ): Set<String> {

        if (studentId.isBlank()) {
            return emptySet()
        }

        return studentAssignmentsRef
            .whereEqualTo(
                "studentId",
                studentId
            )
            .get()
            .await()
            .documents
            .mapNotNull {
                it.getString("subjectId")
                    ?.takeIf(String::isNotBlank)
            }
            .toSet()
    }

    suspend fun saveAssignments(
        student: Student,
        available: List<Subject>,
        selectedSubjectIds: Set<String>
    ): Result {

        return try {

            if (student.studentId.isBlank()) {
                return Result.Error(
                    "Student ID is missing."
                )
            }

            if (student.classId.isBlank()) {
                return Result.Error(
                    "Student class is not assigned."
                )
            }

            if (available.isEmpty()) {
                return Result.Error(
                    "No subjects exist for this student's department, program and semester."
                )
            }

            val validSubjectIds =
                available.map { it.subjectId }.toSet()

            if (
                !selectedSubjectIds.all {
                    it in validSubjectIds
                }
            ) {
                return Result.Error(
                    "One or more selected subjects are invalid."
                )
            }

            /*
             * Remove old enrollment for this student.
             */
            val old =
                studentAssignmentsRef
                    .whereEqualTo(
                        "studentId",
                        student.studentId
                    )
                    .get()
                    .await()
                    .documents

            val batch =
                firestore.batch()

            old.forEach {
                batch.delete(it.reference)
            }

            /*
             * Save the newly selected catalogue subjects.
             *
             * NOTE:
             * teacherId is intentionally NOT stored here.
             *
             * Teacher assignment is a separate relation.
             */
            available
                .filter {
                    it.subjectId in selectedSubjectIds
                }
                .forEach { subject ->

                    val id =
                        buildId(
                            student.studentId,
                            subject.subjectId,
                            student.classId,
                            student.semester,
                            student.session
                        )

                    val model =
                        StudentSubjectAssignment(

                            assignmentId = id,

                            studentId =
                                student.studentId,

                            studentName =
                                student.fullName,

                            studentGeneratedId =
                                student.studentGeneratedId,

                            classId =
                                student.classId,

                            departmentName =
                                student.departmentName,

                            programName =
                                student.programName,

                            semester =
                                student.semester,

                            session =
                                student.session,

                            section =
                                student.section,

                            subjectId =
                                subject.subjectId,

                            subjectName =
                                subject.subjectName,

                            courseCode =
                                subject.courseCode
                        )

                    batch.set(
                        studentAssignmentsRef
                            .document(id),
                        model.toMap()
                    )
                }

            batch.commit().await()

            Result.Success(
                selectedSubjectIds.size
            )

        } catch (e: Exception) {

            Result.Error(
                e.message
                    ?: "Failed to save student subject assignment."
            )
        }
    }

    private fun numberField(
        value: Any?
    ): Int? {

        return when (value) {

            is Number ->
                value.toInt()

            is String ->
                value.toIntOrNull()
                    ?: Regex("\\d+")
                        .find(value)
                        ?.value
                        ?.toIntOrNull()

            else ->
                null
        }
    }

    companion object {

        fun buildId(
            studentId: String,
            subjectId: String,
            classId: String,
            semester: Int,
            session: String
        ): String {

            return listOf(
                studentId,
                subjectId,
                classId,
                semester,
                session
            )
                .joinToString("_")
                .replace(
                    Regex("[^A-Za-z0-9_-]"),
                    "_"
                )
        }
    }
}