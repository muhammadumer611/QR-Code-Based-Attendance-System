package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single student document stored in the "students" collection.
 *
 * Firestore path: students/{studentId}
 *
 * classId is a composite key built from university + department + program +
 * session + section (e.g. "COMSATS_CS_BSSE_2022_A"). It is used to group
 * students into the same class automatically -- see StudentRepository.
 */
data class Student(
    @get:Exclude @set:Exclude var studentId: String = "", // Firestore doc ID, filled after fetch/save

    var universityName: String = "",
    var departmentName: String = "",
    var programName: String = "",      // e.g. BSSE, BSCS, BSIT
    var session: String = "",          // e.g. 2022, 2023, Fall-2022
    var section: String = "",          // e.g. A, B, C

    var classId: String = "",          // auto-generated grouping key, see ClassUtils

    var fullName: String = "",
    var contactNumber: String = "",
    var cnicNumber: String = "",
    var fatherCnicNumber: String = "",
    var guardianNumber: String = "",
    var regNo: String = "",

    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,

    @ServerTimestamp
    var createdAt: Date? = null
) {
    /** No-arg constructor required by Firestore for automatic deserialization. */
    constructor() : this(studentId = "")

    /**
     * Converts this object into a Map for Firestore writes.
     * We exclude studentId since it's the document ID, not a field.
     */
    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "universityName" to universityName,
        "departmentName" to departmentName,
        "programName" to programName,
        "session" to session,
        "section" to section,
        "classId" to classId,
        "fullName" to fullName,
        "contactNumber" to contactNumber,
        "cnicNumber" to cnicNumber,
        "fatherCnicNumber" to fatherCnicNumber,
        "guardianNumber" to guardianNumber,
        "regNo" to regNo,
        "isActive" to isActive,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}