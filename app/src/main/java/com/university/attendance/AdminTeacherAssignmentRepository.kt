package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Admin source of truth for Teacher -> Class -> Subject assignments.
 *
 * Classes come from `classes`.
 * Subjects come from `subjects`.
 * The actual teacher/class/subject relation is stored in
 * `teacherSubjectAssignments`.
 *
 * We DO NOT store one teacher directly on Subject because the same
 * subject can be taught by different teachers in different classes.
 */
class AdminTeacherAssignmentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val teachersRef = firestore.collection("teachers")
    private val classesRef = firestore.collection("classes")
    private val subjectsRef = firestore.collection("subjects")
    private val assignmentsRef = firestore.collection("teacherSubjectAssignments")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String) : OpResult()
    }

    suspend fun getAllTeachers(): List<Teacher> {
        return teachersRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Teacher::class.java)?.apply {
                    teacherId = doc.id
                }
            }
            .filter {
                it.teacherId.isNotBlank() &&
                        it.fullName.isNotBlank()
            }
            .sortedBy {
                it.fullName.lowercase()
            }
    }

    /**
     * Classes are read from the real classes collection.
     *
     * We do NOT depend on students collection to discover classes.
     */
    suspend fun getAllClasses(
        session: String,
        semester: Int
    ): List<StudentClass> {

        val cleanSession = session.trim()

        if (cleanSession.isBlank()) {
            return emptyList()
        }

        return classesRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val docSession =
                    doc.getString("session")
                        .orEmpty()
                        .trim()

                if (
                    doc.id.isBlank() ||
                    !docSession.equals(
                        cleanSession,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                StudentClass(
                    classId = doc.id,

                    universityName =
                        doc.getString("universityName")
                            .orEmpty(),

                    departmentName =
                        doc.getString("departmentName")
                            .orEmpty(),

                    programName =
                        doc.getString("programName")
                            .orEmpty(),

                    session =
                        docSession,

                    section =
                        doc.getString("section")
                            .orEmpty(),

                    studentCount =
                        doc.getLong("studentCount")
                            ?: 0L
                )
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.section }
                )
            )
    }

    /**
     * Gets subjects belonging to the selected class/program/department
     * and selected semester.
     *
     * Handles Firestore semester stored as String OR Number.
     */
    suspend fun getSubjectsForClass(
        classId: String,
        semester: Int
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

        val program =
            classDoc
                .getString("programName")
                .orEmpty()
                .trim()

        val department =
            classDoc
                .getString("departmentName")
                .orEmpty()
                .trim()

        return subjectsRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                val rawSemester =
                    doc.get("semester")

                val subjectSemester =
                    when (rawSemester) {

                        is Number ->
                            rawSemester.toInt()

                        is String ->
                            rawSemester.toIntOrNull()
                                ?: Regex("\\d+")
                                    .find(rawSemester)
                                    ?.value
                                    ?.toIntOrNull()

                        else ->
                            null
                    }

                if (subjectSemester != semester) {
                    return@mapNotNull null
                }

                val subjectProgram =
                    doc.getString("programName")
                        .orEmpty()
                        .trim()

                val subjectDepartment =
                    doc.getString("departmentName")
                        .orEmpty()
                        .trim()

                if (
                    !subjectProgram.equals(
                        program,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                if (
                    !subjectDepartment.equals(
                        department,
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                Subject(

                    subjectId =
                        doc.id,

                    departmentId =
                        doc.getString("departmentId")
                            .orEmpty(),

                    departmentName =
                        subjectDepartment,

                    programName =
                        subjectProgram,

                    semester =
                        semester.toString(),

                    courseCode =
                        doc.getString("courseCode")
                            .orEmpty(),

                    subjectName =
                        doc.getString("subjectName")
                            .orEmpty(),

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
                        },

                    teacherId =
                        doc.getString("teacherId")
                            .orEmpty(),

                    teacherName =
                        doc.getString("teacherName")
                            .orEmpty()
                )
            }
            .sortedWith(
                compareBy(
                    { it.courseCode },
                    { it.subjectName }
                )
            )
    }

    suspend fun getAssignedSubjectIds(
        teacherId: String,
        classId: String,
        semester: Int,
        session: String
    ): Set<String> {

        if (
            teacherId.isBlank() ||
            classId.isBlank() ||
            session.isBlank()
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

                it.getString("classId") ==
                        classId &&

                        it.getString("session")
                            .orEmpty()
                            .equals(
                                session.trim(),
                                ignoreCase = true
                            ) &&

                        numberField(
                            it.get("semester")
                        ) == semester
            }
            .mapNotNull {

                it.getString("subjectId")
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
            .toSet()
    }

    /**
     * Saves the exact:
     *
     * Teacher
     * + Class
     * + Subject
     * + Semester
     * + Session
     *
     * relation.
     */
    suspend fun saveAssignment(
        teacher: Teacher,
        selectedClass: StudentClass,
        semester: Int,
        session: String,
        subjects: List<Subject>,
        selectedSubjectIds: Set<String>
    ): OpResult {

        return try {

            val cleanSession =
                session.trim()

            if (teacher.teacherId.isBlank()) {
                return OpResult.Error(
                    "Teacher ID is missing."
                )
            }

            if (selectedClass.classId.isBlank()) {
                return OpResult.Error(
                    "Class is missing."
                )
            }

            if (cleanSession.isBlank()) {
                return OpResult.Error(
                    "Session is required."
                )
            }

            if (selectedSubjectIds.isEmpty()) {
                return OpResult.Error(
                    "Select at least one subject."
                )
            }

            val validSubjectIds =
                subjects
                    .map { it.subjectId }
                    .toSet()

            if (
                !selectedSubjectIds.all {
                    it in validSubjectIds
                }
            ) {
                return OpResult.Error(
                    "One or more selected subjects do not belong to the selected class/semester."
                )
            }

            val oldAssignments =
                assignmentsRef
                    .whereEqualTo(
                        "teacherId",
                        teacher.teacherId
                    )
                    .get()
                    .await()
                    .documents
                    .filter {

                        it.getString("classId") ==
                                selectedClass.classId &&

                                it.getString("session")
                                    .orEmpty()
                                    .equals(
                                        cleanSession,
                                        ignoreCase = true
                                    ) &&

                                numberField(
                                    it.get("semester")
                                ) == semester
                    }

            val batch =
                firestore.batch()

            /*
             * Delete old assignments for this exact
             * teacher/class/semester/session.
             */
            oldAssignments.forEach {
                batch.delete(
                    it.reference
                )
            }

            /*
             * Re-create selected assignments.
             */
            subjects
                .filter {
                    it.subjectId in
                            selectedSubjectIds
                }
                .forEach { subject ->

                    val assignmentId =
                        buildAssignmentId(
                            teacher.teacherId,
                            selectedClass.classId,
                            semester,
                            cleanSession,
                            subject.subjectId
                        )

                    val assignment =
                        TeacherSubjectAssignment(

                            assignmentId =
                                assignmentId,

                            teacherId =
                                teacher.teacherId,

                            teacherName =
                                teacher.fullName,

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
                                semester,

                            session =
                                cleanSession
                        )

                    batch.set(
                        assignmentsRef
                            .document(
                                assignmentId
                            ),
                        assignment.toMap()
                    )
                }

            batch.commit().await()

            OpResult.Success

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save class/subject assignment."
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
                semester.toString(),
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