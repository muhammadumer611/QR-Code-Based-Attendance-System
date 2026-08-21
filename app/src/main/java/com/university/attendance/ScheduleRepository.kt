package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ScheduleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val scheduleRef =
        firestore.collection("schedule")

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
    // GET TEACHER SCHEDULE
    // ============================================================

    suspend fun getScheduleForTeacher(
        teacherAuthUid: String
    ): Schedule? {

        val snapshot = scheduleRef
            .whereEqualTo("teacherAuthUid", teacherAuthUid)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            return null
        }

        val document = snapshot.documents.first()

        return document
            .toObject(Schedule::class.java)
            ?.apply {
                scheduleId = document.id
            }
    }

    // ============================================================
    // SAVE TEACHER SCHEDULE
    // ============================================================

    suspend fun saveSchedule(
        teacherAuthUid: String,
        teacherName: String,
        fileName: String,
        note: String
    ): OpResult {

        return try {

            val existingSnapshot = scheduleRef
                .whereEqualTo("teacherAuthUid", teacherAuthUid)
                .limit(1)
                .get()
                .await()

            val schedule = Schedule(
                teacherAuthUid = teacherAuthUid,
                teacherName = teacherName,
                fileName = fileName.trim(),
                note = note.trim(),
                uploadedBy = "Admin"
            )

            if (!existingSnapshot.isEmpty) {

                val existingDocument =
                    existingSnapshot.documents.first()

                scheduleRef
                    .document(existingDocument.id)
                    .set(schedule.toMap())
                    .await()

                OpResult.Success(
                    scheduleId = existingDocument.id
                )

            } else {

                val newDocument =
                    scheduleRef.document()

                newDocument
                    .set(schedule.toMap())
                    .await()

                OpResult.Success(
                    scheduleId = newDocument.id
                )
            }

        } catch (e: Exception) {

            OpResult.Error(
                message = e.message
                    ?: "Failed to save schedule.",
                exception = e
            )
        }
    }
}