package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a "class" grouping document, stored in the "classes" collection.
 *
 * Firestore path: classes/{classId}
 *
 * A class is uniquely identified by the combination of:
 * university + department + program + session + section
 *
 * Whenever a new student is added, StudentRepository checks whether a class
 * with this exact combination already exists:
 *  - If YES  -> studentCount is incremented by 1
 *  - If NO   -> a new class document is created with studentCount = 1
 */
data class StudentClass(
    @get:Exclude @set:Exclude var classId: String = "", // Firestore doc ID (composite key)

    var universityName: String = "",
    var departmentName: String = "",
    var programName: String = "",
    var session: String = "",
    var section: String = "",

    var studentCount: Long = 0,

    @ServerTimestamp
    var createdAt: Date? = null
) {
    constructor() : this(classId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "universityName" to universityName,
        "departmentName" to departmentName,
        "programName" to programName,
        "session" to session,
        "section" to section,
        "studentCount" to studentCount,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}