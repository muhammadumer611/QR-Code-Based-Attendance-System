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

                            schedule.dayName
                                .equals(
                                    todayDay,
                                    ignoreCase = true
                                )

                isDaily || isWeekly
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
                totalPresent * 100L /
                        totalPossible
                )
            .toInt()
            .coerceIn(
                0,
                100
            )
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