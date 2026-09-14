package com.university.attendance

import com.google.firebase.firestore.Exclude

data class Subject(

    var subjectId: String = "",

    var departmentId: String = "",
    var departmentName: String = "",

    var programName: String = "",
    var semester: String = "",

    var courseCode: String = "",
    var subjectName: String = "",
    var creditHours: String = "",

    var teacherId: String = "",
    var teacherName: String = ""
) {

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(
            "departmentId" to departmentId,
            "departmentName" to departmentName,

            "programName" to programName,
            "semester" to semester,

            "courseCode" to courseCode,
            "subjectName" to subjectName,
            "creditHours" to creditHours,

            "teacherId" to teacherId,
            "teacherName" to teacherName
        )
    }
}