package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ClassSchedule(

    @get:Exclude
    @set:Exclude
    var scheduleId: String = "",

    // ============================================================
    // TEACHER
    // ============================================================

    var teacherId: String = "",
    var teacherName: String = "",
    var teacherAuthUid: String = "",

    // ============================================================
    // CLASS
    // ============================================================

    var className: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var roomNumber: Int = 1,

    // ============================================================
    // CLASS FILTER
    // ============================================================

    var programName: String = "",
    var semester: Int = 1,
    var session: String = "",
    var section: String = "",

    // ============================================================
    // DATE
    // ============================================================

    var date: String = "",
    var dayName: String = "",

    // ============================================================
    // TIME
    // ============================================================

    var startTime: String = "",
    var endTime: String = "",

    // ============================================================
    // PERIOD
    // ============================================================

    var periodType: String = "Weekly",


    // ============================================================
    // NOTE
    // ============================================================

    var note: String = "",

    var createdBy: String = "Admin",

    @ServerTimestamp
    var createdAt: Date? = null

) {

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(

            "teacherId" to teacherId,
            "teacherName" to teacherName,
            "teacherAuthUid" to teacherAuthUid,

            "className" to className,

            "subjectId" to subjectId,
            "subjectName" to subjectName,
            "courseCode" to courseCode,

            "roomNumber" to roomNumber,

            "programName" to programName,
            "semester" to semester,
            "session" to session,
            "section" to section,

            "date" to date,
            "dayName" to dayName,

            "startTime" to startTime,
            "endTime" to endTime,

            "periodType" to periodType,

            "note" to note,

            "createdBy" to createdBy,

            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}