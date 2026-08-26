package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ClassSchedule(

    // Firestore document ID
    @get:Exclude
    @set:Exclude
    var scheduleId: String = "",

    // ------------------------------------------------------------
    // TEACHER
    // ------------------------------------------------------------

    var teacherAuthUid: String = "",

    var teacherName: String = "",

    // ------------------------------------------------------------
    // CLASS INFORMATION
    // ------------------------------------------------------------

    var className: String = "",

    var subjectName: String = "",

    var roomNumber: String = "",

    // ------------------------------------------------------------
    // DATE
    // ------------------------------------------------------------

    // Example:
    // 2026-08-26
    var date: String = "",

    // Example:
    // Wednesday
    var dayName: String = "",

    // ------------------------------------------------------------
    // TIME
    // ------------------------------------------------------------

    // Example:
    // 09:00 AM
    var startTime: String = "",

    // Example:
    // 10:00 AM
    var endTime: String = "",

    // ------------------------------------------------------------
    // SEMESTER
    // ------------------------------------------------------------

    // Example:
    // 1st Semester
    var semester: String = "",

    // ------------------------------------------------------------
    // OPTIONAL NOTE
    // ------------------------------------------------------------

    var note: String = "",

    // ------------------------------------------------------------
    // ADMIN
    // ------------------------------------------------------------

    var createdBy: String = "Admin",

    @ServerTimestamp
    var createdAt: Date? = null

) {

    constructor() : this(scheduleId = "")

    // ============================================================
    // FIRESTORE MAP
    // ============================================================

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(

            "teacherAuthUid" to teacherAuthUid,

            "teacherName" to teacherName,

            "className" to className,

            "subjectName" to subjectName,

            "roomNumber" to roomNumber,

            "date" to date,

            "dayName" to dayName,

            "startTime" to startTime,

            "endTime" to endTime,

            "semester" to semester,

            "note" to note,

            "createdBy" to createdBy,

            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}