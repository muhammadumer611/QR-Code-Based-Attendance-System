package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Manual Update (Admin) screen:
 *   Department -> Class -> Subject -> Date -> class register (toggle Present/Absent) -> Save
 *
 * Reuses the SAME "attendance_records" collection and composite-key scheme
 * (studentId_subjectId_date) used by the (future) QR scan flow. This means:
 *   - A manually-marked Present and a QR-scanned Present are
 *     indistinguishable in storage (both are just a "present" document),
 *     so Admin's Attendance Summary screens work correctly no matter which
 *     method produced the record.
 *   - Editing an existing date just overwrites the same document
 *     (same composite key), it never creates duplicates.
 *   - Un-marking a student back to Absent DELETES their present record
 *     for that date (since Absent is never stored), keeping the "no
 *     separate Absent documents" rule consistent everywhere in the app.
 */
class ManualAttendanceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val studentsRef = firestore.collection("students")
    private val attendanceRef = firestore.collection("attendance_records")

    sealed class OpResult {
        object Success : OpResult()
        data class Error(val message: String, val exception: Exception? = null) : OpResult()
    }

    /**
     * Loads every student in the class, each paired with whether they
     * already have a "present" record for this exact subject+date
     * (so the register pre-fills correctly when editing an existing date).
     */
    suspend fun loadRoster(classId: String, subjectId: String, date: String): List<AttendanceRosterRow> {
        val studentsSnapshot = studentsRef
            .whereEqualTo("classId", classId)
            .get()
            .await()
        val students = studentsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Student::class.java)?.apply { studentId = doc.id }
        }.sortedBy { it.fullName }

        val existingSnapshot = attendanceRef
            .whereEqualTo("classId", classId)
            .whereEqualTo("subjectId", subjectId)
            .whereEqualTo("date", date)
            .get()
            .await()
        val presentStudentIds = existingSnapshot.documents
            .mapNotNull { it.getString("studentId") }
            .toSet()

        return students.map { student ->
            AttendanceRosterRow(
                student = student,
                isPresent = presentStudentIds.contains(student.studentId)
            )
        }
    }

    /**
     * Saves the whole register in one batch write:
     *   - Present rows  -> set (create/overwrite) their attendance_records doc
     *   - Absent rows   -> delete their attendance_records doc, if one exists
     *     (Absent is never stored)
     *
     * Uses a single Firestore WriteBatch so the whole class's attendance
     * for this subject+date saves atomically (all rows succeed together,
     * or none do) -- avoids a half-saved register if the network drops
     * partway through.
     */
    suspend fun saveRoster(
        rows: List<AttendanceRosterRow>,
        subjectId: String,
        subjectName: String,
        courseCode: String,
        teacherName: String,
        classId: String,
        departmentId: String,
        departmentName: String,
        date: String
    ): OpResult {
        return try {
            val batch = firestore.batch()

            rows.forEach { row ->
                val recordId = "${row.student.studentId}_${subjectId}_$date"
                val docRef = attendanceRef.document(recordId)

                if (row.isPresent) {
                    val record = AttendanceRecord(
                        studentId = row.student.studentId,
                        studentName = row.student.fullName,
                        regNo = row.student.regNo,
                        subjectId = subjectId,
                        subjectName = subjectName,
                        courseCode = courseCode,
                        teacherName = teacherName,
                        classId = classId,
                        departmentId = departmentId,
                        departmentName = departmentName,
                        date = date,
                        status = "present",
                        sessionId = "manual" // marks this as manually entered, not from a QR session
                    )
                    batch.set(docRef, mapOf(
                        "studentId" to record.studentId,
                        "studentName" to record.studentName,
                        "regNo" to record.regNo,
                        "subjectId" to record.subjectId,
                        "subjectName" to record.subjectName,
                        "courseCode" to record.courseCode,
                        "teacherName" to record.teacherName,
                        "classId" to record.classId,
                        "departmentId" to record.departmentId,
                        "departmentName" to record.departmentName,
                        "date" to record.date,
                        "status" to record.status,
                        "markedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "sessionId" to record.sessionId
                    ))
                } else {
                    // Absent -> remove any existing present record for this
                    // student+subject+date (safe even if none exists).
                    batch.delete(docRef)
                }
            }

            batch.commit().await()
            OpResult.Success
        } catch (e: Exception) {
            OpResult.Error(e.message ?: "Failed to save attendance.", e)
        }
    }
}