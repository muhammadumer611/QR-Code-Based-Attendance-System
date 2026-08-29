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
        // TODAY (must be Daily schedule)
        // --------------------------------------------------------

        val today =
            todayDate()


        val todayClasses =
            allClasses
                .filter {

                    it.periodType
                        .trim()
                        .equals(
                            "Daily",
                            ignoreCase = true
                        ) &&

                            it.date == today
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


        // --------------------------------------------------------
        // THIS WEEK (must be Weekly schedule)
        // --------------------------------------------------------

        val weekClasses =
            getCurrentWeekClasses(
                allClasses
            )


        // --------------------------------------------------------
        // SEMESTER (must be Semester schedule)
        // --------------------------------------------------------

        val semesterClasses =
            getSemesterClasses(
                allClasses,
                subjects
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

                }.thenBy {

                    it.subjectName
                }
            )
    }


    // ============================================================
    // TODAY DATE
    // ============================================================

    private fun todayDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(
            Calendar.getInstance().time
        )
    }


    // ============================================================
    // THIS WEEK (strict: periodType must be Weekly)
    // ============================================================

    private fun getCurrentWeekClasses(
        classes: List<ClassSchedule>
    ): List<ClassSchedule> {

        val calendar =
            Calendar.getInstance()


        // Monday
        calendar.set(
            Calendar.DAY_OF_WEEK,
            Calendar.MONDAY
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val weekStart =
            calendar.time


        // Sunday
        calendar.add(
            Calendar.DAY_OF_YEAR,
            6
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            23
        )

        calendar.set(
            Calendar.MINUTE,
            59
        )

        calendar.set(
            Calendar.SECOND,
            59
        )

        val weekEnd =
            calendar.time


        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )


        return classes
            .filter { classSchedule ->

                if (
                    !classSchedule.periodType
                        .trim()
                        .equals(
                            "Weekly",
                            ignoreCase = true
                        )
                ) {
                    return@filter false
                }

                try {

                    val classDate =
                        dateFormat.parse(
                            classSchedule.date
                        )

                    classDate != null &&
                            !classDate.before(
                                weekStart
                            ) &&
                            !classDate.after(
                                weekEnd
                            )

                } catch (_: Exception) {

                    false
                }
            }
            .sortedWith(

                compareBy<ClassSchedule> {

                    it.date

                }.thenBy {

                    parseTimeForSorting(
                        it.startTime
                    )

                }.thenBy {

                    it.subjectName
                }
            )
    }


    // ============================================================
    // SEMESTER CLASSES (strict: periodType must be Semester)
    // ============================================================

    private fun getSemesterClasses(
        classes: List<ClassSchedule>,
        subjects: List<Subject>
    ): List<ClassSchedule> {

        if (classes.isEmpty()) {
            return emptyList()
        }


        /*
         * Admin schedule mein semester directly save hota hai.
         *
         * Teacher ke assigned subjects ke semesters bhi
         * subjects collection mein available hain.
         *
         * Isliye pehle assigned subjects ke semester collect
         * karte hain.
         */

        val assignedSemesters =
            subjects
                .map {
                    it.semester
                        .toString()
                        .trim()
                        .lowercase()
                }
                .filter {
                    it.isNotBlank()
                }
                .toSet()


        return classes
            .filter { classSchedule ->

                // MUST be Semester schedule
                if (
                    !classSchedule.periodType
                        .trim()
                        .equals(
                            "Semester",
                            ignoreCase = true
                        )
                ) {
                    return@filter false
                }

                // If teacher subjects have semester info,
                // match schedule semester with them.
                if (assignedSemesters.isNotEmpty()) {

                    assignedSemesters.contains(
                        classSchedule.semester
                            .trim()
                            .lowercase()
                    )

                } else {

                    classSchedule.semester
                        .isNotBlank()
                }
            }
            .sortedWith(

                compareBy<ClassSchedule> {

                    it.semester

                }.thenBy {

                    it.date

                }.thenBy {

                    parseTimeForSorting(
                        it.startTime
                    )
                }
            )
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