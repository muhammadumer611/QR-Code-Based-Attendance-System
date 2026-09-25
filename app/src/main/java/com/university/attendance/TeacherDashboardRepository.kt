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

    private val assignmentsRef =
        firestore.collection("teacherSubjectAssignments")


    // ============================================================
    // DASHBOARD DATA
    // ============================================================

    data class DashboardData(

        val teacher: Teacher,

        val assignedSubjects: List<Subject>,

        val totalStudents: Int,

        val attendancePercentage: Int,

        val todayClasses: List<ClassSchedule>,

        val weekClasses: List<ClassSchedule>,

        val semesterClasses: List<ClassSchedule>,

        val schedule: Schedule?
    )


    // ============================================================
    // MAIN DASHBOARD
    // ============================================================

    suspend fun loadDashboard(): DashboardData {

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
        // TOTAL STUDENTS
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
        // ALL TEACHER CLASSES
        // --------------------------------------------------------

        val allClasses =
            getTeacherClasses(
                teacher.teacherId
            )


        // --------------------------------------------------------
        // TEACHER SCHEDULE (PDF)
        // --------------------------------------------------------

        val teacherSchedule =
            ScheduleRepository(
                firestore = firestore
            ).getScheduleForTeacher(

                teacherId =
                    teacher.teacherId,

                teacherAuthUid =
                    teacher.authUid
            )


        // --------------------------------------------------------
        // TODAY / WEEK / SEMESTER (clean filtering)
        // --------------------------------------------------------

        val todayClasses =
            getTodayClasses(
                allClasses
            )

        val weekClasses =
            getWeeklyClasses(
                allClasses
            )

        val semesterClasses =
            getSemesterClasses(
                allClasses
            )


        return DashboardData(

            teacher = teacher,

            assignedSubjects = subjects,

            totalStudents = totalStudents,

            attendancePercentage = attendancePercentage,

            todayClasses = todayClasses,

            weekClasses = weekClasses,

            semesterClasses = semesterClasses,

            schedule = teacherSchedule
        )
    }


    // ============================================================
    // GET ALL TEACHER CLASSES
    // ============================================================

    private suspend fun getTeacherClasses(
        teacherId: String
    ): List<ClassSchedule> {

        if (teacherId.isBlank()) {
            return emptyList()
        }

        val snapshot =
            classScheduleRef
                .whereEqualTo(
                    "teacherId",
                    teacherId
                )
                .get()
                .await()

        return snapshot.documents
            .map { document ->

                ClassSchedule.fromDocument(
                    document
                )
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
    }


    // ============================================================
    // TODAY
    // ============================================================

    private fun getTodayClasses(
        allClasses: List<ClassSchedule>
    ): List<ClassSchedule> {

        val today =
            todayDate()

        val todayDay =
            todayDayName()

        return allClasses
            .filter { schedule ->

                val isDaily =
                    schedule.periodType
                        .equals(
                            "Daily",
                            ignoreCase = true
                        ) &&

                            schedule.date == today

                val isWeekly =
                    schedule.periodType
                        .equals(
                            "Weekly",
                            ignoreCase = true
                        ) &&

                            (schedule.dayName.equals(todayDay, ignoreCase = true) || schedule.date == today)

                val isMonthly =
                    schedule.periodType.equals("Monthly", ignoreCase = true) &&
                        schedule.date == today

                val isSemester =
                    schedule.periodType.equals("Semester", ignoreCase = true) &&
                        (schedule.dayName.equals(todayDay, ignoreCase = true) || schedule.date == today)

                isDaily || isWeekly || isMonthly || isSemester
            }
            .sortedWith(
                compareBy(
                    { parseTimeForSorting(it.startTime) },
                    { it.subjectName }
                )
            )
    }


    // ============================================================
    // CURRENT WEEK
    // ============================================================

    private fun getWeeklyClasses(
        allClasses: List<ClassSchedule>
    ): List<ClassSchedule> {

        return allClasses
            .filter {

                it.periodType
                    .equals(
                        "Weekly",
                        ignoreCase = true
                    )
            }
            .sortedWith(

                compareBy<ClassSchedule> {

                    dayOrder(
                        it.dayName
                    )

                }.thenBy {

                    parseTimeForSorting(
                        it.startTime
                    )
                }
            )
    }


    // ============================================================
    // SEMESTER
    // ============================================================

    private fun getSemesterClasses(
        allClasses: List<ClassSchedule>
    ): List<ClassSchedule> {

        return allClasses
            .filter {

                it.periodType
                    .equals(
                        "Semester",
                        ignoreCase = true
                    )
            }
            .sortedWith(

                compareBy(
                    { it.semester },
                    { it.date },
                    { parseTimeForSorting(it.startTime) }
                )
            )
    }


    // ============================================================
    // DATE / DAY HELPERS
    // ============================================================

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )
    }


    private fun todayDayName(): String {

        return SimpleDateFormat(
            "EEEE",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )
    }


    private fun dayOrder(
        day: String
    ): Int {

        return when (
            day.lowercase()
        ) {

            "monday" -> 1
            "tuesday" -> 2
            "wednesday" -> 3
            "thursday" -> 4
            "friday" -> 5
            "saturday" -> 6
            "sunday" -> 7

            else -> 99
        }
    }


    // ============================================================
    // TOTAL STUDENTS
    // ============================================================

    private suspend fun getTotalStudents(
        subjects: List<Subject>
    ): Int {
        if (subjects.isEmpty()) return 0
        val teacher = teacherSubjectsRepository.getCurrentTeacher()
        val assignments = assignmentsRef.whereEqualTo("teacherId", teacher.teacherId).get().await().documents
        val classIds = assignments.mapNotNull { it.getString("classId") }.toSet()
        if (classIds.isEmpty()) return 0
        val ids = mutableSetOf<String>()
        classIds.forEach { classId ->
            studentsRef.whereEqualTo("classId", classId).whereEqualTo("isActive", true).get().await().documents.forEach { ids.add(it.id) }
        }
        return ids.size
    }

    // ============================================================
    // ATTENDANCE PERCENTAGE
    // ============================================================

    private suspend fun getAttendancePercentage(
        subjects: List<Subject>
    ): Int {
        if (subjects.isEmpty()) return 0
        val teacher = teacherSubjectsRepository.getCurrentTeacher()
        val assignmentDocs = assignmentsRef.whereEqualTo("teacherId", teacher.teacherId).get().await().documents
        val attendanceDocs = attendanceRef.whereEqualTo("teacherId", teacher.teacherId).whereEqualTo("status", "present").get().await().documents
        var present = 0L
        var possible = 0L
        for (assignment in assignmentDocs) {
            val classId = assignment.getString("classId").orEmpty()
            val subjectId = assignment.getString("subjectId").orEmpty()
            if (classId.isBlank() || subjectId.isBlank()) continue
            val enrolled = studentsRef.whereEqualTo("classId", classId).whereEqualTo("isActive", true).get().await().size()
            if (enrolled == 0) continue
            val records = attendanceDocs.filter { it.getString("classId") == classId && it.getString("subjectId") == subjectId }
            val dates = records.mapNotNull { it.getString("date") }.distinct().size
            present += records.size
            possible += dates.toLong() * enrolled.toLong()
        }
        return if (possible == 0L) 0 else (present * 100L / possible).toInt().coerceIn(0, 100)
    }

    // ============================================================
    // TIME SORT
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