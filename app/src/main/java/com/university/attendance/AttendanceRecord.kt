package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a single attendance record stored in "attendance_records".
 *
 * Firestore path: attendance_records/{recordId}
 * recordId format: "{studentId}_{subjectId}_{date}"  (composite key)
 *
 * Only PRESENT records are ever written here (see architecture plan --
 * Absent is calculated, never stored). This will be written automatically
 * when a student scans a teacher's QR code (future work); for now this
 * model exists so Admin's attendance screens can read whatever records
 * do exist.
 */
data class AttendanceRecord(
    @get:Exclude @set:Exclude var recordId: String = "",

    var studentId: String = "",
    var studentName: String = "",
    var regNo: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var teacherId: String = "",
    var teacherName: String = "",

    var classId: String = "",
    var departmentId: String = "",
    var departmentName: String = "",

    var date: String = "",          // yyyy-MM-dd
    var status: String = "present", // only "present" is ever written

    @ServerTimestamp
    var markedAt: Date? = null,

    var sessionId: String = ""
) {
    constructor() : this(recordId = "")
}