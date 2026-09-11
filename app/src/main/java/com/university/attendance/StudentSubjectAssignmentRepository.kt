package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudentSubjectAssignmentRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentsRef =
        firestore.collection("students")

    private val teacherAssignmentsRef =
        firestore.collection(
            "teacherSubjectAssignments"
        )

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
                        it.isActive
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.session },
                    { it.section },
                    { it.fullName }
                )
            )
    }

    /**
     * Returns subjects already assigned to a teacher for
     * the exact student's class.
     */
    suspend fun getTeacherSubjectsForStudent(
        student: Student
    ): List<TeacherSubjectAssignment> {

        if (
            student.classId.isBlank() ||
            student.session.isBlank()
        ) {
            return emptyList()
        }

        return teacherAssignmentsRef
            .whereEqualTo(
                "classId",
                student.classId
            )
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val semester =
                    numberField(
                        doc.get("semester")
                    )
                        ?: return@mapNotNull null

                if (
                    semester !=
                    student.semester
                ) {
                    return@mapNotNull null
                }

                if (
                    !doc.getString(
                        "session"
                    )
                        .orEmpty()
                        .equals(
                            student.session,
                            true
                        )
                ) {
                    return@mapNotNull null
                }

                if (
                    !doc.getString(
                        "section"
                    )
                        .orEmpty()
                        .equals(
                            student.section,
                            true
                        )
                ) {
                    return@mapNotNull null
                }

                if (
                    !doc.getString(
                        "programName"
                    )
                        .orEmpty()
                        .equals(
                            student.programName,
                            true
                        )
                ) {
                    return@mapNotNull null
                }

                if (
                    !doc.getString(
                        "departmentName"
                    )
                        .orEmpty()
                        .equals(
                            student.departmentName,
                            true
                        )
                ) {
                    return@mapNotNull null
                }

                TeacherSubjectAssignment(

                    assignmentId =
                        doc.id,

                    teacherId =
                        doc.getString(
                            "teacherId"
                        ).orEmpty(),

                    teacherName =
                        doc.getString(
                            "teacherName"
                        ).orEmpty(),

                    classId =
                        doc.getString(
                            "classId"
                        ).orEmpty(),

                    className =
                        doc.getString(
                            "className"
                        ).orEmpty(),

                    departmentName =
                        doc.getString(
                            "departmentName"
                        ).orEmpty(),

                    programName =
                        doc.getString(
                            "programName"
                        ).orEmpty(),

                    section =
                        doc.getString(
                            "section"
                        ).orEmpty(),

                    subjectId =
                        doc.getString(
                            "subjectId"
                        ).orEmpty(),

                    subjectName =
                        doc.getString(
                            "subjectName"
                        ).orEmpty(),

                    courseCode =
                        doc.getString(
                            "courseCode"
                        ).orEmpty(),

                    semester =
                        semester,

                    session =
                        doc.getString(
                            "session"
                        ).orEmpty()
                )
            }
            .filter {

                it.subjectId.isNotBlank() &&
                        it.teacherId.isNotBlank()
            }
            .distinctBy {

                "${it.subjectId}_${it.teacherId}"
            }
            .sortedWith(
                compareBy(
                    { it.courseCode },
                    { it.subjectName },
                    { it.teacherName }
                )
            )
    }

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
                )?.takeIf {
                    it.isNotBlank()
                }
            }
            .toSet()
    }

    suspend fun saveAssignments(
        student: Student,
        available:
        List<TeacherSubjectAssignment>,
        selectedKeys: Set<String>
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
                    "Student class is not assigned."
                )
            }

            if (
                selectedKeys.isEmpty()
            ) {
                return Result.Error(
                    "Select at least one subject."
                )
            }

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

            available
                .filter {
                    key(it) in selectedKeys
                }
                .forEach { taught ->

                    val id =
                        buildId(
                            student.studentId,
                            taught.subjectId,
                            taught.teacherId,
                            student.classId,
                            student.semester,
                            student.session
                        )

                    val model =
                        StudentSubjectAssignment(

                            assignmentId =
                                id,

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
                                taught.subjectId,

                            subjectName =
                                taught.subjectName,

                            courseCode =
                                taught.courseCode,

                            teacherId =
                                taught.teacherId,

                            teacherName =
                                taught.teacherName
                        )

                    batch.set(
                        studentAssignmentsRef
                            .document(id),
                        model.toMap()
                    )
                }

            batch.commit()
                .await()

            Result.Success(
                selectedKeys.size
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

        fun key(
            assignment:
            TeacherSubjectAssignment
        ): String {

            return "${assignment.subjectId}__${assignment.teacherId}"
        }

        fun buildId(
            studentId: String,
            subjectId: String,
            teacherId: String,
            classId: String,
            semester: Int,
            session: String
        ): String {

            return listOf(
                studentId,
                subjectId,
                teacherId,
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