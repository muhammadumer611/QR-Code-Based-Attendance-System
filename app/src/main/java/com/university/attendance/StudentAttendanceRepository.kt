package com.university.attendance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudentAttendanceRepository(
    private val firestore:
    FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentsRef =
        firestore.collection("students")

    private val sessionsRef =
        firestore.collection("attendanceSessions")

    private val schedulesRef =
        firestore.collection("classSchedules")

    private val teachersRef =
        firestore.collection("teachers")

    private val teacherAssignmentsRef =
        firestore.collection(
            "teacherSubjectAssignments"
        )

    private val studentAssignmentsRef =
        firestore.collection(
            "studentSubjectAssignments"
        )

    private val attendanceRef =
        firestore.collection(
            "attendance_records"
        )

    suspend fun getCurrentStudent():
            Student {

        val uid =
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid
                ?: throw Exception(
                    "Student is not logged in."
                )

        val doc =
            studentsRef
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
                    "Student profile not found."
                )

        return doc
            .toObject(
                Student::class.java
            )
            ?.apply {

                studentId =
                    doc.id

                authUid =
                    uid
            }
            ?: throw Exception(
                "Unable to read student profile."
            )
    }

    fun listenForActiveSessions(
        student: Student,
        onChanged:
            (List<AttendanceSession>) -> Unit,
        onError:
            (Exception) -> Unit
    ):
            com.google.firebase.firestore
            .ListenerRegistration {

        if (student.classId.isBlank()) {

            return sessionsRef
                .document(
                    "_no_student_class"
                )
                .addSnapshotListener {
                        _,
                        _ ->

                    onChanged(
                        emptyList()
                    )
                }
        }

        return sessionsRef
            .whereEqualTo(
                "classId",
                student.classId
            )
            .addSnapshotListener {
                    snap,
                    error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                onChanged(
                    filterSessionIdentity(
                        student,
                        snap?.documents
                            .orEmpty()
                            .mapNotNull(
                                ::sessionFromDoc
                            )
                    )
                )
            }
    }

    private fun filterSessionIdentity(
        student: Student,
        source: List<AttendanceSession>
    ): List<AttendanceSession> {

        val now =
            System.currentTimeMillis()

        val today =
            todayDate()

        return source
            .filter { session ->

                session.isActive &&

                        session.status
                            .equals(
                                "active",
                                true
                            ) &&

                        session.date ==
                        today &&

                        session.semester ==
                        student.semester &&

                        session.session
                            .equals(
                                student.session,
                                true
                            ) &&

                        session.section
                            .equals(
                                student.section,
                                true
                            ) &&

                        session.departmentName
                            .equals(
                                student.departmentName,
                                true
                            ) &&

                        session.programName
                            .equals(
                                student.programName,
                                true
                            ) &&

                        (
                                session.expiresAt
                                    ?.time
                                    ?: 0L
                                ) > now
            }
            .sortedBy(
                ::startMillis
            )
    }

    /*
     * ------------------------------------------------------------
     * STUDENT SUBJECT ELIGIBILITY
     * ------------------------------------------------------------
     *
     * Student enrollment only requires subjectId.
     *
     * Teacher relation is checked separately.
     */

    suspend fun filterEligibleSessions(
        student: Student,
        sessions: List<AttendanceSession>
    ): List<AttendanceSession> {

        if (
            student.studentId.isBlank() ||
            sessions.isEmpty()
        ) {
            return emptyList()
        }

        val enrolled =
            studentAssignmentsRef
                .whereEqualTo(
                    "studentId",
                    student.studentId
                )
                .get()
                .await()
                .documents
                .mapNotNull { doc ->

                    if (
                        doc.getString(
                            "classId"
                        ) !=
                        student.classId
                    ) {
                        return@mapNotNull null
                    }

                    if (
                        numberField(
                            doc.get(
                                "semester"
                            )
                        ) !=
                        student.semester
                    ) {
                        return@mapNotNull null
                    }

                    if (
                        !doc.getString(
                            "session"
                        )
                            .orEmpty()
                            .equals(
                                student.session,
                                true
                            )
                    ) {
                        return@mapNotNull null
                    }

                    if (
                        !doc.getString(
                            "section"
                        )
                            .orEmpty()
                            .equals(
                                student.section,
                                true
                            )
                    ) {
                        return@mapNotNull null
                    }

                    doc.getString(
                        "subjectId"
                    )
                        ?.takeIf(
                            String::isNotBlank
                        )
                }
                .toSet()

        return sessions.filter {
            it.subjectId in enrolled
        }
    }

    suspend fun getActiveSessionsForStudent(
        student: Student
    ): List<AttendanceSession> {

        if (
            student.classId.isBlank()
        ) {
            return emptyList()
        }

        val snapshot =
            sessionsRef
                .whereEqualTo(
                    "classId",
                    student.classId
                )
                .get()
                .await()

        val sessions =
            filterSessionIdentity(
                student,
                snapshot.documents
                    .mapNotNull(
                        ::sessionFromDoc
                    )
            )

        return filterEligibleSessions(
            student,
            sessions
        )
    }

    /*
     * ------------------------------------------------------------
     * TODAY'S CLASS SCHEDULE FOR STUDENT
     * ------------------------------------------------------------
     */

    suspend fun getTodayScheduleForStudent(
        student: Student
    ): List<ClassSchedule> {

        if (
            student.classId.isBlank()
        ) {
            return emptyList()
        }

        val today =
            todayDate()

        val day =
            todayDayName()

        return schedulesRef
            .whereEqualTo(
                "classId",
                student.classId
            )
            .get()
            .await()
            .documents
            .map {
                ClassSchedule.fromDocument(
                    it
                )
            }
            .filter { schedule ->

                schedule.semester ==
                        student.semester &&

                        schedule.session
                            .equals(
                                student.session,
                                true
                            ) &&

                        schedule.section
                            .equals(
                                student.section,
                                true
                            ) &&

                        schedule.departmentName
                            .equals(
                                student.departmentName,
                                true
                            ) &&

                        schedule.programName
                            .equals(
                                student.programName,
                                true
                            ) &&

                        when {

                            schedule.periodType
                                .equals(
                                    "Daily",
                                    true
                                ) ->
                                schedule.date ==
                                        today

                            schedule.periodType
                                .equals(
                                    "Weekly",
                                    true
                                ) ->
                                schedule.dayName
                                    .equals(
                                        day,
                                        true
                                    ) ||
                                        schedule.date ==
                                        today

                            schedule.periodType
                                .equals(
                                    "Semester",
                                    true
                                ) ->
                                schedule.dayName
                                    .equals(
                                        day,
                                        true
                                    ) ||
                                        schedule.date ==
                                        today

                            else ->
                                schedule.date ==
                                        today
                        }
            }
            .sortedBy(
                ::scheduleStartMillis
            )
    }

    /*
     * ------------------------------------------------------------
     * FINAL SAVE ATTENDANCE VALIDATION
     * ------------------------------------------------------------
     */

    suspend fun markAttendance(
        sessionId: String,
        student: Student,
        detectedBleToken: String
    ) {

        if (!student.isActive) {
            throw Exception(
                "Your student account is inactive."
            )
        }

        if (student.classId.isBlank()) {
            throw Exception(
                "Your class is not assigned."
            )
        }

        val session =
            sessionFromDoc(
                sessionsRef
                    .document(sessionId)
                    .get()
                    .await()
            )
                ?: throw Exception(
                    "Attendance session not found."
                )

        if (session.bleToken.isBlank()) {
            throw Exception(
                "This attendance session is not ready for classroom proximity verification."
            )
        }

        if (!detectedBleToken.equals(session.bleToken, ignoreCase = true)) {
            throw Exception(
                "Teacher Bluetooth signal was not detected nearby."
            )
        }

        if (
            !session.isActive ||
            !session.status.equals(
                "active",
                true
            )
        ) {
            throw Exception(
                "Attendance is no longer open."
            )
        }

        if (
            session.date !=
            todayDate()
        ) {
            throw Exception(
                "This attendance session is not for today."
            )
        }

        if (
            (
                    session.expiresAt
                        ?.time
                        ?: 0L
                    ) <=
            System.currentTimeMillis()
        ) {
            throw Exception(
                "Attendance session has expired."
            )
        }

        /*
         * Exact student class identity.
         */

        if (
            session.classId !=
            student.classId ||

            !session.departmentName.equals(
                student.departmentName,
                true
            ) ||

            !session.programName.equals(
                student.programName,
                true
            ) ||

            session.semester !=
            student.semester ||

            !session.session.equals(
                student.session,
                true
            ) ||

            !session.section.equals(
                student.section,
                true
            )
        ) {

            throw Exception(
                "This attendance does not belong to your class."
            )
        }

        /*
         * Student must be enrolled in this subject.
         */
        val studentEnrollment =
            studentAssignmentsRef
                .whereEqualTo(
                    "studentId",
                    student.studentId
                )
                .get()
                .await()
                .documents
                .any { doc ->

                    doc.getString(
                        "classId"
                    ) ==
                            session.classId &&

                            doc.getString(
                                "subjectId"
                            ) ==
                            session.subjectId &&

                            numberField(
                                doc.get(
                                    "semester"
                                )
                            ) ==
                            session.semester &&

                            doc.getString(
                                "session"
                            )
                                .orEmpty()
                                .equals(
                                    session.session,
                                    true
                                ) &&

                            doc.getString(
                                "section"
                            )
                                .orEmpty()
                                .equals(
                                    session.section,
                                    true
                                )
                }

        if (!studentEnrollment) {

            throw Exception(
                "You are not enrolled in this subject."
            )
        }

        /*
         * Schedule must match the session.
         */

        val schedule =
            schedulesRef
                .document(
                    session.scheduleId
                )
                .get()
                .await()
                .takeIf {
                    it.exists()
                }
                ?.let(
                    ClassSchedule::fromDocument
                )
                ?: throw Exception(
                    "Class schedule not found."
                )

        if (
            schedule.teacherId !=
            session.teacherId ||

            schedule.subjectId !=
            session.subjectId ||

            schedule.classId !=
            session.classId
        ) {

            throw Exception(
                "Attendance session is not linked to the scheduled class."
            )
        }

        /*
         * Teacher must still exist.
         */

        val teacherDoc =
            teachersRef
                .document(
                    session.teacherId
                )
                .get()
                .await()

        if (!teacherDoc.exists()) {

            throw Exception(
                "Teacher profile not found."
            )
        }

        val teacherUid =
            teacherDoc
                .getString(
                    "authUid"
                )
                .orEmpty()

        if (
            session.teacherAuthUid
                .isNotBlank() &&

            teacherUid !=
            session.teacherAuthUid
        ) {

            throw Exception(
                "Teacher verification failed."
            )
        }

        /*
         * Teacher must be assigned to exact class + subject.
         */

        val teacherAssignment =
            teacherAssignmentsRef
                .whereEqualTo(
                    "teacherId",
                    session.teacherId
                )
                .get()
                .await()
                .documents
                .any { doc ->

                    doc.getString(
                        "classId"
                    ) ==
                            session.classId &&

                            doc.getString(
                                "subjectId"
                            ) ==
                            session.subjectId &&

                            numberField(
                                doc.get(
                                    "semester"
                                )
                            ) ==
                            session.semester &&

                            doc.getString(
                                "session"
                            )
                                .orEmpty()
                                .equals(
                                    session.session,
                                    true
                                )
                }

        if (!teacherAssignment) {

            throw Exception(
                "Teacher is not assigned to this class and subject."
            )
        }

        /*
         * One attendance per student + subject + day.
         */

        val recordId =
            "${student.studentId}_" +
                    "${session.subjectId}_" +
                    todayDate()

        val ref =
            attendanceRef
                .document(recordId)

        firestore.runTransaction { transaction ->

            if (
                transaction
                    .get(ref)
                    .exists()
            ) {

                throw IllegalStateException(
                    "Your attendance is already marked for this subject today."
                )
            }

            transaction.set(
                ref,
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
                            session.classId,

                    "departmentName" to
                            session.departmentName,

                    "programName" to
                            session.programName,

                    "semester" to
                            session.semester,

                    "session" to
                            session.session,

                    "section" to
                            session.section,

                    "date" to
                            todayDate(),

                    "status" to
                            "present",

                    "markedAt" to
                            FieldValue.serverTimestamp(),

                    "sessionId" to
                            sessionId
                )
            )
        }.await()
    }

    suspend fun isAttendanceAlreadyMarked(
        studentId: String,
        subjectId: String,
        date: String = todayDate()
    ): Boolean {

        return attendanceRef
            .document(
                "${studentId}_${subjectId}_$date"
            )
            .get()
            .await()
            .exists()
    }

    private fun sessionFromDoc(
        doc: DocumentSnapshot
    ): AttendanceSession? {

        if (!doc.exists()) {
            return null
        }

        return AttendanceSession(

            sessionId =
                doc.id,

            scheduleId =
                doc
                    .getString(
                        "scheduleId"
                    )
                    .orEmpty(),

            teacherId =
                doc
                    .getString(
                        "teacherId"
                    )
                    .orEmpty(),

            teacherAuthUid =
                doc
                    .getString(
                        "teacherAuthUid"
                    )
                    .orEmpty(),

            teacherName =
                doc
                    .getString(
                        "teacherName"
                    )
                    .orEmpty(),

            subjectId =
                doc
                    .getString(
                        "subjectId"
                    )
                    .orEmpty(),

            subjectName =
                doc
                    .getString(
                        "subjectName"
                    )
                    .orEmpty(),

            courseCode =
                doc
                    .getString(
                        "courseCode"
                    )
                    .orEmpty(),

            classId =
                doc
                    .getString(
                        "classId"
                    )
                    .orEmpty(),

            departmentName =
                doc
                    .getString(
                        "departmentName"
                    )
                    .orEmpty(),

            programName =
                doc
                    .getString(
                        "programName"
                    )
                    .orEmpty(),

            semester =
                numberField(
                    doc.get(
                        "semester"
                    )
                ) ?: 1,

            session =
                doc
                    .getString(
                        "session"
                    )
                    .orEmpty(),

            section =
                doc
                    .getString(
                        "section"
                    )
                    .orEmpty(),

            className =
                doc
                    .getString(
                        "className"
                    )
                    .orEmpty(),

            roomNumber =
                numberField(
                    doc.get(
                        "roomNumber"
                    )
                ) ?: 0,

            date =
                doc
                    .getString(
                        "date"
                    )
                    .orEmpty(),

            dayName =
                doc
                    .getString(
                        "dayName"
                    )
                    .orEmpty(),

            startTime =
                doc
                    .getString(
                        "startTime"
                    )
                    .orEmpty(),

            endTime =
                doc
                    .getString(
                        "endTime"
                    )
                    .orEmpty(),

            qrPayload =
                doc
                    .getString(
                        "qrPayload"
                    )
                    .orEmpty(),

            bleToken =
                doc
                    .getString(
                        "bleToken"
                    )
                    .orEmpty(),

            isActive =
                doc
                    .getBoolean(
                        "isActive"
                    )
                    ?: false,

            status =
                doc
                    .getString(
                        "status"
                    )
                    ?: "active",

            createdAt =
                doc.getDate(
                    "createdAt"
                ),

            expiresAt =
                doc.getDate(
                    "expiresAt"
                )
        )
    }

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

    private fun startMillis(
        session: AttendanceSession
    ): Long {

        return try {

            SimpleDateFormat(
                "hh:mm a",
                Locale.US
            )
                .parse(
                    session.startTime
                )
                ?.time
                ?: Long.MAX_VALUE

        } catch (_: Exception) {

            Long.MAX_VALUE
        }
    }

    private fun scheduleStartMillis(
        schedule: ClassSchedule
    ): Long {

        return try {

            SimpleDateFormat(
                "hh:mm a",
                Locale.US
            )
                .parse(
                    schedule.startTime
                )
                ?.time
                ?: Long.MAX_VALUE

        } catch (_: Exception) {

            Long.MAX_VALUE
        }
    }

    private fun todayDate(): String =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())

    private fun todayDayName(): String =
        SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(Date())
}