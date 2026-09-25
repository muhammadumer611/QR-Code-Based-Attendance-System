package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Shared utility called from OTHER repositories (Student, Teacher,
 * Department, Subject, Attendance, TeacherSubject) right after a
 * successful save -- writes ONE activity_logs entry AND ONE
 * notifications entry in a single call, so both "Recent Activities" and
 * "Notifications" stay in sync automatically without duplicating logic
 * in every repository.
 *
 * Usage pattern in an existing repository, right after a successful
 * Firestore write:
 *     ActivityLogHelper.log(
 *         type = ActivityType.STUDENT_ADDED,
 *         title = "Student Added",
 *         description = "Ahmed Raza added to BSSE - Section A"
 *     )
 *
 * This is fire-and-forget by design (failures are swallowed, never
 * thrown) -- a logging failure should never cause the ORIGINAL action
 * (e.g. adding a student) to fail or roll back.
 */
object ActivityLogHelper {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun log(type: String, title: String, description: String) {
        try {
            val activityLog = Log(type = type, title = title, description = description)
            firestore.collection("activity_logs").document().set(activityLog.toMap()).await()

            val notification = AppNotification(type = type, title = title, description = description)
            firestore.collection("notifications").document().set(notification.toMap()).await()
        } catch (e: Exception) {
            // Intentionally swallowed -- see class-level comment. The
            // action that triggered this log (e.g. adding a student)
            // has already succeeded and must not be affected by a
            // logging failure.
        }
    }
}