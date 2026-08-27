package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeacherDashboardRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val teacherSubjectsRepository: TeacherSubjectsRepository =
        TeacherSubjectsRepository()
) {

    private val studentsRef =
        firestore.collection("students")

    private val attendanceRef =
        firestore.collection("attendance_records")

    private val classScheduleRef =
        firestore.collection("classSchedules")


    // ============================================================
    // DASHBOARD DATA
    // ============================================================

    data class DashboardData(

        val teacher: Teacher,

        val assignedSubjects: List<Subject>,

        val totalStudents: Int,

        val attendancePercentage: Int,

        val todayClasses: List<ClassSchedule>,

        val upcomingClasses: List<ClassSchedule>
    )


    // ============================================================
    // MAIN DASHBOARD
    // ============================================================

    suspend fun loadDashboard(): DashboardData {

        // --------------------------------------------------------
        // CURRENT TEACHER
        // --------------------------------------------------------

        val teacher =
            teacherSubjectsRepository
                .getCurrentTeacher()


        // --------------------------------------------------------
        // ASSIGNED SUBJECTS
        // --------------------------------------------------------

        val subjects =
            teacherSubjectsRepository
                .getAssignedSubjects(
                    teacher.teacherId
                )


        // --------------------------------------------------------
        // STUDENTS
        // --------------------------------------------------------

        val totalStudents =
            getTotalStudents(
                subjects
            )


        // --------------------------------------------------------
        // ATTENDANCE
        // --------------------------------------------------------

        val attendancePercentage =
            getAttendancePercentage(
                subjects
            )


        // --------------------------------------------------------
        // TODAY'S CLASSES
        // --------------------------------------------------------

        val todayClasses =
            getTodayClasses(
                teacher
            )


        // --------------------------------------------------------
        // UPCOMING CLASSES
        // --------------------------------------------------------

        val upcomingClasses =
            getUpcomingClasses(
                teacher
            )


        return DashboardData(

            teacher = teacher,

            assignedSubjects = subjects,

            totalStudents = totalStudents,

            attendancePercentage = attendancePercentage,

            todayClasses = todayClasses,

            upcomingClasses = upcomingClasses
        )
    }


    // ============================================================
    // GET TOTAL STUDENTS
    // ============================================================

    private suspend fun getTotalStudents(
        subjects: List<Subject>
    ): Int {

        if (subjects.isEmpty()) {
            return 0
        }


        val studentIds =
            mutableSetOf<String>()


        subjects.forEach { subject ->

            val snapshot =
                studentsRef

                    .whereEqualTo(
                        "departmentName",
                        subject.departmentName
                    )

                    .whereEqualTo(
                        "programName",
                        subject.programName
                    )

                    .get()
                    .await()


            snapshot.documents.forEach { document ->

                val isActive =
                    document.getBoolean(
                        "isActive"
                    ) != false


                if (isActive) {

                    studentIds.add(
                        document.id
                    )
                }
            }
        }


        return studentIds.size
    }


    // ============================================================
    // ATTENDANCE PERCENTAGE
    // ============================================================

    private suspend fun getAttendancePercentage(
        subjects: List<Subject>
    ): Int {

        if (subjects.isEmpty()) {
            return 0
        }


        var totalPresent = 0

        var totalPossible = 0


        subjects.forEach { subject ->

            // ----------------------------------------------------
            // STUDENTS OF SUBJECT PROGRAM
            // ----------------------------------------------------

            val studentSnapshot =
                studentsRef

                    .whereEqualTo(
                        "departmentName",
                        subject.departmentName
                    )

                    .whereEqualTo(
                        "programName",
                        subject.programName
                    )

                    .get()
                    .await()


            val enrolledStudents =
                studentSnapshot.documents.count {

                    it.getBoolean(
                        "isActive"
                    ) != false
                }


            if (enrolledStudents == 0) {
                return@forEach
            }


            // ----------------------------------------------------
            // PRESENT RECORDS
            // ----------------------------------------------------

            val attendanceSnapshot =
                attendanceRef

                    .whereEqualTo(
                        "teacherId",
                        subject.teacherId
                    )

                    .whereEqualTo(
                        "subjectId",
                        subject.subjectId
                    )

                    .whereEqualTo(
                        "status",
                        "present"
                    )

                    .get()
                    .await()


            if (attendanceSnapshot.isEmpty) {
                return@forEach
            }


            // ----------------------------------------------------
            // DISTINCT CLASS DATES
            // ----------------------------------------------------

            val dates =
                attendanceSnapshot.documents

                    .mapNotNull {
                        it.getString("date")
                    }

                    .distinct()


            val classesHeld =
                dates.size


            if (classesHeld > 0) {

                val presentCount =
                    attendanceSnapshot.size()


                totalPresent +=
                    presentCount


                totalPossible +=
                    classesHeld *
                            enrolledStudents
            }
        }


        if (totalPossible == 0) {
            return 0
        }


        return (
                totalPresent * 100L
                        / totalPossible
                )
            .toInt()
            .coerceIn(
                0,
                100
            )
    }


    // ============================================================
    // GET TODAY'S CLASSES
    // ============================================================

    private suspend fun getTodayClasses(
        teacher: Teacher
    ): List<ClassSchedule> {

        val teacherAuthUid =
            teacher.authUid


        if (teacherAuthUid.isBlank()) {
            return emptyList()
        }


        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Calendar.getInstance().time
            )


        val snapshot =
            classScheduleRef

                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )

                .whereEqualTo(
                    "date",
                    today
                )

                .get()
                .await()


        return snapshot.documents

            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }

            .sortedWith(
                compareBy<ClassSchedule> {

                    parseTimeForSorting(
                        it.startTime
                    )

                }.thenBy {

                    it.subjectName
                }
            )
    }


    // ============================================================
    // GET UPCOMING CLASSES
    // ============================================================

    private suspend fun getUpcomingClasses(
        teacher: Teacher
    ): List<ClassSchedule> {

        val teacherAuthUid =
            teacher.authUid


        if (teacherAuthUid.isBlank()) {
            return emptyList()
        }


        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Calendar.getInstance().time
            )


        val snapshot =
            classScheduleRef

                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )

                .whereGreaterThanOrEqualTo(
                    "date",
                    today
                )

                .get()
                .await()


        return snapshot.documents

            .mapNotNull { document ->

                document
                    .toObject(
                        ClassSchedule::class.java
                    )
                    ?.apply {

                        scheduleId =
                            document.id
                    }
            }

            .sortedWith(

                compareBy<ClassSchedule> {

                    it.date

                }.thenBy {

                    parseTimeForSorting(
                        it.startTime
                    )
                }
            )

            // Today's classes are also returned by the query.
            // Keep only the first upcoming classes for the dashboard.
            .take(10)
    }


    // ============================================================
    // TIME SORTING
    // ============================================================

    private fun parseTimeForSorting(
        time: String
    ): Long {

        return try {

            SimpleDateFormat(
                "hh:mm a",
                Locale.US
            )

                .parse(
                    time
                )
                ?.time

                ?: Long.MAX_VALUE

        } catch (
            _: Exception
        ) {

            Long.MAX_VALUE
        }
    }
}