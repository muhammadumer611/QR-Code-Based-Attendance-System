package com.university.attendance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TeacherAttendanceRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val sessionsRef = firestore.collection("attendanceSessions")
    private val teachersRef = firestore.collection("teachers")
    private val studentsRef = firestore.collection("students")
    private val assignmentsRef = firestore.collection("teacherSubjectAssignments")
    private val attendanceRef = firestore.collection("attendance_records")

    suspend fun createSession(schedule: ClassSchedule): AttendanceSession {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Teacher is not logged in.")
        val teacherDoc = teachersRef.whereEqualTo("authUid", uid).limit(1).get().await().documents.firstOrNull()
            ?: throw Exception("Teacher profile not found.")
        val teacherId = teacherDoc.id
        if (teacherId != schedule.teacherId) throw Exception("You are not authorized to start attendance for this class.")
        if (schedule.scheduleId.isBlank() || schedule.classId.isBlank() || schedule.subjectId.isBlank()) throw Exception("Class schedule identity is incomplete.")
        val exactAssignment = assignmentsRef.whereEqualTo("teacherId", teacherId).get().await().documents.any {
            it.getString("classId") == schedule.classId && it.getString("subjectId") == schedule.subjectId &&
                it.getString("session") == schedule.session.trim() &&
                (it.getLong("semester")?.toInt() ?: it.getString("semester")?.toIntOrNull()) == schedule.semester
        }
        if (!exactAssignment) throw Exception("This teacher is not assigned to this class and subject.")

        val now = Date(); val today = todayDate(); val todayDay = todayDayName()
        val validToday = (schedule.periodType.equals("Daily", true) && schedule.date == today) ||
            (schedule.periodType.equals("Weekly", true) && schedule.dayName.equals(todayDay, true))
        if (!validToday) throw Exception("This class is not scheduled for today.")
        val start = parseTodayTime(schedule.startTime) ?: throw Exception("Invalid class start time.")
        val end = parseTodayTime(schedule.endTime) ?: throw Exception("Invalid class end time.")
        if (now.time < start.time - 5 * 60 * 1000L) throw Exception("Attendance can be started 5 minutes before class.")
        if (now.time > end.time) throw Exception("This class has already ended.")

        val existingDocs = sessionsRef.whereEqualTo("scheduleId", schedule.scheduleId).get().await().documents
        val existing = existingDocs.firstOrNull {
            (it.getBoolean("isActive") ?: false) &&
                (it.getString("status") ?: "active").equals("active", true) &&
                (it.getDate("expiresAt")?.time ?: 0L) > now.time
        }
        existingDocs.filter { it.id != existing?.id && (it.getBoolean("isActive") ?: false) && (it.getDate("expiresAt")?.time ?: 0L) <= now.time }
            .forEach { it.reference.update("isActive", false, "status", "ended") }
        if (existing != null) return AttendanceSession(
            sessionId = existing.id,
            scheduleId = existing.getString("scheduleId").orEmpty(), teacherId = existing.getString("teacherId").orEmpty(),
            teacherAuthUid = existing.getString("teacherAuthUid").orEmpty(), teacherName = existing.getString("teacherName").orEmpty(),
            subjectId = existing.getString("subjectId").orEmpty(), subjectName = existing.getString("subjectName").orEmpty(),
            courseCode = existing.getString("courseCode").orEmpty(), classId = existing.getString("classId").orEmpty(),
            departmentName = existing.getString("departmentName").orEmpty(), programName = existing.getString("programName").orEmpty(),
            semester = existing.getLong("semester")?.toInt() ?: 1, session = existing.getString("session").orEmpty(),
            section = existing.getString("section").orEmpty(), className = existing.getString("className").orEmpty(),
            roomNumber = existing.getLong("roomNumber")?.toInt() ?: 0, date = existing.getString("date").orEmpty(),
            dayName = existing.getString("dayName").orEmpty(), startTime = existing.getString("startTime").orEmpty(),
            endTime = existing.getString("endTime").orEmpty(), qrPayload = existing.getString("qrPayload").orEmpty(),
            isActive = true, status = existing.getString("status") ?: "active", expiresAt = existing.getDate("expiresAt")
        )

        val sessionId = UUID.randomUUID().toString()
        val payload = "UOL_ATTENDANCE|$sessionId"
        val data = mapOf(
            "scheduleId" to schedule.scheduleId, "teacherId" to teacherId, "teacherAuthUid" to uid, "teacherName" to schedule.teacherName,
            "subjectId" to schedule.subjectId, "subjectName" to schedule.subjectName, "courseCode" to schedule.courseCode,
            "classId" to schedule.classId, "className" to schedule.className, "departmentName" to schedule.departmentName,
            "programName" to schedule.programName, "semester" to schedule.semester, "session" to schedule.session,
            "section" to schedule.section, "roomNumber" to schedule.roomNumber, "date" to today, "dayName" to todayDay,
            "startTime" to schedule.startTime, "endTime" to schedule.endTime, "qrPayload" to payload,
            "isActive" to true, "status" to "active", "createdAt" to FieldValue.serverTimestamp(), "expiresAt" to end
        )
        sessionsRef.document(sessionId).set(data).await()
        return AttendanceSession(sessionId = sessionId, scheduleId = schedule.scheduleId, teacherId = teacherId, teacherAuthUid = uid,
            teacherName = schedule.teacherName, subjectId = schedule.subjectId, subjectName = schedule.subjectName, courseCode = schedule.courseCode,
            classId = schedule.classId, departmentName = schedule.departmentName, programName = schedule.programName, semester = schedule.semester,
            session = schedule.session, section = schedule.section, className = schedule.className, roomNumber = schedule.roomNumber,
            date = today, dayName = todayDay, startTime = schedule.startTime, endTime = schedule.endTime, qrPayload = payload,
            isActive = true, status = "active", expiresAt = end)
    }

    suspend fun endSession(sessionId: String) { if (sessionId.isNotBlank()) sessionsRef.document(sessionId).update("isActive", false, "status", "ended").await() }

    suspend fun getRosterCount(classId: String): Int = if (classId.isBlank()) 0 else studentsRef.whereEqualTo("classId", classId).whereEqualTo("isActive", true).get().await().size()
    suspend fun getPresentCount(sessionId: String): Int = if (sessionId.isBlank()) 0 else attendanceRef.whereEqualTo("sessionId", sessionId).whereEqualTo("status", "present").get().await().size()

    private fun todayDate() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun todayDayName() = SimpleDateFormat("EEEE", Locale.US).format(Date())
    private fun parseTodayTime(time: String): Date? = try {
        val parsed = SimpleDateFormat("hh:mm a", Locale.US).parse(time) ?: return null
        val now = java.util.Calendar.getInstance(); val c = java.util.Calendar.getInstance(); c.time = parsed
        now.set(java.util.Calendar.HOUR_OF_DAY, c.get(java.util.Calendar.HOUR_OF_DAY)); now.set(java.util.Calendar.MINUTE, c.get(java.util.Calendar.MINUTE)); now.set(java.util.Calendar.SECOND, 0); now.set(java.util.Calendar.MILLISECOND, 0); now.time
    } catch (_: Exception) { null }
}
