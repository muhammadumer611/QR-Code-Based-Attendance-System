package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Source of truth for:
 *
 * Teacher -> Class -> Subject
 *
 * Class already contains:
 * department
 * program
 * semester
 * session
 * section
 *
 * Therefor Teacher Assignment screen does NOT need separate
 * semester/session selection.
 */
class AdminTeacherAssignmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val teachersRef =
        firestore.collection("teachers")

    private val classesRef =
        firestore.collection("classes")

    private val subjectsRef =
        firestore.collection("subjects")

    private val assignmentsRef =
        firestore.collection("teacherSubjectAssignments")

    sealed class OpResult {
        object Success : OpResult()

        data class Error(
            val message: String
        ) : OpResult()
    }

    // ------------------------------------------------------------
    // TEACHERS
    // ------------------------------------------------------------

    suspend fun getAllTeachers(): List<Teacher> {

        return try {

            teachersRef
                .get()
                .await()
                .documents
                .mapNotNull { doc ->

                    doc.toObject(
                        Teacher::class.java
                    )?.apply {

                        teacherId = doc.id
                    }
                }
                .filter {

                    it.teacherId.isNotBlank() &&
                            (
                                    it.fullName.isNotBlank() ||
                                            it.email.isNotBlank()
                                    )
                }
                .sortedBy {

                    it.fullName
                        .ifBlank {
                            it.email
                        }
                        .lowercase()
                }

        } catch (e: Exception) {

            throw e
        }
    }

    // ------------------------------------------------------------
    // ALL CLASSES
    // ------------------------------------------------------------

    suspend fun getAllClasses(): List<StudentClass> {

        return classesRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val semester =
                    numberField(
                        doc.get("semester")
                    ) ?: 1

                StudentClass(

                    classId =
                        doc.id,

                    universityName =
                        doc.getString(
                            "universityName"
                        ).orEmpty(),

                    departmentName =
                        doc.getString(
                            "departmentName"
                        ).orEmpty(),

                    programName =
                        doc.getString(
                            "programName"
                        ).orEmpty(),

                    session =
                        doc.getString(
                            "session"
                        ).orEmpty(),

                    section =
                        doc.getString(
                            "section"
                        ).orEmpty(),

                    semester =
                        semester,

                    studentCount =
                        doc.getLong(
                            "studentCount"
                        ) ?: 0L
                )
            }
            .sortedWith(
                compareBy(
                    { it.departmentName.lowercase() },
                    { it.programName.lowercase() },
                    { it.semester },
                    { it.session.lowercase() },
                    { it.section.lowercase() }
                )
            )
    }

    // ------------------------------------------------------------
    // SUBJECTS FOR SELECTED CLASS
    // ------------------------------------------------------------

    suspend fun getSubjectsForClass(
        classId: String
    ): List<Subject> {

        if (classId.isBlank()) {
            return emptyList()
        }

        val classDoc =
            classesRef
                .document(classId)
                .get()
                .await()

        if (!classDoc.exists()) {
            return emptyList()
        }

        val department =
            classDoc
                .getString("departmentName")
                .orEmpty()
                .trim()

        val program =
            classDoc
                .getString("programName")
                .orEmpty()
                .trim()

        val semester =
            numberField(
                classDoc.get("semester")
            ) ?: 1

        return subjectsRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val subjectSemester =
                    numberField(
                        doc.get("semester")
                    ) ?: return@mapNotNull null

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
    // EXISTING ASSIGNMENTS
    // ------------------------------------------------------------

    suspend fun getAssignedSubjectIds(
        teacherId: String,
        classId: String
    ): Set<String> {

        if (
            teacherId.isBlank() ||
            classId.isBlank()
        ) {
            return emptySet()
        }

        return assignmentsRef
            .whereEqualTo(
                "teacherId",
                teacherId
            )
            .get()
            .await()
            .documents
            .filter {

                it.getString(
                    "classId"
                ) == classId
            }
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
    // SAVE TEACHER -> CLASS -> SUBJECTS
    // ------------------------------------------------------------

    suspend fun saveAssignment(
        teacher: Teacher,
        selectedClass: StudentClass,
        subjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): OpResult {

        return try {

            if (teacher.teacherId.isBlank()) {

                return OpResult.Error(
                    "Teacher ID is missing."
                )
            }

            if (selectedClass.classId.isBlank()) {

                return OpResult.Error(
                    "Class ID is missing."
                )
            }

            if (selectedSubjectIds.isEmpty()) {

                return OpResult.Error(
                    "Please select at least one subject."
                )
            }

            val validSubjectIds =
                subjects
                    .map {
                        it.subjectId
                    }
                    .toSet()

            if (
                !selectedSubjectIds.all {
                    it in validSubjectIds
                }
            ) {

                return OpResult.Error(
                    "One or more selected subjects do not belong to this class."
                )
            }

            /*
             * Get every assignment for this class.
             *
             * We use this to make sure one subject in one class
             * belongs to one teacher only.
             */
            val classAssignments =
                assignmentsRef
                    .whereEqualTo(
                        "classId",
                        selectedClass.classId
                    )
                    .get()
                    .await()
                    .documents

            val batch =
                firestore.batch()

            /*
             * Remove old assignments of THIS teacher
             * for this class.
             *
             * This allows Admin to uncheck subjects later.
             */
            classAssignments
                .filter {

                    it.getString(
                        "teacherId"
                    ) == teacher.teacherId

                }
                .forEach {

                    batch.delete(
                        it.reference
                    )
                }

            /*
             * If selected subject is currently assigned
             * to another teacher for the same class,
             * remove that old assignment.
             *
             * Result:
             *
             * One Class + One Subject = One Teacher
             */
            classAssignments
                .filter { doc ->

                    val subjectId =
                        doc.getString(
                            "subjectId"
                        )

                    subjectId != null &&
                            subjectId in selectedSubjectIds

                }
                .forEach {

                    batch.delete(
                        it.reference
                    )
                }

            val teacherName =
                teacher.fullName
                    .ifBlank {
                        teacher.email
                    }

            /*
             * Create new assignments.
             */
            subjects
                .filter {
                    it.subjectId in selectedSubjectIds
                }
                .forEach { subject ->

                    val assignmentId =
                        buildAssignmentId(
                            teacherId =
                                teacher.teacherId,

                            classId =
                                selectedClass.classId,

                            semester =
                                selectedClass.semester,

                            session =
                                selectedClass.session,

                            subjectId =
                                subject.subjectId
                        )

                    val model =
                        TeacherSubjectAssignment(

                            assignmentId =
                                assignmentId,

                            teacherId =
                                teacher.teacherId,

                            teacherName =
                                teacherName,

                            classId =
                                selectedClass.classId,

                            className =
                                "${selectedClass.programName} • Section ${selectedClass.section}",

                            departmentName =
                                selectedClass.departmentName,

                            programName =
                                selectedClass.programName,

                            section =
                                selectedClass.section,

                            subjectId =
                                subject.subjectId,

                            subjectName =
                                subject.subjectName,

                            courseCode =
                                subject.courseCode,

                            semester =
                                selectedClass.semester,

                            session =
                                selectedClass.session
                        )

                    batch.set(
                        assignmentsRef
                            .document(
                                assignmentId
                            ),
                        model.toMap()
                    )
                }

            batch.commit().await()

            OpResult.Success

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save teacher assignment."
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

        fun buildAssignmentId(
            teacherId: String,
            classId: String,
            semester: Int,
            session: String,
            subjectId: String
        ): String {

            return listOf(
                teacherId,
                classId,
                semester,
                session,
                subjectId
            )
                .joinToString("_")
                .replace(
                    Regex("[^A-Za-z0-9_-]"),
                    "_"
                )
        }
    }
}