package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents the current schedule file record, stored in the "schedule"
 * collection as a SINGLE document (fixed ID "current").
 *
 * NOTE: Per current requirements, this only stores the file's NAME and an
 * optional note/link -- it does NOT store or serve an actual PDF file
 * (that requires Firebase Storage, which is intentionally deferred).
 * "View" is a placeholder until real file storage is wired in.
 */
data class Schedule(
    @get:Exclude @set:Exclude var scheduleId: String = "",

    var fileName: String = "",     // e.g. "Semester_5_Schedule.pdf"
    var note: String = "",         // optional: a link or description Admin can add
    var uploadedBy: String = "Admin",

    @ServerTimestamp
    var uploadedAt: Date? = null
) {
    constructor() : this(scheduleId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "fileName" to fileName,
        "note" to note,
        "uploadedBy" to uploadedBy,
        "uploadedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}