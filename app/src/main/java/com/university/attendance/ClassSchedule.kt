package com.university.attendance

data class ClassSchedule(

    var scheduleId: String = "",

    var teacherId: String = "",
    var teacherName: String = "",
    var teacherAuthUid: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var departmentName: String = "",
    var programName: String = "",
    var className: String = "",
    var semester: String = "",
    var session: String = "",

    var date: String = "",
    var dayOfWeek: String = "",

    var startTime: String = "",
    var endTime: String = "",

    var roomNumber: String = "",

    // Daily / Weekly / Semester
    var scheduleType: String = "Daily",

    // PDF / notes
    var pdfUrl: String = "",
    var pdfName: String = "",

    var createdAt: Long = System.currentTimeMillis()
)