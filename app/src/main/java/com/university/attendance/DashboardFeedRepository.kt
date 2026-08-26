package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Powers the Admin Dashboard's "Recent Activities" RecyclerView and the
 * Notifications list -- both read from collections that ActivityLogHelper
 * writes to automatically whenever another repository logs an action.
 */
class DashboardFeedRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val activityLogsRef = firestore.collection("activity_logs")
    private val notificationsRef = firestore.collection("notifications")

    /** Fetches the most recent activity log entries, newest first. */
    suspend fun getRecentActivities(limit: Long = 20): List<Log> {
        val snapshot = activityLogsRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Log::class.java)?.apply { logId = doc.id }
        }
    }

    /** Fetches the most recent notifications, newest first. */
    suspend fun getNotifications(limit: Long = 30): List<AppNotification> {
        val snapshot = notificationsRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(AppNotification::class.java)?.apply { notificationId = doc.id }
        }
    }

    /** Counts unread notifications, for the badge shown on the Dashboard bell icon. */
    suspend fun getUnreadNotificationCount(): Int {
        val snapshot = notificationsRef
            .whereEqualTo("isRead", false)
            .get()
            .await()
        return snapshot.size()
    }

    /** Marks a single notification as read. */
    suspend fun markNotificationRead(notificationId: String) {
        try {
            notificationsRef.document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            // Non-critical -- if this fails, the notification just stays
            // unread until the next successful tap; no user-facing error needed.
        }
    }
}