package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Student(

    @get:Exclude
    @set:Exclude
    var studentId: String = "",

    var studentGeneratedId: String = "",

    var universityName: String = "",
    var departmentName: String = "",
    var programName: String = "",
    var semester: Int = 1,
    var session: String = "",
    var section: String = "",

    var classId: String = "",

    var fullName: String = "",

    var personalEmail: String = "",

    var contactNumber: String = "",

    var cnicNumber: String = "",

    var fatherName: String = "",

    var fatherCnicNumber: String = "",

    var guardianNumber: String = "",

    var regNo: String = "",

    @get:Exclude
    @set:Exclude
    var authUid: String = "",

    var isActive: Boolean = true,

    @ServerTimestamp
    var createdAt: Date? = null

) {

    constructor() : this(
        studentId = ""
    )

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(

            "studentGeneratedId" to studentGeneratedId,

            "universityName" to universityName,
            "departmentName" to departmentName,
            "programName" to programName,
            "semester" to semester,
            "session" to session,
            "section" to section,

            "classId" to classId,

            "fullName" to fullName,
            "personalEmail" to personalEmail,

            "contactNumber" to contactNumber,
            "cnicNumber" to cnicNumber,

            "fatherName" to fatherName,
            "fatherCnicNumber" to fatherCnicNumber,

            "guardianNumber" to guardianNumber,

            "regNo" to regNo,

            "authUid" to authUid,

            "isActive" to isActive,

            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}