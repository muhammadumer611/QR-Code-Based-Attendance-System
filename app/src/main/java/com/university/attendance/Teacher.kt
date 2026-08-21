package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Teacher(
    // Our application's permanent Teacher ID.
    // Example: TCH-8F3K2A91
    @get:Exclude
    @set:Exclude
    var teacherId: String = "",

    // Firebase Authentication UID.
    // This will be linked when teacher creates/logs into account.
    var authUid: String = "",

    var email: String = "",
    var departmentName: String = "",
    var designation: String = "",
    var mainSubject: String = "",
    var fullName: String = "",
    var fatherName: String = "",
    var cnicNumber: String = "",
    var fatherCnicNumber: String = "",
    var contactNumber: String = "",

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    // Important:
    // Admin creates teacher first.
    // Account can initially be NOT linked.
    var accountLinked: Boolean = false,

    @ServerTimestamp
    var createdAt: Date? = null,

    var linkedAt: Date? = null
) {
    constructor() : this(teacherId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "teacherId" to teacherId,
        "authUid" to authUid,
        "email" to email,
        "departmentName" to departmentName,
        "designation" to designation,
        "mainSubject" to mainSubject,
        "fullName" to fullName,
        "fatherName" to fatherName,
        "cnicNumber" to cnicNumber,
        "fatherCnicNumber" to fatherCnicNumber,
        "contactNumber" to contactNumber,
        "isActive" to isActive,
        "accountLinked" to accountLinked,
        "createdAt" to FieldValue.serverTimestamp(),
        "linkedAt" to linkedAt
    )
}