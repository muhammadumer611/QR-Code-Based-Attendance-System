package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents one entry in the "activity_logs" collection -- an automatic
 * record of something an Admin did (added a student, marked attendance,
 * etc.). Powers the "Recent Activities" list on the Admin Dashboard.
 *
 * type is a simple string tag (see ActivityType) used to pick the right
 * icon when rendering the list -- kept as a plain String (not an enum)
 * so it round-trips through Firestore without extra converter code.
 */
data class Log(
    @get:Exclude @set:Exclude var logId: String = "",

    var type: String = "",         // see ActivityType constants
    var title: String = "",        // e.g. "Student Added"
    var description: String = "",  // e.g. "Ahmed Raza added to BSSE - Section A"

    @ServerTimestamp
    var timestamp: Date? = null
) {
    constructor() : this(logId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type,
        "title" to title,
        "description" to description,
        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}

