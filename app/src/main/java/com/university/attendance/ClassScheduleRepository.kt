package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClassScheduleRepository(
    private val firestore:
    FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val classScheduleRef =
        firestore.collection("classSchedules")

    private val assignmentRef =
        firestore.collection("teacherSubjectAssignments")

    private val classesRef =
        firestore.collection("classes")

    sealed class OpResult {

        data class Success(
            val scheduleId: String
        ) : OpResult()

        data class Error(
            val message: String,
            val exception: Exception? = null
        ) : OpResult()
    }

    /*
     * ------------------------------------------------------------
     * TEACHER ASSIGNED CLASSES
     * ------------------------------------------------------------
     */

    suspend fun getAssignedClasses(
        teacherId: String,
        semester: Int,
        session: String
    ): List<StudentClass> {

        if (
            teacherId.isBlank() ||
            session.isBlank()
        ) {
            return emptyList()
        }

        val cleanSession =
            session.trim()

        val assignmentDocs =
            assignmentRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()
                .documents
                .filter {

                    numberField(
                        it.get("semester")
                    ) == semester &&

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

        val classIds =
            assignmentDocs
                .mapNotNull {
                    it.getString(
                        "classId"
                    )
                        ?.takeIf(
                            String::isNotBlank
                        )
                }
                .distinct()

        return classIds
            .mapNotNull { classId ->

                val doc =
                    classesRef
                        .document(classId)
                        .get()
                        .await()

                if (!doc.exists()) {
                    return@mapNotNull null
                }

                StudentClass(

                    classId =
                        doc.id,

                    universityName =
                        doc
                            .getString(
                                "universityName"
                            )
                            .orEmpty(),

                    departmentName =
                        doc
                            .getString(
                                "departmentName"
                            )
                            .orEmpty(),

                    programName =
                        doc
                            .getString(
                                "programName"
                            )
                            .orEmpty(),

                    session =
                        doc
                            .getString(
                                "session"
                            )
                            .orEmpty()
                            .ifBlank {
                                cleanSession
                            },

                    section =
                        doc
                            .getString(
                                "section"
                            )
                            .orEmpty(),

                    studentCount =
                        doc.getLong(
                            "studentCount"
                        ) ?: 0L
                )
            }
            .sortedWith(
                compareBy(
                    { it.programName },
                    { it.section }
                )
            )
    }

    /*
     * ------------------------------------------------------------
     * EXACT TEACHER SUBJECTS
     * ------------------------------------------------------------
     */

    suspend fun getAssignedSubjects(
        teacherId: String,
        classId: String,
        semester: Int,
        session: String
    ): List<Subject> {

        if (
            teacherId.isBlank() ||
            classId.isBlank() ||
            session.isBlank()
        ) {
            return emptyList()
        }

        return assignmentRef
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
            .mapNotNull { doc ->

                val id =
                    doc.getString(
                        "subjectId"
                    )
                        ?.takeIf(
                            String::isNotBlank
                        )
                        ?: return@mapNotNull null

                Subject(

                    subjectId =
                        id,

                    subjectName =
                        doc
                            .getString(
                                "subjectName"
                            )
                            .orEmpty(),

                    courseCode =
                        doc
                            .getString(
                                "courseCode"
                            )
                            .orEmpty(),

                    programName =
                        doc
                            .getString(
                                "programName"
                            )
                            .orEmpty(),

                    departmentName =
                        doc
                            .getString(
                                "departmentName"
                            )
                            .orEmpty(),

                    semester =
                        semester.toString(),

                    teacherId =
                        teacherId,

                    teacherName =
                        doc
                            .getString(
                                "teacherName"
                            )
                            .orEmpty()
                )
            }
            .distinctBy {
                it.subjectId
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
     * SAVE SCHEDULE
     * ------------------------------------------------------------
     */

    suspend fun saveClass(
        classSchedule: ClassSchedule
    ): OpResult {

        return try {

            if (
                classSchedule.teacherId.isBlank()
            ) {
                return OpResult.Error(
                    "Teacher ID is missing."
                )
            }

            if (
                classSchedule.classId.isBlank()
            ) {
                return OpResult.Error(
                    "Class is missing."
                )
            }

            if (
                classSchedule.subjectId.isBlank()
            ) {
                return OpResult.Error(
                    "Please select an assigned subject."
                )
            }

            if (
                classSchedule.session.isBlank()
            ) {
                return OpResult.Error(
                    "Session is required."
                )
            }

            /*
             * Final safety check.
             */
            val exactAssignment =
                assignmentRef
                    .whereEqualTo(
                        "teacherId",
                        classSchedule.teacherId
                    )
                    .get()
                    .await()
                    .documents
                    .any {

                        it.getString(
                            "classId"
                        ) ==
                                classSchedule.classId &&

                                it.getString(
                                    "subjectId"
                                ) ==
                                classSchedule.subjectId &&

                                numberField(
                                    it.get("semester")
                                ) ==
                                classSchedule.semester &&

                                it.getString(
                                    "session"
                                )
                                    .orEmpty()
                                    .trim()
                                    .equals(
                                        classSchedule.session
                                            .trim(),
                                        true
                                    )
                    }

            if (!exactAssignment) {

                return OpResult.Error(
                    "This teacher is not assigned to this exact class and subject for the selected semester/session."
                )
            }

            val ref =
                classScheduleRef.document()

            ref.set(
                classSchedule.toMap()
            ).await()

            OpResult.Success(
                ref.id
            )

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save class schedule.",
                e
            )
        }
    }

    suspend fun getClassesForTeacher(
        teacherId: String
    ): List<ClassSchedule> {

        return classScheduleRef
            .whereEqualTo(
                "teacherId",
                teacherId
            )
            .get()
            .await()
            .documents
            .map {
                ClassSchedule.fromDocument(
                    it
                )
            }
            .sortedWith(
                compareBy(
                    { it.date },
                    { parseTime(it.startTime) }
                )
            )
    }

    suspend fun getTodayClassesForTeacher(
        teacherId: String
    ): List<ClassSchedule> {

        return getClassesForTeacher(
            teacherId
        )
            .filter {

                (
                        it.periodType
                            .equals(
                                "Daily",
                                true
                            ) &&
                                it.date ==
                                todayDate()
                        ) ||

                        (
                                (
                                        it.periodType.equals(
                                            "Weekly",
                                            true
                                        ) ||
                                                it.periodType.equals(
                                                    "Semester",
                                                    true
                                                )
                                        ) &&

                                        (
                                                it.dayName.equals(
                                                    todayDayName(),
                                                    true
                                                ) ||
                                                        it.date ==
                                                        todayDate()
                                                )
                                )
            }
            .sortedBy {
                parseTime(
                    it.startTime
                )
            }
    }

    suspend fun getClassesForDate(
        teacherId: String,
        date: String
    ): List<ClassSchedule> {

        return getClassesForTeacher(
            teacherId
        )
            .filter {
                it.date == date
            }
    }

    suspend fun deleteClass(
        scheduleId: String
    ): OpResult = try {

        classScheduleRef
            .document(scheduleId)
            .delete()
            .await()

        OpResult.Success(
            scheduleId
        )

    } catch (e: Exception) {

        OpResult.Error(
            e.message
                ?: "Failed to delete class.",
            e
        )
    }

    suspend fun updateClass(
        scheduleId: String,
        classSchedule: ClassSchedule
    ): OpResult = try {

        classScheduleRef
            .document(scheduleId)
            .set(
                classSchedule.toMap()
            )
            .await()

        OpResult.Success(
            scheduleId
        )

    } catch (e: Exception) {

        OpResult.Error(
            e.message
                ?: "Failed to update class.",
            e
        )
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

    private fun todayDate(): String =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )

    private fun todayDayName(): String =
        SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )

    private fun parseTime(
        time: String
    ): Long =
        try {

            SimpleDateFormat(
                "hh:mm a",
                Locale.US
            )
                .parse(time)
                ?.time
                ?: Long.MAX_VALUE

        } catch (_: Exception) {

            Long.MAX_VALUE
        }
}