package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue

data class TeacherSubjectAssignment(
    @get:Exclude
    @set:Exclude
    var assignmentId: String = "",

    var teacherId: String = "",
    var teacherName: String = "",

    var classId: String = "",
    var className: String = "",

    var departmentName: String = "",
    var programName: String = "",
    var section: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var semester: Int = 1,
    var session: String = "",

    var createdAt: Any? = null,
    var updatedAt: Any? = null
) {

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "teacherId" to teacherId,
            "teacherName" to teacherName,

            "classId" to classId,
            "className" to className,

            "departmentName" to departmentName,
            "programName" to programName,
            "section" to section,

            "subjectId" to subjectId,
            "subjectName" to subjectName,
            "courseCode" to courseCode,

            "semester" to semester,
            "session" to session,

            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}