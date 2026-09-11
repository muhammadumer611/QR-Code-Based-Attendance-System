package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue

/**
 * Exact enrollment:
 *
 * Student
 * + Class
 * + Subject
 * + Teacher
 * + Semester
 * + Session
 */
data class StudentSubjectAssignment(

    @get:Exclude
    @set:Exclude
    var assignmentId: String = "",

    var studentId: String = "",

    var studentName: String = "",

    var studentGeneratedId: String = "",

    var classId: String = "",

    var departmentName: String = "",

    var programName: String = "",

    var semester: Int = 1,

    var session: String = "",

    var section: String = "",

    var subjectId: String = "",

    var subjectName: String = "",

    var courseCode: String = "",

    var teacherId: String = "",

    var teacherName: String = "",

    var createdAt: Any? = null,

    var updatedAt: Any? = null

) {

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(

            "studentId" to
                    studentId,

            "studentName" to
                    studentName,

            "studentGeneratedId" to
                    studentGeneratedId,

            "classId" to
                    classId,

            "departmentName" to
                    departmentName,

            "programName" to
                    programName,

            "semester" to
                    semester,

            "session" to
                    session,

            "section" to
                    section,

            "subjectId" to
                    subjectId,

            "subjectName" to
                    subjectName,

            "courseCode" to
                    courseCode,

            "teacherId" to
                    teacherId,

            "teacherName" to
                    teacherName,

            "createdAt" to
                    FieldValue.serverTimestamp(),

            "updatedAt" to
                    FieldValue.serverTimestamp()
        )
    }
}