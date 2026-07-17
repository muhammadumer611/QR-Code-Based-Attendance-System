package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single subject document stored in the "subjects" collection.
 *
 * Firestore path: subjects/{subjectId}
 *
 * Hierarchy this belongs to: Department -> Program -> Semester -> Subject
 * (e.g. Computer Science -> BSSE -> Semester 3 -> "Data Structures", CS-201, 3 credit hours)
 *
 * departmentId is stored alongside departmentName so the subject stays
 * correctly linked even if a department's name is later edited via
 * Department Management (departmentId never changes, departmentName might).
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
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
}