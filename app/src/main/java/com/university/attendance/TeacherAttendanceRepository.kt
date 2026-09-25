package com.university.attendance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TeacherAttendanceRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val sessionsRef =
        firestore.collection("attendanceSessions")

    private val schedulesRef =
        firestore.collection("classSchedules")

    private val teachersRef =
        firestore.collection("teachers")

    private val studentsRef =
        firestore.collection("students")

    private val assignmentsRef =
        firestore.collection("teacherSubjectAssignments")

    private val attendanceRef =
        firestore.collection("attendance_records")


    // ============================================================
    // CREATE SESSION
    // ============================================================

    suspend fun createSession(
        schedule: ClassSchedule
    ): AttendanceSession {

        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: throw Exception(
                    "Teacher is not logged in."
                )


        // ========================================================
        // TEACHER PROFILE
        // ========================================================

        val teacherDoc =
            teachersRef
                .whereEqualTo(
                    "authUid",
                    uid
                )
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: throw Exception(
                    "Teacher profile not found."
                )

        val teacherId =
            teacherDoc.id


        // ========================================================
        // TEACHER AUTHORIZATION
        // ========================================================

        if (
            teacherId != schedule.teacherId
        ) {

            throw Exception(
                "You are not authorized to start attendance for this class."
            )
        }


        // ========================================================
        // BASIC VALIDATION
        // ========================================================

        if (
            schedule.scheduleId.isBlank()
        ) {

            throw Exception(
                "Class schedule ID is missing."
            )
        }

        if (
            schedule.classId.isBlank()
        ) {

            throw Exception(
                "Class ID is missing."
            )
        }

        if (
            schedule.subjectId.isBlank()
        ) {

            throw Exception(
                "Subject ID is missing."
            )
        }


        // ========================================================
        // EXACT TEACHER ASSIGNMENT
        // ========================================================

        val exactAssignment =
            assignmentsRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()
                .documents
                .any { doc ->

                    val assignedSemester =
                        numberField(
                            doc.get("semester")
                        )

                    val assignedSession =
                        doc.getString(
                            "session"
                        )
                            .orEmpty()
                            .trim()

                    doc.getString(
                        "classId"
                    ) == schedule.classId &&

                            doc.getString(
                                "subjectId"
                            ) == schedule.subjectId &&

                            assignedSemester ==
                            schedule.semester &&

                            assignedSession.equals(
                                schedule.session.trim(),
                                ignoreCase = true
                            )
                }

        if (!exactAssignment) {

            throw Exception(
                "This teacher is not assigned to this exact class and subject."
            )
        }


        // ========================================================
        // CURRENT DATE / DAY
        // ========================================================

        val now =
            Date()

        val today =
            todayDate()

        val todayDay =
            todayDayName()


        // ========================================================
        // NORMALIZE PERIOD
        // ========================================================

        val period =
            schedule.periodType
                .trim()
                .lowercase(
                    Locale.US
                )


        // ========================================================
        // SCHEDULE VALIDATION
        // ========================================================
        //
        // Daily:
        //      exact date
        //
        // Monthly:
        //      exact date for the configured occurrence
        //
        // Weekly:
        //      day of week OR exact date
        //
        // Semester:
        //      day of week OR exact date
        //
        // ========================================================

        val scheduledDay =
            normalizeDayName(
                schedule.dayName
            )


        val currentDay =
            normalizeDayName(
                todayDay
            )


        val dateMatches =
            schedule.date
                .trim()
                .equals(
                    today,
                    ignoreCase = true
                )


        val dayMatches =
            scheduledDay.isNotBlank() &&
                    scheduledDay == currentDay


        val validToday =
            when (period) {

                "daily" ->
                    dateMatches

                "monthly" ->
                    dateMatches

                "weekly" ->
                    dateMatches || dayMatches

                "semester" ->
                    dateMatches || dayMatches

                else ->
                    dateMatches
            }


        if (!validToday) {

            throw Exception(
                buildScheduleError(
                    schedule =
                        schedule,

                    today =
                        today,

                    todayDay =
                        todayDay
                )
            )
        }


        // ========================================================
        // TIME VALIDATION
        // ========================================================

        val start =
            parseTodayTime(
                schedule.startTime
            )
                ?: throw Exception(
                    "Invalid class start time: ${schedule.startTime}"
                )

        val end =
            parseTodayTime(
                schedule.endTime
            )
                ?: throw Exception(
                    "Invalid class end time: ${schedule.endTime}"
                )


        // ========================================================
        // ALLOW 5 MINUTES BEFORE START
        // ========================================================

        val fiveMinutesBefore =
            start.time -
                    (5 * 60 * 1000L)

        if (
            now.time < fiveMinutesBefore
        ) {

            throw Exception(
                "Attendance can be started 5 minutes before class."
            )
        }


        // ========================================================
        // CLASS ALREADY ENDED
        // ========================================================

        if (
            now.time > end.time
        ) {

            throw Exception(
                "This class has already ended."
            )
        }


        // ========================================================
        // EXISTING ACTIVE SESSION
        // ========================================================

        val existingDocs =
            sessionsRef
                .whereEqualTo(
                    "scheduleId",
                    schedule.scheduleId
                )
                .get()
                .await()
                .documents


        val existing =
            existingDocs.firstOrNull {

                val active =
                    it.getBoolean(
                        "isActive"
                    ) ?: false

                val status =
                    it.getString(
                        "status"
                    )
                        ?: "active"

                val expiresAt =
                    it.getDate(
                        "expiresAt"
                    )
                        ?.time
                        ?: 0L

                active &&
                        status.equals(
                            "active",
                            ignoreCase = true
                        ) &&
                        expiresAt > now.time
            }


        // ========================================================
        // CLOSE EXPIRED SESSIONS
        // ========================================================

        existingDocs
            .filter { doc ->

                doc.id != existing?.id &&

                        (
                                doc.getBoolean(
                                    "isActive"
                                ) ?: false
                                ) &&

                        (
                                doc.getDate(
                                    "expiresAt"
                                )?.time ?: 0L
                                ) <= now.time
            }
            .forEach { doc ->

                doc.reference.update(
                    "isActive",
                    false,
                    "status",
                    "ended"
                )
            }


        // ========================================================
        // REOPEN EXISTING ACTIVE SESSION
        // ========================================================

        if (
            existing != null
        ) {

            var existingBleToken =
                existing.getString(
                    "bleToken"
                ).orEmpty()

            if (existingBleToken.isBlank()) {
                existingBleToken =
                    BleAttendanceManager.generateSessionToken()

                existing.reference
                    .update(
                        "bleToken",
                        existingBleToken
                    )
                    .await()
            }

            return AttendanceSession(

                sessionId =
                    existing.id,

                scheduleId =
                    existing.getString(
                        "scheduleId"
                    ).orEmpty(),

                teacherId =
                    existing.getString(
                        "teacherId"
                    ).orEmpty(),

                teacherAuthUid =
                    existing.getString(
                        "teacherAuthUid"
                    ).orEmpty(),

                teacherName =
                    existing.getString(
                        "teacherName"
                    ).orEmpty(),

                subjectId =
                    existing.getString(
                        "subjectId"
                    ).orEmpty(),

                subjectName =
                    existing.getString(
                        "subjectName"
                    ).orEmpty(),

                courseCode =
                    existing.getString(
                        "courseCode"
                    ).orEmpty(),

                classId =
                    existing.getString(
                        "classId"
                    ).orEmpty(),

                departmentName =
                    existing.getString(
                        "departmentName"
                    ).orEmpty(),

                programName =
                    existing.getString(
                        "programName"
                    ).orEmpty(),

                semester =
                    numberField(
                        existing.get("semester")
                    ) ?: 1,

                session =
                    existing.getString(
                        "session"
                    ).orEmpty(),

                section =
                    existing.getString(
                        "section"
                    ).orEmpty(),

                className =
                    existing.getString(
                        "className"
                    ).orEmpty(),

                roomNumber =
                    numberField(
                        existing.get("roomNumber")
                    ) ?: 0,

                date =
                    existing.getString(
                        "date"
                    ).orEmpty(),

                dayName =
                    existing.getString(
                        "dayName"
                    ).orEmpty(),

                startTime =
                    existing.getString(
                        "startTime"
                    ).orEmpty(),

                endTime =
                    existing.getString(
                        "endTime"
                    ).orEmpty(),

                qrPayload =
                    existing.getString(
                        "qrPayload"
                    ).orEmpty(),

                bleToken =
                    existingBleToken,

                isActive =
                    true,

                status =
                    existing.getString(
                        "status"
                    )
                        ?: "active",

                expiresAt =
                    existing.getDate(
                        "expiresAt"
                    )
            )
        }


        // ========================================================
        // CREATE NEW SESSION
        // ========================================================

        val sessionId =
            UUID.randomUUID()
                .toString()

        val payload =
            "UOL_ATTENDANCE|$sessionId"

        val bleToken =
            BleAttendanceManager.generateSessionToken()


        val data =
            mapOf(

                "scheduleId" to
                        schedule.scheduleId,

                "teacherId" to
                        teacherId,

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

                "classId" to
                        schedule.classId,

                "className" to
                        schedule.className,

                "departmentName" to
                        schedule.departmentName,

                "programName" to
                        schedule.programName,

                "semester" to
                        schedule.semester,

                "session" to
                        schedule.session,

                "section" to
                        schedule.section,

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

                "qrPayload" to
                        payload,

                "bleToken" to
                        bleToken,

                "isActive" to
                        true,

                "status" to
                        "active",

                "createdAt" to
                        FieldValue.serverTimestamp(),

                "expiresAt" to
                        end
            )


        sessionsRef
            .document(sessionId)
            .set(data)
            .await()


        return AttendanceSession(

            sessionId =
                sessionId,

            scheduleId =
                schedule.scheduleId,

            teacherId =
                teacherId,

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

            classId =
                schedule.classId,

            departmentName =
                schedule.departmentName,

            programName =
                schedule.programName,

            semester =
                schedule.semester,

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

            qrPayload =
                payload,

            bleToken =
                bleToken,

            isActive =
                true,

            status =
                "active",

            expiresAt =
                end
        )
    }


    // ============================================================
    // GET ACTUAL SCHEDULE FROM FIRESTORE
    // ============================================================

    suspend fun getSchedule(
        scheduleId: String
    ): ClassSchedule {

        if (
            scheduleId.isBlank()
        ) {

            throw Exception(
                "Class schedule ID is missing."
            )
        }

        val document =
            schedulesRef
                .document(scheduleId)
                .get()
                .await()


        if (
            !document.exists()
        ) {

            throw Exception(
                "Class schedule no longer exists."
            )
        }

        return ClassSchedule
            .fromDocument(
                document
            )
    }


    // ============================================================
    // END SESSION
    // ============================================================

    suspend fun endSession(
        sessionId: String
    ) {

        if (
            sessionId.isBlank()
        ) {
            return
        }

        sessionsRef
            .document(sessionId)
            .update(
                "isActive",
                false,
                "status",
                "ended"
            )
            .await()
    }


    // ============================================================
    // ROSTER
    // ============================================================

    suspend fun getRosterCount(
        classId: String
    ): Int {

        if (
            classId.isBlank()
        ) {
            return 0
        }

        return studentsRef
            .whereEqualTo(
                "classId",
                classId
            )
            .whereEqualTo(
                "isActive",
                true
            )
            .get()
            .await()
            .size()
    }


    // ============================================================
    // PRESENT COUNT
    // ============================================================

    suspend fun getPresentCount(
        sessionId: String
    ): Int {

        if (
            sessionId.isBlank()
        ) {
            return 0
        }

        return attendanceRef
            .whereEqualTo(
                "sessionId",
                sessionId
            )
            .whereEqualTo(
                "status",
                "present"
            )
            .get()
            .await()
            .size()
    }
    fun listenToSessionAttendance(
        sessionId: String,
        onChanged: (
            List<TeacherAttendanceHistoryItem>
        ) -> Unit,
        onError: (
            Exception
        ) -> Unit
    ): ListenerRegistration {

        if (
            sessionId.isBlank()
        ) {

            onChanged(
                emptyList()
            )

            return firestore
                .collection(
                    "attendance_records"
                )
                .limit(1)
                .addSnapshotListener { _, _ -> }
        }

        return attendanceRef
            .whereEqualTo(
                "sessionId",
                sessionId
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(
                        error
                    )

                    return@addSnapshotListener
                }

                if (
                    snapshot == null
                ) {

                    onChanged(
                        emptyList()
                    )

                    return@addSnapshotListener
                }

                val records =
                    snapshot.documents
                        .mapNotNull { doc ->

                            val status =
                                doc.getString(
                                    "status"
                                )
                                    .orEmpty()

                            /*
                             * Only PRESENT records should
                             * appear in teacher's live list.
                             */
                            if (
                                !status.equals(
                                    "present",
                                    ignoreCase = true
                                )
                            ) {
                                return@mapNotNull null
                            }

                            TeacherAttendanceHistoryItem(

                                recordId =
                                    doc.id,

                                studentId =
                                    doc.getString(
                                        "studentId"
                                    ).orEmpty(),

                                studentName =
                                    doc.getString(
                                        "studentName"
                                    ).orEmpty(),

                                regNo =
                                    doc.getString(
                                        "regNo"
                                    ).orEmpty(),

                                subjectName =
                                    doc.getString(
                                        "subjectName"
                                    ).orEmpty(),

                                courseCode =
                                    doc.getString(
                                        "courseCode"
                                    ).orEmpty(),

                                date =
                                    doc.getString(
                                        "date"
                                    ).orEmpty(),

                                status =
                                    status,

                                markedAt =
                                    doc.getTimestamp(
                                        "markedAt"
                                    )?.toDate(),

                                sessionId =
                                    doc.getString(
                                        "sessionId"
                                    ).orEmpty()
                            )
                        }
                        .sortedWith(
                            compareByDescending<
                                    TeacherAttendanceHistoryItem
                                    > {
                                it.markedAt
                            }.thenBy {
                                it.studentName
                                    .lowercase()
                            }
                        )

                onChanged(
                    records
                )
            }
    }


    // ============================================================
    // TODAY DATE
    // ============================================================

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Date()
        )
    }


    // ============================================================
    // TODAY DAY
    // ============================================================

    private fun todayDayName(): String {

        return SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(
            Date()
        )
    }


    // ============================================================
    // TIME PARSER
    // ============================================================

    private fun parseTodayTime(
        time: String
    ): Date? {

        val clean =
            time
                .trim()
                .uppercase(
                    Locale.US
                )

        val formats =
            listOf(
                "hh:mm a",
                "h:mm a",
                "HH:mm"
            )

        for (
        pattern in formats
        ) {

            try {

                val parsed =
                    SimpleDateFormat(
                        pattern,
                        Locale.US
                    )
                        .parse(
                            clean
                        )
                        ?: continue

                val now =
                    Calendar.getInstance()

                val classTime =
                    Calendar.getInstance()

                classTime.time =
                    parsed

                now.set(
                    Calendar.HOUR_OF_DAY,
                    classTime.get(
                        Calendar.HOUR_OF_DAY
                    )
                )

                now.set(
                    Calendar.MINUTE,
                    classTime.get(
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

                return now.time

            } catch (
                _: Exception
            ) {
                // Try next format.
            }
        }

        return null
    }


    // ============================================================
    // NUMBER
    // ============================================================

    private fun numberField(
        value: Any?
    ): Int? {

        return when (value) {

            is Number ->
                value.toInt()

            is String ->
                value.toIntOrNull()
                    ?: Regex("\\d+")
                        .find(value)
                        ?.value
                        ?.toIntOrNull()

            else ->
                null
        }
    }


    // ============================================================
    // NORMALIZE DAY
    // ============================================================

    private fun normalizeDayName(
        value: String
    ): String {

        return when (
            value
                .trim()
                .lowercase(
                    Locale.US
                )
        ) {

            "monday" ->
                "monday"

            "tuesday" ->
                "tuesday"

            "wednesday" ->
                "wednesday"

            "thursday" ->
                "thursday"

            "friday" ->
                "friday"

            "saturday" ->
                "saturday"

            "sunday" ->
                "sunday"

            else ->
                ""
        }
    }


    // ============================================================
    // BETTER ERROR
    // ============================================================

    private fun buildScheduleError(
        schedule: ClassSchedule,
        today: String,
        todayDay: String
    ): String {

        val configuredDay =
            schedule.dayName
                .ifBlank {
                    "-"
                }

        val configuredDate =
            schedule.date
                .ifBlank {
                    "-"
                }

        return "This class is not scheduled for today. " +
                "Today: $todayDay ($today). " +
                "Schedule: $configuredDay ($configuredDate)."
    }
}