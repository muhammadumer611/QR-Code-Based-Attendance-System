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
        firestore.collection("classSchedules")

    // ============================================================
    // RESULT
    // ============================================================

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
    // SAVE CLASS
    // ============================================================

    suspend fun saveClass(
        classSchedule: ClassSchedule
    ): OpResult {

        return try {

            val document =
                classScheduleRef.document()

            document
                .set(classSchedule.toMap())
                .await()

            OpResult.Success(
                scheduleId = document.id
            )

        } catch (e: Exception) {

            OpResult.Error(
                message =
                    e.message
                        ?: "Failed to save class schedule.",

                exception = e
            )
        }
    }

    // ============================================================
    // GET ALL CLASSES FOR TEACHER
    // ============================================================

    suspend fun getClassesForTeacher(
        teacherAuthUid: String
    ): List<ClassSchedule> {

        if (teacherAuthUid.isBlank()) {
            return emptyList()
        }

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )
                .get()
                .await()

        return snapshot.documents.mapNotNull { document ->

            document
                .toObject(ClassSchedule::class.java)
                ?.apply {

                    scheduleId =
                        document.id
                }
        }.sortedWith(
            compareBy<ClassSchedule> {

                it.date

            }.thenBy {

                parseTimeForSorting(
                    it.startTime
                )
            }
        )
    }

    // ============================================================
    // GET TODAY'S CLASSES
    // ============================================================

    suspend fun getTodayClassesForTeacher(
        teacherAuthUid: String
    ): List<ClassSchedule> {

        if (teacherAuthUid.isBlank()) {
            return emptyList()
        }

        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Calendar.getInstance().time
            )

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )
                .whereEqualTo(
                    "date",
                    today
                )
                .get()
                .await()

        return snapshot.documents.mapNotNull { document ->

            document
                .toObject(ClassSchedule::class.java)
                ?.apply {

                    scheduleId =
                        document.id
                }

        }.sortedBy {

            parseTimeForSorting(
                it.startTime
            )
        }
    }

    // ============================================================
    // GET CLASSES FOR SPECIFIC DATE
    // ============================================================

    suspend fun getClassesForDate(
        teacherAuthUid: String,
        date: String
    ): List<ClassSchedule> {

        if (
            teacherAuthUid.isBlank() ||
            date.isBlank()
        ) {
            return emptyList()
        }

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )
                .whereEqualTo(
                    "date",
                    date
                )
                .get()
                .await()

        return snapshot.documents.mapNotNull { document ->

            document
                .toObject(ClassSchedule::class.java)
                ?.apply {

                    scheduleId =
                        document.id
                }

        }.sortedBy {

            parseTimeForSorting(
                it.startTime
            )
        }
    }

    // ============================================================
    // GET CLASSES BETWEEN TWO DATES
    // ============================================================

    suspend fun getClassesBetweenDates(
        teacherAuthUid: String,
        startDate: String,
        endDate: String
    ): List<ClassSchedule> {

        if (
            teacherAuthUid.isBlank() ||
            startDate.isBlank() ||
            endDate.isBlank()
        ) {
            return emptyList()
        }

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )
                .whereGreaterThanOrEqualTo(
                    "date",
                    startDate
                )
                .whereLessThanOrEqualTo(
                    "date",
                    endDate
                )
                .get()
                .await()

        return snapshot.documents.mapNotNull { document ->

            document
                .toObject(ClassSchedule::class.java)
                ?.apply {

                    scheduleId =
                        document.id
                }

        }.sortedWith(
            compareBy<ClassSchedule> {

                it.date

            }.thenBy {

                parseTimeForSorting(
                    it.startTime
                )
            }
        )
    }

    // ============================================================
    // DELETE CLASS
    // ============================================================

    suspend fun deleteClass(
        scheduleId: String
    ): OpResult {

        if (scheduleId.isBlank()) {

            return OpResult.Error(
                "Invalid class schedule ID."
            )
        }

        return try {

            classScheduleRef
                .document(scheduleId)
                .delete()
                .await()

            OpResult.Success(
                scheduleId
            )

        } catch (e: Exception) {

            OpResult.Error(
                message =
                    e.message
                        ?: "Failed to delete class.",

                exception = e
            )
        }
    }

    // ============================================================
    // UPDATE CLASS
    // ============================================================

    suspend fun updateClass(
        scheduleId: String,
        classSchedule: ClassSchedule
    ): OpResult {

        if (scheduleId.isBlank()) {

            return OpResult.Error(
                "Invalid class schedule ID."
            )
        }

        return try {

            classScheduleRef
                .document(scheduleId)
                .set(classSchedule.toMap())
                .await()

            OpResult.Success(
                scheduleId
            )

        } catch (e: Exception) {

            OpResult.Error(
                message =
                    e.message
                        ?: "Failed to update class.",

                exception = e
            )
        }
    }

    // ============================================================
    // TIME SORTING
    // ============================================================

    private fun parseTimeForSorting(
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