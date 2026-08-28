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

            if (classSchedule.teacherId.isBlank()) {

                return OpResult.Error(
                    "Teacher ID is missing."
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
                scheduleId = document.id
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
    // GET ALL CLASSES FOR TEACHER
    // PRIMARY KEY = TEACHER ID
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
            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }
            .sortedWith(
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
    // NO COMPOSITE INDEX REQUIRED
    // ============================================================

    suspend fun getTodayClassesForTeacher(
        teacherId: String
    ): List<ClassSchedule> {

        if (teacherId.isBlank()) {
            return emptyList()
        }

        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Calendar.getInstance().time
            )

        // Only teacherId query.
        // Date filtering is done locally.
        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()

        return snapshot.documents
            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }
            .filter {

                it.date == today
            }
            .sortedBy {

                parseTimeForSorting(
                    it.startTime
                )
            }
    }

    // ============================================================
    // GET CLASSES FOR SPECIFIC DATE
    // ============================================================

    suspend fun getClassesForDate(
        teacherId: String,
        date: String
    ): List<ClassSchedule> {

        if (
            teacherId.isBlank() ||
            date.isBlank()
        ) {
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
            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }
            .filter {

                it.date == date
            }
            .sortedBy {

                parseTimeForSorting(
                    it.startTime
                )
            }
    }

    // ============================================================
    // GET CLASSES BETWEEN TWO DATES
    // ============================================================

    suspend fun getClassesBetweenDates(
        teacherId: String,
        startDate: String,
        endDate: String
    ): List<ClassSchedule> {

        if (
            teacherId.isBlank() ||
            startDate.isBlank() ||
            endDate.isBlank()
        ) {
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
            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }
            .filter {

                it.date >= startDate &&
                        it.date <= endDate
            }
            .sortedWith(

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
    // DELETE
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

        if (scheduleId.isBlank()) {

            return OpResult.Error(
                "Invalid class schedule ID."
            )
        }

        return try {

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