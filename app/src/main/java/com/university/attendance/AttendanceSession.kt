package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class AttendanceSession(

    @get:Exclude
    @set:Exclude
    var sessionId: String = "",

    var scheduleId: String = "",

    var teacherId: String = "",
    var teacherAuthUid: String = "",
    var assignmentId: String = "",
    var teacherName: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var classId: String = "",

    var departmentName: String = "",
    var programName: String = "",
    var semester: Int = 1,
    var session: String = "",
    var section: String = "",
    var className: String = "",
    var roomNumber: Int = 0,

    var date: String = "",
    var dayName: String = "",

    var startTime: String = "",
    var endTime: String = "",

    var qrPayload: String = "",

    var isActive: Boolean = true,

    var status: String = "active",

    @ServerTimestamp
    var createdAt: Date? = null,

    var expiresAt: Date? = null
) {

    constructor() : this(
        sessionId = ""
    )
}