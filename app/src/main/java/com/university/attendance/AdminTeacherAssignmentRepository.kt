package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Exact Teacher -> Class -> Semester -> Subject assignment.
 */
class AdminTeacherAssignmentRepository(
    private val firestore:
    FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val teachersRef =
        firestore.collection("teachers")

    private val classesRef =
        firestore.collection("classes")

    private val studentsRef =
        firestore.collection("students")

    private val subjectsRef =
        firestore.collection("subjects")

    private val assignmentsRef =
        firestore.collection("teacherSubjectAssignments")

    private val studentAssignmentsRef =
        firestore.collection("studentSubjectAssignments")

    sealed class OpResult {

        object Success :
            OpResult()

        data class Error(
            val message: String
        ) : OpResult()
    }

    /*
     * ------------------------------------------------------------
     * ALL ACTIVE TEACHERS
     * ------------------------------------------------------------
     */

    suspend fun getAllTeachers():
            List<Teacher> {

        return teachersRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                doc.toObject(
                    Teacher::class.java
                )?.apply {

                    teacherId =
                        doc.id
                }
            }
            .filter {

                it.teacherId.isNotBlank() &&

                        (
                                it.fullName.isNotBlank() ||
                                        it.email.isNotBlank()
                                ) &&

                        it.isActive
            }
            .sortedBy {

                it.fullName
                    .ifBlank {
                        it.email
                    }
                    .lowercase()
            }
    }

    /*
     * ------------------------------------------------------------
     * CLASSES FOR SESSION + SEMESTER
     * ------------------------------------------------------------
     *
     * Class document itself does not have semester because the
     * same cohort moves from semester to semester.
     *
     * Therefore semester is derived from students.
     */

    suspend fun getAllClasses(
        session: String,
        semester: Int
    ): List<StudentClass> {

        val cleanSession =
            session.trim()

        if (cleanSession.isBlank()) {
            return emptyList()
        }

        val students =
            studentsRef
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

                    it.isActive &&

                            it.classId.isNotBlank() &&

                            it.semester ==
                            semester &&

                            it.session
                                .trim()
                                .equals(
                                    cleanSession,
                                    true
                                )
                }

        return students
            .groupBy {
                it.classId
            }
            .mapNotNull { (classId, roster) ->

                val classDoc =
                    classesRef
                        .document(classId)
                        .get()
                        .await()

                if (!classDoc.exists()) {
                    return@mapNotNull null
                }

                StudentClass(

                    classId =
                        classDoc.id,

                    universityName =
                        classDoc
                            .getString(
                                "universityName"
                            )
                            .orEmpty(),

                    departmentName =
                        classDoc
                            .getString(
                                "departmentName"
                            )
                            .orEmpty(),

                    programName =
                        classDoc
                            .getString(
                                "programName"
                            )
                            .orEmpty(),

                    session =
                        classDoc
                            .getString(
                                "session"
                            )
                            .orEmpty()
                            .ifBlank {
                                cleanSession
                            },

                    section =
                        classDoc
                            .getString(
                                "section"
                            )
                            .orEmpty(),

                    studentCount =
                        roster.size.toLong()
                )
            }
            .sortedWith(
                compareBy(
                    { it.departmentName },
                    { it.programName },
                    { it.section }
                )
            )
    }

    /*
     * ------------------------------------------------------------
     * SUBJECT CATALOGUE
     * ------------------------------------------------------------
     *
     * Exact:
     * class Department
     * class Program
     * selected Semester
     */

    suspend fun getSubjectsForClass(
        classId: String,
        semester: Int
    ): List<Subject> {

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

                val subjectSemester =
                    numberField(
                        doc.get("semester")
                    )
                        ?: return@mapNotNull null

                if (
                    subjectSemester !=
                    semester
                ) {
                    return@mapNotNull null
                }

                val subjectProgram =
                    doc
                        .getString(
                            "programName"
                        )
                        .orEmpty()
                        .trim()

                val subjectDepartment =
                    doc
                        .getString(
                            "departmentName"
                        )
                        .orEmpty()
                        .trim()

                if (
                    !subjectProgram.equals(
                        program,
                        true
                    )
                ) {
                    return@mapNotNull null
                }

                if (
                    !subjectDepartment.equals(
                        department,
                        true
                    )
                ) {
                    return@mapNotNull null
                }

                Subject(

                    subjectId =
                        doc.id,

                    departmentId =
                        doc
                            .getString(
                                "departmentId"
                            )
                            .orEmpty(),

                    departmentName =
                        subjectDepartment,

                    programName =
                        subjectProgram,

                    semester =
                        subjectSemester
                            .toString(),

                    courseCode =
                        doc
                            .getString(
                                "courseCode"
                            )
                            .orEmpty(),

                    subjectName =
                        doc
                            .getString(
                                "subjectName"
                            )
                            .orEmpty(),

                    creditHours =
                        when (
                            val value =
                                doc.get(
                                    "creditHours"
                                )
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

    /*
     * ------------------------------------------------------------
     * ALREADY ASSIGNED SUBJECTS
     * ------------------------------------------------------------
     */

    suspend fun getAssignedSubjectIds(
        teacherId: String,
        classId: String,
        semester: Int,
        session: String
    ): Set<String> {

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
                ) == classId &&

                        numberField(
                            it.get("semester")
                        ) == semester &&

                        it.getString(
                            "session"
                        )
                            .orEmpty()
                            .trim()
                            .equals(
                                session.trim(),
                                true
                            )
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

    /*
     * ------------------------------------------------------------
     * SAVE EXACT TEACHER ASSIGNMENT
     * ------------------------------------------------------------
     *
     * Teacher A can teach MT in Class A.
     * Teacher B can teach MT in Class B.
     *
     * Subject document is NEVER modified.
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
                    "Selected subject does not belong to this class/program/semester."
                )
            }

            /*
             * Delete only this teacher's assignments
             * for this exact class + semester + session.
             */
            val old =
                assignmentsRef
                    .whereEqualTo(
                        "teacherId",
                        teacher.teacherId
                    )
                    .get()
                    .await()
                    .documents
                    .filter {

                        it.getString(
                            "classId"
                        ) ==
                                selectedClass.classId &&

                                numberField(
                                    it.get("semester")
                                ) ==
                                semester &&

                                it.getString(
                                    "session"
                                )
                                    .orEmpty()
                                    .trim()
                                    .equals(
                                        cleanSession,
                                        true
                                    )
                    }

            val batchWriter =
                ChunkedBatchWriter(firestore)

            old.forEach {
                batchWriter.delete(
                    it.reference
                )
            }

            val selectedSubjects =
                subjects.filter {
                    it.subjectId in
                            selectedSubjectIds
                }

            selectedSubjects
                .forEach { subject ->

                    val assignmentId =
                        buildAssignmentId(
                            teacher.teacherId,
                            selectedClass.classId,
                            semester,
                            cleanSession,
                            subject.subjectId
                        )

                    val model =
                        TeacherSubjectAssignment(

                            assignmentId =
                                assignmentId,

                            teacherId =
                                teacher.teacherId,

                            teacherName =
                                teacher.fullName
                                    .ifBlank {
                                        teacher.email
                                    },

                            classId =
                                selectedClass.classId,

                            className =
                                "${selectedClass.programName} • " +
                                        "Section ${selectedClass.section}",

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

                    batchWriter.set(
                        assignmentsRef
                            .document(
                                assignmentId
                            ),
                        model.toMap()
                    )
                }

            /*
             * ------------------------------------------------------------
             * AUTO-LINK STUDENTS TO THE ASSIGNED SUBJECTS
             * ------------------------------------------------------------
             *
             * This is the step that was missing: assigning a teacher to a
             * class + subject must also enroll every active student of
             * that exact class in that subject, otherwise no student ever
             * becomes "eligible" for the live attendance session later
             * (Student Dashboard / markAttendance both require a matching
             * studentSubjectAssignments record). Without this, the teacher
             * assignment and class schedule work perfectly but students
             * never see a live class.
             *
             * Records created here are tagged autoAssignedFromTeacher=true
             * so they can be told apart from manual per-student enrollment
             * done from the separate Student Subject Assignment screen,
             * and so re-saving this exact assignment is idempotent (same
             * deterministic document id) and cleans up stale links if a
             * subject is later unticked.
             */

            val activeStudents =
                studentsRef
                    .whereEqualTo(
                        "classId",
                        selectedClass.classId
                    )
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->

                        doc.toObject(
                            Student::class.java
                        )?.apply {
                            studentId = doc.id
                        }
                    }
                    .filter {

                        it.isActive &&
                                it.studentId.isNotBlank() &&
                                it.semester == semester &&
                                it.session
                                    .trim()
                                    .equals(
                                        cleanSession,
                                        true
                                    )
                    }

            val staleAutoLinks =
                if (activeStudents.isEmpty()) {
                    emptyList()
                } else {
                    val studentIds =
                        activeStudents
                            .map { it.studentId }
                            .toSet()

                    studentAssignmentsRef
                        .whereEqualTo(
                            "classId",
                            selectedClass.classId
                        )
                        .get()
                        .await()
                        .documents
                        .filter { doc ->

                            (
                                    doc.getBoolean(
                                        "autoAssignedFromTeacher"
                                    ) ?: false
                                    ) &&

                                    doc.getString("studentId")
                                        .orEmpty() in studentIds &&

                                    numberField(
                                        doc.get("semester")
                                    ) == semester &&

                                    doc.getString("session")
                                        .orEmpty()
                                        .trim()
                                        .equals(cleanSession, true) &&

                                    doc.getString("subjectId")
                                        .orEmpty() !in selectedSubjectIds
                        }
                }

            staleAutoLinks.forEach {
                batchWriter.delete(it.reference)
            }

            activeStudents.forEach { student ->

                selectedSubjects.forEach { subject ->

                    val linkId =
                        StudentSubjectAssignmentRepository
                            .buildId(
                                student.studentId,
                                subject.subjectId,
                                selectedClass.classId,
                                semester,
                                cleanSession
                            )

                    val linkModel =
                        StudentSubjectAssignment(

                            assignmentId = linkId,
                            studentId = student.studentId,
                            studentName = student.fullName,
                            studentGeneratedId = student.studentGeneratedId,
                            classId = selectedClass.classId,
                            departmentName = selectedClass.departmentName,
                            programName = selectedClass.programName,
                            semester = semester,
                            session = cleanSession,
                            section = selectedClass.section,
                            subjectId = subject.subjectId,
                            subjectName = subject.subjectName,
                            courseCode = subject.courseCode
                        )

                    val linkData =
                        linkModel.toMap().toMutableMap()

                    linkData["autoAssignedFromTeacher"] = true

                    batchWriter.set(
                        studentAssignmentsRef.document(linkId),
                        linkData
                    )
                }
            }

            batchWriter.commit()

            OpResult.Success

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save teacher assignment."
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

/**
 * Firestore limits a single WriteBatch to 500 operations. Teacher
 * assignment now also auto-links every active student of a class to the
 * assigned subjects, so a large class + several subjects at once could
 * exceed that limit. This collects writes and flushes them in safe-sized
 * batches (sequentially awaited) instead of failing once a class grows.
 */
private class ChunkedBatchWriter(
    private val firestore: FirebaseFirestore,
    private val chunkSize: Int = 400
) {

    private var current = firestore.batch()
    private var opsInCurrent = 0
    private val pendingBatches = mutableListOf<com.google.firebase.firestore.WriteBatch>()

    private fun rollIfNeeded() {
        if (opsInCurrent >= chunkSize) {
            pendingBatches.add(current)
            current = firestore.batch()
            opsInCurrent = 0
        }
    }

    fun delete(ref: com.google.firebase.firestore.DocumentReference) {
        rollIfNeeded()
        current.delete(ref)
        opsInCurrent++
    }

    fun set(ref: com.google.firebase.firestore.DocumentReference, data: Map<String, Any?>) {
        rollIfNeeded()
        current.set(ref, data)
        opsInCurrent++
    }

    suspend fun commit() {
        // Every roll pushes the previous (now-full) batch aside; the
        // currently accumulating one still needs to be committed too.
        (pendingBatches + current).forEach { it.commit().await() }
    }
}