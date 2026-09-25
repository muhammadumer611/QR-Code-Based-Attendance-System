package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents one entry in the "notifications" collection -- similar to
 * ActivityLog, but user-facing (shown in the Notifications card/badge)
 * and carries a read/unread state.
 */
data class AppNotification(
    @get:Exclude @set:Exclude var notificationId: String = "",

    var type: String = "",         // see ActivityType constants
    var title: String = "",
    var description: String = "",

    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,

    @ServerTimestamp
    var timestamp: Date? = null
) {
    constructor() : this(notificationId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type,
        "title" to title,
        "description" to description,
        "isRead" to isRead,
        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}