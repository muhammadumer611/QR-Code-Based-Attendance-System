package com.university.attendance

import android.graphics.Bitmap
import android.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceSessionRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()
) {

    private val schedulesRef =
        firestore.collection("classSchedules")

    private val sessionsRef =
        firestore.collection("attendanceSessions")

    // ============================================================
    // CREATE / GET TODAY SESSION
    // ============================================================

    suspend fun createOrGetTodaySession(
        scheduleId: String,
        teacherId: String
    ): AttendanceSession {

        val uid =
            auth.currentUser?.uid
                ?: throw IllegalStateException(
                    "Teacher is not signed in."
                )

        if (
            scheduleId.isBlank() ||
            teacherId.isBlank()
        ) {
            throw IllegalArgumentException(
                "Class information is missing."
            )
        }

        // --------------------------------------------------------
        // LOAD SCHEDULE
        // --------------------------------------------------------

        val scheduleDoc =
            schedulesRef
                .document(scheduleId)
                .get()
                .await()

        if (!scheduleDoc.exists()) {

            throw IllegalStateException(
                "This class schedule no longer exists."
            )
        }

        val schedule =
            ClassSchedule.fromDocument(
                scheduleDoc
            )

        // --------------------------------------------------------
        // VERIFY TEACHER
        // --------------------------------------------------------

        if (
            schedule.teacherId != teacherId
        ) {

            throw IllegalStateException(
                "This class is not assigned to the current teacher."
            )
        }

        if (
            schedule.teacherAuthUid.isNotBlank() &&
            schedule.teacherAuthUid != uid
        ) {

            throw IllegalStateException(
                "Teacher account does not match this class."
            )
        }

        // --------------------------------------------------------
        // OLD SCHEDULE FALLBACK
        // --------------------------------------------------------

        var departmentName =
            schedule.departmentName

        var programName =
            schedule.programName

        var semester =
            schedule.semester

        /*
         * Agar purani schedule mein departmentName
         * save nahi hua tha to Subject se recover kar lenge.
         */

        if (
            departmentName.isBlank() ||
            programName.isBlank()
        ) {

            val subjectDoc =
                firestore
                    .collection("subjects")
                    .document(schedule.subjectId)
                    .get()
                    .await()

            if (subjectDoc.exists()) {

                departmentName =
                    subjectDoc
                        .getString("departmentName")
                        .orEmpty()
                        .ifBlank {
                            departmentName
                        }

                programName =
                    subjectDoc
                        .getString("programName")
                        .orEmpty()
                        .ifBlank {
                            programName
                        }

                semester =
                    subjectDoc
                        .getString("semester")
                        ?.toIntOrNull()
                        ?: semester
            }
        }

        // --------------------------------------------------------
        // TODAY
        // --------------------------------------------------------

        val today =
            todayDate()

        val todayDay =
            todayDayName()

        val isToday =
            schedule.date == today ||

                    (
                            schedule.periodType
                                .equals(
                                    "Weekly",
                                    ignoreCase = true
                                ) &&

                                    schedule.dayName
                                        .equals(
                                            todayDay,
                                            ignoreCase = true
                                        )
                            )

        if (!isToday) {

            throw IllegalStateException(
                "QR can only be generated for today's class."
            )
        }

        // --------------------------------------------------------
        // CHECK EXISTING ACTIVE SESSION
        // --------------------------------------------------------

        val existing =
            sessionsRef
                .whereEqualTo(
                    "scheduleId",
                    scheduleId
                )
                .whereEqualTo(
                    "date",
                    today
                )
                .whereEqualTo(
                    "status",
                    "active"
                )
                .limit(1)
                .get()
                .await()

        if (!existing.isEmpty) {

            val document =
                existing.documents.first()

            val oldSession =
                document.toObject(
                    AttendanceSession::class.java
                )

            if (oldSession != null) {

                oldSession.sessionId =
                    document.id

                if (
                    oldSession.expiresAt != null &&
                    oldSession.expiresAt!!
                        .after(Date())
                ) {

                    return oldSession
                }

                document.reference
                    .update(
                        "status",
                        "expired"
                    )
                    .await()
            }
        }

        // --------------------------------------------------------
        // VERIFY TEACHER DOCUMENT
        // --------------------------------------------------------

        val teacherDoc =
            firestore
                .collection("teachers")
                .document(teacherId)
                .get()
                .await()

        if (!teacherDoc.exists()) {

            throw IllegalStateException(
                "Teacher profile not found."
            )
        }

        val storedAuthUid =
            teacherDoc
                .getString("authUid")
                .orEmpty()

        if (
            storedAuthUid.isNotBlank() &&
            storedAuthUid != uid
        ) {

            throw IllegalStateException(
                "Teacher account verification failed."
            )
        }

        // --------------------------------------------------------
        // NEW SESSION
        // --------------------------------------------------------

        val sessionId =
            UUID.randomUUID().toString()

        val expiry =
            calculateExpiry(
                schedule.endTime
            )

        val session =
            AttendanceSession(

                sessionId =
                    sessionId,

                scheduleId =
                    schedule.scheduleId,

                teacherId =
                    schedule.teacherId,

                teacherAuthUid =
                    uid,

                teacherName =
                    schedule.teacherName,

                subjectId =
                    schedule.subjectId,

                subjectName =
                    schedule.subjectName,

                courseCode =
                    schedule.courseCode,

                departmentName =
                    departmentName,

                programName =
                    programName,

                semester =
                    semester,

                session =
                    schedule.session,

                section =
                    schedule.section,

                className =
                    schedule.className,

                roomNumber =
                    schedule.roomNumber,

                date =
                    today,

                dayName =
                    todayDay,

                startTime =
                    schedule.startTime,

                endTime =
                    schedule.endTime,

                status =
                    "active",

                expiresAt =
                    expiry
            )

        val data =
            mapOf(

                "scheduleId" to
                        schedule.scheduleId,

                "teacherId" to
                        schedule.teacherId,

                "teacherAuthUid" to
                        uid,

                "teacherName" to
                        schedule.teacherName,

                "subjectId" to
                        schedule.subjectId,

                "subjectName" to
                        schedule.subjectName,

                "courseCode" to
                        schedule.courseCode,

                "departmentName" to
                        departmentName,

                "programName" to
                        programName,

                "semester" to
                        semester,

                "session" to
                        schedule.session,

                "section" to
                        schedule.section,

                "className" to
                        schedule.className,

                "roomNumber" to
                        schedule.roomNumber,

                "date" to
                        today,

                "dayName" to
                        todayDay,

                "startTime" to
                        schedule.startTime,

                "endTime" to
                        schedule.endTime,

                "status" to
                        "active",

                "createdAt" to
                        FieldValue.serverTimestamp(),

                "expiresAt" to
                        expiry
            )

        sessionsRef
            .document(sessionId)
            .set(data)
            .await()

        return session
    }

    // ============================================================
    // GET ACTIVE SESSION
    // ============================================================

    suspend fun getActiveSession(
        sessionId: String
    ): AttendanceSession? {

        if (
            sessionId.isBlank()
        ) {
            return null
        }

        val document =
            sessionsRef
                .document(sessionId)
                .get()
                .await()

        if (!document.exists()) {
            return null
        }

        val session =
            document.toObject(
                AttendanceSession::class.java
            )
                ?: return null

        session.sessionId =
            document.id

        if (
            session.status != "active"
        ) {
            return null
        }

        if (
            session.date != todayDate()
        ) {
            return null
        }

        val expiry =
            session.expiresAt

        if (
            expiry != null &&
            !expiry.after(Date())
        ) {
            return null
        }

        return session
    }

    // ============================================================
    // MARK ATTENDANCE
    // ============================================================

    suspend fun markAttendance(
        session: AttendanceSession,
        student: Student
    ) {

        if (
            session.sessionId.isBlank()
        ) {
            throw IllegalArgumentException(
                "Invalid attendance QR."
            )
        }

        if (!student.isActive) {

            throw IllegalStateException(
                "Your student account is inactive."
            )
        }

        if (
            student.studentId.isBlank()
        ) {

            throw IllegalStateException(
                "Student profile is incomplete."
            )
        }

        // --------------------------------------------------------
        // EXACT CLASS MATCH
        // --------------------------------------------------------

        val classMatches =

            student.departmentName
                .trim()
                .equals(
                    session.departmentName.trim(),
                    ignoreCase = true
                ) &&

                    student.programName
                        .trim()
                        .equals(
                            session.programName.trim(),
                            ignoreCase = true
                        ) &&

                    student.semester ==
                    session.semester &&

                    student.session
                        .trim()
                        .equals(
                            session.session.trim(),
                            ignoreCase = true
                        ) &&

                    student.section
                        .trim()
                        .equals(
                            session.section.trim(),
                            ignoreCase = true
                        )

        if (!classMatches) {

            throw IllegalStateException(
                "This QR is not for your class (${session.className})."
            )
        }

        val today =
            todayDate()

        if (
            session.date != today
        ) {

            throw IllegalStateException(
                "This attendance QR has expired."
            )
        }

        val now =
            Date()

        if (
            session.expiresAt != null &&
            !session.expiresAt!!
                .after(now)
        ) {

            throw IllegalStateException(
                "Attendance time has ended."
            )
        }

        if (
            !isClassTimeOpen(
                session.startTime,
                session.endTime
            )
        ) {

            throw IllegalStateException(
                "Attendance is only available during the scheduled class time."
            )
        }

        // --------------------------------------------------------
        // ONE ATTENDANCE PER SUBJECT PER DAY
        // --------------------------------------------------------

        val recordId =
            "${student.studentId}_${session.subjectId}_$today"
                .replace(
                    "/",
                    "_"
                )

        val recordRef =
            firestore
                .collection(
                    "attendance_records"
                )
                .document(recordId)

        val record =
            mapOf(

                "studentId" to
                        student.studentId,

                "studentName" to
                        student.fullName,

                "regNo" to
                        student.regNo,

                "subjectId" to
                        session.subjectId,

                "subjectName" to
                        session.subjectName,

                "courseCode" to
                        session.courseCode,

                "teacherId" to
                        session.teacherId,

                "teacherName" to
                        session.teacherName,

                "classId" to
                        student.classId,

                "departmentName" to
                        student.departmentName,

                "programName" to
                        student.programName,

                "semester" to
                        student.semester,

                "session" to
                        student.session,

                "section" to
                        student.section,

                "date" to
                        today,

                "status" to
                        "present",

                "markedAt" to
                        FieldValue.serverTimestamp(),

                "sessionId" to
                        session.sessionId
            )

        // --------------------------------------------------------
        // TRANSACTION
        // Prevent duplicate attendance from simultaneous scans.
        // --------------------------------------------------------

        firestore.runTransaction { transaction ->

            val existing =
                transaction.get(
                    recordRef
                )

            if (existing.exists()) {

                throw IllegalStateException(
                    "Attendance already marked for ${session.subjectName} today."
                )
            }

            transaction.set(
                recordRef,
                record
            )

            null
        }.await()
    }

    // ============================================================
    // GENERATE QR BITMAP
    // ============================================================

    fun generateQrBitmap(
        sessionId: String,
        size: Int = 760
    ): Bitmap {

        val matrix =
            MultiFormatWriter()
                .encode(
                    sessionId,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
                )

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.RGB_565
            )

        for (x in 0 until size) {

            for (y in 0 until size) {

                bitmap.setPixel(

                    x,
                    y,

                    if (
                        matrix[x, y]
                    ) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                )
            }
        }

        return bitmap
    }

    // ============================================================
    // CLASS TIME
    // ============================================================

    private fun isClassTimeOpen(
        startTime: String,
        endTime: String
    ): Boolean {

        return try {

            val format =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.US
                )

            val start =
                format.parse(
                    startTime
                )
                    ?: return true

            val end =
                format.parse(
                    endTime
                )
                    ?: return true

            val startCal =
                Calendar.getInstance().apply {
                    time = start
                }

            val endCal =
                Calendar.getInstance().apply {
                    time = end
                }

            val now =
                Calendar.getInstance()

            val startMinutes =
                startCal.get(
                    Calendar.HOUR_OF_DAY
                ) * 60 +
                        startCal.get(
                            Calendar.MINUTE
                        )

            val endMinutes =
                endCal.get(
                    Calendar.HOUR_OF_DAY
                ) * 60 +
                        endCal.get(
                            Calendar.MINUTE
                        )

            val nowMinutes =
                now.get(
                    Calendar.HOUR_OF_DAY
                ) * 60 +
                        now.get(
                            Calendar.MINUTE
                        )

            nowMinutes in
                    startMinutes..endMinutes

        } catch (
            _: Exception
        ) {

            true
        }
    }

    // ============================================================
    // EXPIRY
    // ============================================================

    private fun calculateExpiry(
        endTime: String
    ): Date {

        val now =
            Calendar.getInstance()

        try {

            val time =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.US
                ).parse(
                    endTime
                )

            if (time != null) {

                val parsed =
                    Calendar.getInstance()

                parsed.time =
                    time

                now.set(
                    Calendar.HOUR_OF_DAY,
                    parsed.get(
                        Calendar.HOUR_OF_DAY
                    )
                )

                now.set(
                    Calendar.MINUTE,
                    parsed.get(
                        Calendar.MINUTE
                    )
                )

                now.set(
                    Calendar.SECOND,
                    0
                )

                now.set(
                    Calendar.MILLISECOND,
                    0
                )

                if (
                    now.time.before(
                        Date()
                    )
                ) {

                    return Date(
                        System.currentTimeMillis() +
                                60_000L
                    )
                }

                return now.time
            }

        } catch (
            _: Exception
        ) {
        }

        return Date(
            System.currentTimeMillis() +
                    60 * 60 * 1000L
        )
    }

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Date()
        )
    }

    private fun todayDayName(): String {

        return SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(
            Date()
        )
    }
}