package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudentAttendanceSummaryRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    private val studentAssignmentsRef =
        firestore.collection(
            "studentSubjectAssignments"
        )

    private val attendanceRef =
        firestore.collection(
            "attendance_records"
        )

    private val sessionsRef =
        firestore.collection(
            "attendanceSessions"
        )

    /**
     * Returns subject-wise attendance for the logged-in student.
     *
     * Total classes are calculated from attendanceSessions,
     * NOT attendance_records.
     *
     * This is important because a class can be conducted even
     * when this particular student does not mark attendance.
     */
    suspend fun getStudentAttendance(
        student: Student
    ): List<SubjectAttendanceSummary> {

        if (
            student.studentId.isBlank()
        ) {
            return emptyList()
        }

        if (
            student.classId.isBlank()
        ) {
            return emptyList()
        }

        // --------------------------------------------------------
        // STUDENT'S ENROLLED SUBJECTS
        // --------------------------------------------------------

        val assignments =
            studentAssignmentsRef
                .whereEqualTo(
                    "studentId",
                    student.studentId
                )
                .get()
                .await()
                .documents

        if (
            assignments.isEmpty()
        ) {
            return emptyList()
        }

        val result =
            mutableListOf<SubjectAttendanceSummary>()


        // --------------------------------------------------------
        // EACH ENROLLED SUBJECT
        // --------------------------------------------------------

        for (
        assignment in assignments
        ) {

            val subjectId =
                assignment
                    .getString(
                        "subjectId"
                    )
                    .orEmpty()

            if (
                subjectId.isBlank()
            ) {
                continue
            }


            val subjectName =
                assignment
                    .getString(
                        "subjectName"
                    )
                    .orEmpty()
                    .ifBlank {
                        "Unknown Subject"
                    }

            val courseCode =
                assignment
                    .getString(
                        "courseCode"
                    )
                    .orEmpty()


            // ----------------------------------------------------
            // CLASSES ACTUALLY HELD
            // ----------------------------------------------------

            val sessionDocuments =
                sessionsRef
                    .whereEqualTo(
                        "classId",
                        student.classId
                    )
                    .get()
                    .await()
                    .documents
                    .filter {

                        it.getString(
                            "subjectId"
                        ) == subjectId
                    }


            /*
             * A teacher could technically reopen/create more than
             * one attendance session on the same day.
             *
             * Therefore we count DISTINCT dates.
             */
            val classDates =
                sessionDocuments
                    .mapNotNull {

                        it.getString(
                            "date"
                        )?.takeIf {
                            it.isNotBlank()
                        }
                    }
                    .distinct()


            val totalClassesHeld =
                classDates.size


            // ----------------------------------------------------
            // STUDENT'S PRESENT RECORDS
            // ----------------------------------------------------

            val studentRecords =
                attendanceRef
                    .whereEqualTo(
                        "studentId",
                        student.studentId
                    )
                    .get()
                    .await()
                    .documents
                    .filter {

                        it.getString(
                            "subjectId"
                        ) == subjectId &&

                                it.getString(
                                    "classId"
                                ) == student.classId &&

                                it.getString(
                                    "status"
                                )
                                    ?.equals(
                                        "present",
                                        ignoreCase = true
                                    ) == true
                    }


            val presentDates =
                studentRecords
                    .mapNotNull {

                        it.getString(
                            "date"
                        )?.takeIf {
                            it.isNotBlank()
                        }
                    }
                    .distinct()
                    .sortedDescending()


            val presentCount =
                presentDates.size


            // ----------------------------------------------------
            // TEACHER NAME
            // ----------------------------------------------------

            val teacherName =
                sessionDocuments
                    .asSequence()
                    .mapNotNull {
                        it.getString(
                            "teacherName"
                        )
                    }
                    .firstOrNull {
                        it.isNotBlank()
                    }
                    ?: "Not available"


            result.add(
                SubjectAttendanceSummary(

                    subjectId =
                        subjectId,

                    subjectName =
                        subjectName,

                    courseCode =
                        courseCode,

                    teacherName =
                        teacherName,

                    totalClassesHeld =
                        totalClassesHeld,

                    presentCount =
                        presentCount,

                    presentDates =
                        presentDates
                )
            )
        }


        return result
            .sortedWith(
                compareBy(
                    { it.courseCode },
                    { it.subjectName }
                )
            )
    }


    /**
     * Returns subjects whose attendance is <= 75%.
     */
    suspend fun getLowAttendanceSubjects(
        student: Student
    ): List<SubjectAttendanceSummary> {

        return getStudentAttendance(
            student
        ).filter {

            it.hasAnyData &&
                    it.percentage <= 75
        }
    }
}