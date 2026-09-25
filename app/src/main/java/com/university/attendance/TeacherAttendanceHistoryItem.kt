package com.university.attendance

import java.util.Date

data class TeacherAttendanceHistoryItem(

    val recordId: String = "",

    val studentId: String = "",

    val studentName: String = "",

    val regNo: String = "",

    val subjectName: String = "",

    val courseCode: String = "",

    val date: String = "",

    val status: String = "present",

    val markedAt: Date? = null,

    val sessionId: String = ""
)