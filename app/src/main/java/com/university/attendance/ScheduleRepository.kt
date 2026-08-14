package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles the "schedule" collection -- kept intentionally simple as a
 * SINGLE fixed document ("current"), since there's one active schedule
 * file at a time (matching the "Class Schedule" card already in your
 * Admin Dashboard XML).
 */
class ScheduleRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val scheduleRef = firestore.collection("schedule").document("current")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String, val exception: Exception? = null) : OpResult()
    }

    /** Fetches the current schedule record, or null if none has been uploaded yet. */
    suspend fun getCurrentSchedule(): Schedule? {
        val doc = scheduleRef.get().await()
        return if (doc.exists()) doc.toObject(Schedule::class.java)?.apply { scheduleId = doc.id } else null
    }

    /** Saves (creates or overwrites) the current schedule record. */
    suspend fun saveSchedule(fileName: String, note: String): OpResult {
        return try {
            val schedule = Schedule(fileName = fileName.trim(), note = note.trim())
            scheduleRef.set(schedule.toMap()).await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to save schedule.", e)
        }
    }
}