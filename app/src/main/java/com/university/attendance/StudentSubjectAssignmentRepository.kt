package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Source of truth for:
 *
 * Student -> Subject enrollment
 *
 * Student subject enrollment does NOT depend on teacher assignment.
 *
 * Teacher assignment and student enrollment are two separate relations.
 */
class StudentSubjectAssignmentRepository(
    private val firestore:
    FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentsRef =
        firestore.collection("students")

    private val subjectsRef =
        firestore.collection("subjects")

    private val studentAssignmentsRef =
        firestore.collection(
            "studentSubjectAssignments"
        )

    sealed class Result {

        data class Success(
            val count: Int
        ) : Result()

        data class Error(
            val message: String
        ) : Result()
    }

    // ------------------------------------------------------------
    // STUDENTS
    // ------------------------------------------------------------

    suspend fun getStudents(): List<Student> {

        return studentsRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                doc.toObject(
                    Student::class.java
                )?.apply {

                    studentId =
                        doc.id
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
    }

    // ------------------------------------------------------------
    // SUBJECTS FOR STUDENT
    // ------------------------------------------------------------
    //
    // IMPORTANT:
    // No teacherSubjectAssignments check here.
    //
    // Student can be enrolled in a subject even if
    // teacher has not yet been assigned.
    // ------------------------------------------------------------

    suspend fun getSubjectsForStudent(
        student: Student
    ): List<Subject> {

        if (
            student.classId.isBlank()
        ) {
            return emptyList()
        }

        val department =
            student.departmentName
                .trim()

        val program =
            student.programName
                .trim()

        val semester =
            student.semester

        if (
            department.isBlank() ||
            program.isBlank() ||
            semester <= 0
        ) {
            return emptyList()
        }

        return subjectsRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val subjectSemester =
                    numberField(
                        doc.get("semester")
                    )
                        ?: return@mapNotNull null

                if (
                    subjectSemester != semester
                ) {
                    return@mapNotNull null
                }

                val subjectDepartment =
                    doc.getString(
                        "departmentName"
                    )
                        .orEmpty()
                        .trim()

                val subjectProgram =
                    doc.getString(
                        "programName"
                    )
                        .orEmpty()
                        .trim()

                if (
                    !subjectDepartment.equals(
                        department,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                if (
                    !subjectProgram.equals(
                        program,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                Subject(

                    subjectId =
                        doc.id,

                    departmentId =
                        doc.getString(
                            "departmentId"
                        ).orEmpty(),

                    departmentName =
                        subjectDepartment,

                    programName =
                        subjectProgram,

                    semester =
                        subjectSemester.toString(),

                    courseCode =
                        doc.getString(
                            "courseCode"
                        ).orEmpty(),

                    subjectName =
                        doc.getString(
                            "subjectName"
                        ).orEmpty(),

                    creditHours =
                        when (
                            val value =
                                doc.get("creditHours")
                        ) {

                            is Number ->
                                value.toString()

                            is String ->
                                value

                            else ->
                                ""
                        }
                )
            }
            .sortedWith(
                compareBy(
                    { it.courseCode },
                    { it.subjectName }
                )
            )
    }

    // ------------------------------------------------------------
    // ALREADY ENROLLED SUBJECTS
    // ------------------------------------------------------------

    suspend fun getAssignedSubjectIds(
        studentId: String
    ): Set<String> {

        if (
            studentId.isBlank()
        ) {
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

                it.getString(
                    "subjectId"
                )
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            .toSet()
    }

    // ------------------------------------------------------------
    // SAVE ENROLLMENT
    // ------------------------------------------------------------

    suspend fun saveAssignments(
        student: Student,
        available: List<Subject>,
        selectedSubjectIds: Set<String>
    ): Result {

        return try {

            if (
                student.studentId.isBlank()
            ) {

                return Result.Error(
                    "Student ID is missing."
                )
            }

            if (
                student.classId.isBlank()
            ) {

                return Result.Error(
                    "Student is not assigned to a class."
                )
            }

            if (
                available.isEmpty()
            ) {

                return Result.Error(
                    "No subjects found for this student's class."
                )
            }

            val validSubjectIds =
                available
                    .map {
                        it.subjectId
                    }
                    .toSet()

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
             * Remove previous enrollment records
             * for this student.
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

                batch.delete(
                    it.reference
                )
            }

            /*
             * Save selected subjects.
             */
            available
                .filter {
                    it.subjectId in
                            selectedSubjectIds
                }
                .forEach { subject ->

                    val assignmentId =
                        buildId(

                            studentId =
                                student.studentId,

                            subjectId =
                                subject.subjectId,

                            classId =
                                student.classId,

                            semester =
                                student.semester,

                            session =
                                student.session
                        )

                    val model =
                        StudentSubjectAssignment(

                            assignmentId =
                                assignmentId,

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
                            .document(
                                assignmentId
                            ),

                        model.toMap()
                    )
                }

            batch.commit()
                .await()

            Result.Success(
                selectedSubjectIds.size
            )

        } catch (e: Exception) {

            Result.Error(
                e.message
                    ?: "Failed to save student subjects."
            )
        }
    }

    // ------------------------------------------------------------
    // NUMBER HELPER
    // ------------------------------------------------------------

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