package com.university.attendance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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

    suspend fun getCurrentStudent(): Student {

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
            com.google.firebase.firestore.ListenerRegistration {

        if (
            student.classId.isBlank()
        ) {

            return sessionsRef
                .document(
                    "_no_student_class"
                )
                .addSnapshotListener {
                        _, _ ->

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

                    onError(
                        error
                    )

                    return@addSnapshotListener
                }

                val now =
                    System.currentTimeMillis()

                val today =
                    todayDate()

                val list =
                    snap
                        ?.documents
                        .orEmpty()
                        .mapNotNull(
                            ::sessionFromDoc
                        )
                        .filter { s ->

                            s.isActive &&

                                    s.status
                                        .equals(
                                            "active",
                                            true
                                        ) &&

                                    s.date ==
                                    today &&

                                    s.semester ==
                                    student.semester &&

                                    s.session
                                        .equals(
                                            student.session,
                                            true
                                        ) &&

                                    s.section
                                        .equals(
                                            student.section,
                                            true
                                        ) &&

                                    s.departmentName
                                        .equals(
                                            student.departmentName,
                                            true
                                        ) &&

                                    s.programName
                                        .equals(
                                            student.programName,
                                            true
                                        ) &&

                                    (
                                            s.expiresAt
                                                ?.time
                                                ?: 0L
                                            ) > now
                        }
                        .sortedBy(
                            ::startMillis
                        )

                onChanged(
                    list
                )
            }
    }

    /**
     * Final student-side filter.
     *
     * Student only sees a live attendance if:
     *
     * Student has explicit enrollment
     * for that exact Subject + Teacher.
     */
    suspend fun filterEligibleSessions(
        student: Student,
        sessions:
        List<AttendanceSession>
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

                    val subjectId =
                        doc.getString(
                            "subjectId"
                        )
                            ?: return@mapNotNull null

                    val teacherId =
                        doc.getString(
                            "teacherId"
                        )
                            ?: return@mapNotNull null

                    if (
                        doc.getString(
                            "classId"
                        ) != student.classId
                    ) {
                        return@mapNotNull null
                    }

                    if (
                        numberField(
                            doc.get("semester")
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

                    "${subjectId}__${teacherId}"
                }
                .toSet()

        return sessions.filter {

            "${it.subjectId}__${it.teacherId}" in
                    enrolled
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

        val snap =
            sessionsRef
                .whereEqualTo(
                    "classId",
                    student.classId
                )
                .get()
                .await()

        val now =
            System.currentTimeMillis()

        val today =
            todayDate()

        val sessions =
            snap
                .documents
                .mapNotNull(
                    ::sessionFromDoc
                )
                .filter { s ->

                    s.isActive &&

                            s.status
                                .equals(
                                    "active",
                                    true
                                ) &&

                            s.date ==
                            today &&

                            s.semester ==
                            student.semester &&

                            s.session
                                .equals(
                                    student.session,
                                    true
                                ) &&

                            s.section
                                .equals(
                                    student.section,
                                    true
                                ) &&

                            s.departmentName
                                .equals(
                                    student.departmentName,
                                    true
                                ) &&

                            s.programName
                                .equals(
                                    student.programName,
                                    true
                                ) &&

                            (
                                    s.expiresAt
                                        ?.time
                                        ?: 0L
                                    ) > now
                }
                .sortedBy(
                    ::startMillis
                )

        return filterEligibleSessions(
            student,
            sessions
        )
    }

    suspend fun markAttendance(
        sessionId: String,
        student: Student
    ) {

        if (!student.isActive) {

            throw Exception(
                "Your student account is inactive."
            )
        }

        if (
            student.classId.isBlank()
        ) {

            throw Exception(
                "Your class is not assigned."
            )
        }

        val sessionDoc =
            sessionsRef
                .document(sessionId)
                .get()
                .await()

        val session =
            sessionFromDoc(
                sessionDoc
            )
                ?: throw Exception(
                    "Attendance session not found."
                )

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

        if (
            session.classId !=
            student.classId ||

            !session.departmentName
                .equals(
                    student.departmentName,
                    true
                ) ||

            !session.programName
                .equals(
                    student.programName,
                    true
                ) ||

            session.semester !=
            student.semester ||

            !session.session
                .equals(
                    student.session,
                    true
                ) ||

            !session.section
                .equals(
                    student.section,
                    true
                )
        ) {

            throw Exception(
                "This attendance does not belong to your class."
            )
        }

        /*
         * IMPORTANT:
         * Student MUST have explicit Subject + Teacher enrollment.
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

                            doc.getString(
                                "teacherId"
                            ) ==
                            session.teacherId &&

                            numberField(
                                doc.get("semester")
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
                "You are not assigned to this subject/teacher."
            )
        }

        /*
         * Verify schedule.
         */
        val scheduleDoc =
            schedulesRef
                .document(
                    session.scheduleId
                )
                .get()
                .await()

        val schedule =
            scheduleDoc
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
         * Verify teacher.
         */
        val teacherDoc =
            teachersRef
                .document(
                    session.teacherId
                )
                .get()
                .await()

        if (
            !teacherDoc.exists()
        ) {

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
         * Verify teacher -> class -> subject relation.
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

                            doc.getString(
                                "session"
                            )
                                .orEmpty()
                                .equals(
                                    session.session,
                                    true
                                ) &&

                            numberField(
                                doc.get("semester")
                            ) ==
                            session.semester
                }

        if (!teacherAssignment) {

            throw Exception(
                "Teacher is not assigned to this class and subject."
            )
        }

        /*
         * One attendance per student + subject + date.
         */
        val recordId =
            "${student.studentId}_${session.subjectId}_${todayDate()}"

        val ref =
            attendanceRef
                .document(recordId)

        firestore.runTransaction { tx ->

            if (
                tx.get(ref).exists()
            ) {

                throw IllegalStateException(
                    "Your attendance is already marked for this subject today."
                )
            }

            tx.set(
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
        date: String =
            todayDate()
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
        d: DocumentSnapshot
    ): AttendanceSession? {

        if (
            !d.exists()
        ) {
            return null
        }

        return AttendanceSession(

            sessionId =
                d.id,

            scheduleId =
                d.getString(
                    "scheduleId"
                ).orEmpty(),

            teacherId =
                d.getString(
                    "teacherId"
                ).orEmpty(),

            teacherAuthUid =
                d.getString(
                    "teacherAuthUid"
                ).orEmpty(),

            teacherName =
                d.getString(
                    "teacherName"
                ).orEmpty(),

            subjectId =
                d.getString(
                    "subjectId"
                ).orEmpty(),

            subjectName =
                d.getString(
                    "subjectName"
                ).orEmpty(),

            courseCode =
                d.getString(
                    "courseCode"
                ).orEmpty(),

            classId =
                d.getString(
                    "classId"
                ).orEmpty(),

            departmentName =
                d.getString(
                    "departmentName"
                ).orEmpty(),

            programName =
                d.getString(
                    "programName"
                ).orEmpty(),

            semester =
                numberField(
                    d.get("semester")
                ) ?: 1,

            session =
                d.getString(
                    "session"
                ).orEmpty(),

            section =
                d.getString(
                    "section"
                ).orEmpty(),

            className =
                d.getString(
                    "className"
                ).orEmpty(),

            roomNumber =
                numberField(
                    d.get("roomNumber")
                ) ?: 0,

            date =
                d.getString(
                    "date"
                ).orEmpty(),

            dayName =
                d.getString(
                    "dayName"
                ).orEmpty(),

            startTime =
                d.getString(
                    "startTime"
                ).orEmpty(),

            endTime =
                d.getString(
                    "endTime"
                ).orEmpty(),

            qrPayload =
                d.getString(
                    "qrPayload"
                ).orEmpty(),

            isActive =
                d.getBoolean(
                    "isActive"
                ) ?: false,

            status =
                d.getString(
                    "status"
                ) ?: "active",

            createdAt =
                d.getDate(
                    "createdAt"
                ),

            expiresAt =
                d.getDate(
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
        session:
        AttendanceSession
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

        } catch (
            _: Exception
        ) {

            Long.MAX_VALUE
        }
    }

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )
            .format(
                Date()
            )
    }
}