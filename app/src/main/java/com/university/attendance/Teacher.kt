package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single teacher document stored in the "teachers" collection.
 *
 * Firestore path: teachers/{teacherId}
 *
 * Teachers are linked to a department (selected from existing departments
 * already used by students/classes), plus their designation and main
 * subject. This lets the Teacher Dashboard later query "which classes does
 * this teacher belong to" via departmentName.
 */
data class Teacher(
    @get:Exclude @set:Exclude var teacherId: String = "", // Firestore doc ID, filled after fetch/save

    var departmentName: String = "",
    var designation: String = "",      // Professor, Assistant Professor, Lecturer, Visiting Faculty
    var mainSubject: String = "",

    var fullName: String = "",
    var fatherName: String = "",
    var cnicNumber: String = "",
    var fatherCnicNumber: String = "",
    var contactNumber: String = "",

    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,

    @ServerTimestamp
    var createdAt: Date? = null
) {
    /** No-arg constructor required by Firestore for automatic deserialization. */
    constructor() : this(teacherId = "")

    /**
     * Converts this object into a Map for Firestore writes.
     * We exclude teacherId since it's the document ID, not a field.
     */
    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "departmentName" to departmentName,
        "designation" to designation,
        "mainSubject" to mainSubject,
        "fullName" to fullName,
        "fatherName" to fatherName,
        "cnicNumber" to cnicNumber,
        "fatherCnicNumber" to fatherCnicNumber,
        "contactNumber" to contactNumber,
        "isActive" to isActive,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}