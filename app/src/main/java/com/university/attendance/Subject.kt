package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single subject document stored in the "subjects" collection.
 *
 * Firestore path: subjects/{subjectId}
 *
 * Hierarchy: Department -> Program -> Semester -> Subject
 * (e.g. Computer Science -> BSSE -> Semester 3 -> "Data Structures", CS-201, 3 credit hours)
 *
 * UPDATED: now also stores which teacher is assigned to teach this
 * specific subject (teacherId + teacherName), since a university course
 * catalogue has a distinct teacher per subject -- unlike a school system
 * where one class teacher covers everything. A teacher can be assigned to
 * multiple subjects; each subject has exactly one assigned teacher at a
 * time (assigning a new one replaces the previous assignment).
 *
 * teacherId is empty ("") until an Admin assigns a teacher via the
 * Teacher-Subject Assignment screen -- until then, "Not assigned" is
 * shown wherever this subject's teacher would normally display.
 */
data class Subject(
    @get:Exclude @set:Exclude var subjectId: String = "", // Firestore doc ID

    var departmentId: String = "",
    var departmentName: String = "",
    var programName: String = "",      // e.g. BSSE, BSCS
    var semester: Int = 1,             // 1 to 8

    var courseCode: String = "",       // e.g. CS-201
    var subjectName: String = "",      // e.g. Data Structures & Algorithms
    var creditHours: Int = 3,

    var teacherId: String = "",        // "" until assigned
    var teacherName: String = "",      // "" until assigned

    @ServerTimestamp
    var createdAt: Date? = null
) {
    /** No-arg constructor required by Firestore for automatic deserialization. */
    constructor() : this(subjectId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(
        "departmentId" to departmentId,
        "departmentName" to departmentName,
        "programName" to programName,
        "semester" to semester,
        "courseCode" to courseCode,
        "subjectName" to subjectName,
        "creditHours" to creditHours,
        "teacherId" to teacherId,
        "teacherName" to teacherName,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}