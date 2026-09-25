package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Admin source of truth for Student -> Class allocation.
 *
 * A student is linked to a class through the same classId used by
 * teacherSubjectAssignments, classSchedules and attendanceSessions.
 *
 * This common classId is what connects:
 *
 * Teacher -> Class -> Subject
 * Student -> Class -> Subject
 *
 * and ultimately allows attendance to be validated against the
 * same class.
 */
class StudentClassAssignmentRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentsRef =
        firestore.collection("students")

    private val classesRef =
        firestore.collection("classes")

    private val studentAssignmentsRef =
        firestore.collection("studentSubjectAssignments")

    sealed class Result {

        data class Success(
            val className: String
        ) : Result()

        data class Error(
            val message: String
        ) : Result()
    }

    /**
     * Get all active students.
     */
    suspend fun getStudents(): List<Student> {

        return studentsRef
            .get()
            .await()
            .documents
            .mapNotNull { doc ->

                doc.toObject(Student::class.java)?.apply {

                    /*
                     * Firestore document ID is the source of truth
                     * for studentId.
                     */
                    studentId = doc.id
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
                    { it.semester },
                    { it.session },
                    { it.section },
                    { it.fullName }
                )
            )
    }

    /**
     * Return classes that are valid for the selected student.
     *
     * Matching is done using:
     *
     * Department
     * Program
     * Session
     * Semester
     *
     * This prevents a student from being assigned to an unrelated class.
     */
    suspend fun getAvailableClasses(
        student: Student
    ): List<StudentClass> {

        if (student.session.isBlank()) {
            return emptyList()
        }

        if (student.semester <= 0) {
            return emptyList()
        }

        val classDocs =
            classesRef
                .get()
                .await()
                .documents

        return classDocs
            .mapNotNull { doc ->

                val department =
                    doc.getString("departmentName")
                        .orEmpty()
                        .trim()

                val program =
                    doc.getString("programName")
                        .orEmpty()
                        .trim()

                val session =
                    doc.getString("session")
                        .orEmpty()
                        .trim()

                /*
                 * Department must match.
                 */
                if (
                    !department.equals(
                        student.departmentName.trim(),
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                /*
                 * Program must match.
                 */
                if (
                    !program.equals(
                        student.programName.trim(),
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                /*
                 * Session must match.
                 */
                if (
                    !session.equals(
                        student.session.trim(),
                        ignoreCase = true
                    )
                ) {
                    return@mapNotNull null
                }

                /*
                 * Read semester safely.
                 *
                 * Firestore may contain:
                 *  - Int
                 *  - Long
                 *  - Double
                 *  - String
                 *  - "Semester 5"
                 *
                 * numberField() handles all supported cases.
                 */
                val storedSemester =
                    numberField(
                        doc.get("semester")
                    )

                /*
                 * Older class documents may not have semester.
                 *
                 * In that case, try to resolve the semester from
                 * an existing student already belonging to this class.
                 */
                val resolvedSemester =
                    storedSemester
                        ?: studentsRef
                            .whereEqualTo(
                                "classId",
                                doc.id
                            )
                            .limit(1)
                            .get()
                            .await()
                            .documents
                            .firstOrNull()
                            ?.let {
                                numberField(
                                    it.get("semester")
                                )
                            }

                /*
                 * Semester must match the selected student.
                 */
                if (
                    resolvedSemester !=
                    student.semester
                ) {
                    return@mapNotNull null
                }

                StudentClass(

                    classId = doc.id,

                    universityName =
                        doc.getString(
                            "universityName"
                        ).orEmpty(),

                    departmentName =
                        department,

                    programName =
                        program,

                    session =
                        session,

                    section =
                        doc.getString(
                            "section"
                        ).orEmpty(),

                    semester =
                        resolvedSemester
                            ?: student.semester,

                    studentCount =
                        doc.getLong(
                            "studentCount"
                        ) ?: 0L
                )
            }
            .sortedWith(

                compareBy(
                    { it.section },
                    { it.programName }
                )
            )
    }

    /**
     * Assign a student to a class.
     *
     * This updates:
     *
     * students/{studentId}
     *
     * and the student counts of:
     *
     * old class
     * new class
     *
     * Everything is performed in one Firestore transaction.
     */
    suspend fun assignStudentToClass(
        student: Student,
        targetClass: StudentClass
    ): Result {

        if (student.studentId.isBlank()) {

            return Result.Error(
                "Student ID is missing."
            )
        }

        if (targetClass.classId.isBlank()) {

            return Result.Error(
                "Class ID is missing."
            )
        }

        return try {

            val oldClassId =
                student.classId
                    .trim()

            val newClassId =
                targetClass.classId
                    .trim()

            /*
             * Student is already in this class.
             */
            if (
                oldClassId == newClassId
            ) {

                return Result.Success(
                    formatClass(targetClass)
                )
            }

            val oldClassRef =
                if (oldClassId.isNotBlank()) {

                    classesRef.document(
                        oldClassId
                    )

                } else {
                    null
                }

            val newClassRef =
                classesRef.document(
                    newClassId
                )

            val studentRef =
                studentsRef.document(
                    student.studentId
                )

            /*
             * Read class documents before transaction.
             */
            val oldClassSnapshot =
                oldClassRef
                    ?.get()
                    ?.await()

            val newClassSnapshot =
                newClassRef
                    .get()
                    .await()

            if (
                !newClassSnapshot.exists()
            ) {

                return Result.Error(
                    "Selected class no longer exists."
                )
            }

            val oldCount =
                oldClassSnapshot
                    ?.getLong("studentCount")
                    ?: 0L

            val newCount =
                newClassSnapshot
                    .getLong("studentCount")
                    ?: 0L

            /*
             * Find existing subject enrollments.
             *
             * They are class-scoped, so they must not remain
             * attached after the student changes class.
             */
            val oldEnrollments =
                studentAssignmentsRef
                    .whereEqualTo(
                        "studentId",
                        student.studentId
                    )
                    .get()
                    .await()
                    .documents

            /*
             * Atomic update.
             */
            firestore
                .runTransaction { transaction ->

                    /*
                     * Decrease old class count.
                     */
                    if (
                        oldClassRef != null &&
                        oldClassSnapshot?.exists() == true
                    ) {

                        transaction.update(
                            oldClassRef,
                            "studentCount",
                            (oldCount - 1L)
                                .coerceAtLeast(0L)
                        )
                    }

                    /*
                     * Increase new class count.
                     */
                    transaction.update(
                        newClassRef,
                        "studentCount",
                        newCount + 1L
                    )

                    /*
                     * Update student's class information.
                     */
                    transaction.update(

                        studentRef,

                        mapOf(

                            "classId" to
                                    newClassId,

                            "departmentName" to
                                    targetClass.departmentName,

                            "programName" to
                                    targetClass.programName,

                            "session" to
                                    targetClass.session,

                            "section" to
                                    targetClass.section,

                            "semester" to
                                    targetClass.semester
                        )
                    )

                    /*
                     * Remove old subject enrollments.
                     *
                     * The admin can enroll the student into
                     * subjects of the new class after wards.
                     */
                    oldEnrollments.forEach { enrollment ->

                        transaction.delete(
                            enrollment.reference
                        )
                    }

                    null
                }
                .await()

            Result.Success(
                formatClass(targetClass)
            )

        } catch (e: Exception) {

            Result.Error(
                e.message
                    ?: "Failed to assign student to class."
            )
        }
    }

    /**
     * Convert a Firestore value into an Int safely.
     *
     * Supported examples:
     *
     * 5
     * 5L
     * 5.0
     * "5"
     * "Semester 5"
     */
    private fun numberField(
        value: Any?
    ): Int? {

        return when (value) {

            is Number -> {

                value.toInt()
            }

            is String -> {

                value
                    .trim()
                    .toIntOrNull()
                    ?: Regex("\\d+")
                        .find(value)
                        ?.value
                        ?.toIntOrNull()
            }

            else -> {

                null
            }
        }
    }

    /**
     * Human-readable class name for UI / Toast messages.
     */
    private fun formatClass(
        c: StudentClass
    ): String {

        return listOf(

            c.departmentName,

            c.programName,

            "Semester ${c.semester}",

            "Session ${c.session}",

            "Section ${c.section}"

        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" • ")
    }
}

