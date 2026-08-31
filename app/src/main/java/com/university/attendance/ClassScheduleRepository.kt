package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClassScheduleRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val classScheduleRef =
        firestore.collection(
            "classSchedules"
        )

    private val assignmentRef =
        firestore.collection(
            "teacherSubjectAssignments"
        )

    sealed class OpResult {

        data class Success(
            val scheduleId: String
        ) : OpResult()

        data class Error(
            val message: String,
            val exception: Exception? = null
        ) : OpResult()
    }

    // ============================================================
    // SUBJECTS ASSIGNED TO TEACHER
    // ============================================================

    suspend fun getAssignedSubjects(
        teacherId: String,
        semester: Int,
        session: String
    ): List<Subject> {

        if (
            teacherId.isBlank() ||
            session.isBlank()
        ) {
            return emptyList()
        }

        val snapshot =
            assignmentRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .whereEqualTo(
                    "semester",
                    semester
                )
                .whereEqualTo(
                    "session",
                    session.trim()
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                val subjectId =
                    doc.getString(
                        "subjectId"
                    )

                if (
                    subjectId.isNullOrBlank()
                ) {
                    null
                } else {

                    Subject(

                        subjectId =
                            subjectId,

                        subjectName =
                            doc.getString(
                                "subjectName"
                            ).orEmpty(),

                        courseCode =
                            doc.getString(
                                "courseCode"
                            ).orEmpty(),

                        programName =
                            doc.getString(
                                "programName"
                            ).orEmpty(),

                        departmentName =
                            doc.getString(
                                "departmentName"
                            ).orEmpty(),

                        semester =
                            semester.toString(),


                        teacherId =
                            teacherId,

                        teacherName =
                            doc.getString(
                                "teacherName"
                            ).orEmpty()
                    )
                }
            }
            .distinctBy {
                it.subjectId
            }
            .sortedBy {
                it.courseCode
            }
    }

    // ============================================================
    // SAVE
    // ============================================================

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
                classSchedule.subjectId.isBlank()
            ) {
                return OpResult.Error(
                    "Please select a subject."
                )
            }

            if (
                classSchedule.session.isBlank()
            ) {
                return OpResult.Error(
                    "Session is required."
                )
            }

            val document =
                classScheduleRef.document()

            document
                .set(
                    classSchedule.toMap()
                )
                .await()

            OpResult.Success(
                document.id
            )

        } catch (e: Exception) {

            OpResult.Error(
                e.message
                    ?: "Failed to save class schedule.",
                e
            )
        }
    }

    // ============================================================
    // TEACHER CLASSES
    // ============================================================

    suspend fun getClassesForTeacher(
        teacherId: String
    ): List<ClassSchedule> {

        if (teacherId.isBlank()) {
            return emptyList()
        }

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { doc ->

                doc.toObject(
                    ClassSchedule::class.java
                )?.apply {

                    scheduleId =
                        doc.id
                }
            }
            .sortedWith(
                compareBy(
                    { it.date },
                    { parseTime(it.startTime) }
                )
            )
    }

    // ============================================================
    // TODAY
    // ============================================================

    suspend fun getTodayClassesForTeacher(
        teacherId: String
    ): List<ClassSchedule> {

        val all =
            getClassesForTeacher(
                teacherId
            )

        val today =
            todayDate()

        val day =
            todayDayName()

        return all
            .filter {

                val daily =
                    it.periodType
                        .equals(
                            "Daily",
                            ignoreCase = true
                        ) &&
                            it.date == today

                val weekly =
                    it.periodType
                        .equals(
                            "Weekly",
                            ignoreCase = true
                        ) &&
                            it.dayName
                                .equals(
                                    day,
                                    ignoreCase = true
                                )

                daily || weekly
            }
            .sortedBy {
                parseTime(
                    it.startTime
                )
            }
    }

    // ============================================================
    // DATE
    // ============================================================

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

    // ============================================================
    // DELETE
    // ============================================================

    suspend fun deleteClass(
        scheduleId: String
    ): OpResult {

        return try {

            classScheduleRef
                .document(
                    scheduleId
                )
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
    }

    // ============================================================
    // UPDATE
    // ============================================================

    suspend fun updateClass(
        scheduleId: String,
        classSchedule: ClassSchedule
    ): OpResult {

        return try {

            classScheduleRef
                .document(
                    scheduleId
                )
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
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )
    }

    private fun todayDayName(): String {

        return SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )
    }

    private fun parseTime(
        time: String
    ): Long {

        return try {

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
}