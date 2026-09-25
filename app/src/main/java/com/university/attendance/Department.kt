package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single department document stored in the "departments" collection.
 *
 * Firestore path: departments/{departmentId}
 *
 * Kept intentionally simple -- only the department name is stored, as requested.
 * departmentId is the Firestore auto-generated document ID, used for
 * Update/Delete operations (CRUD).
 */
data class Department(
    @get:Exclude @set:Exclude var departmentId: String = "", // Firestore doc ID

    var name: String = "",

    @ServerTimestamp
    var createdAt: Date? = null
) {
    /** No-arg constructor required by Firestore for automatic deserialization. */
    constructor() : this(departmentId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}