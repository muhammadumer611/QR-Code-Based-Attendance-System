package com.university.attendance


import DailySessionGroup
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Daily Overview screen: for a selected date, shows EVERY
 * class (across all departments/sessions) and whether attendance was
 * marked for each of its subjects that day.
 *
 * "Classes" here means every (class, subject) pair that exists in the
 * system (from "classes" + "subjects"), cross-referenced against
 * "attendance_records" for that specific date -- so both marked AND
 * not-yet-marked classes show up.
 */
class DailyOverviewRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val classesRef = firestore.collection("classes")
    private val subjectsRef = firestore.collection("subjects")
    private val attendanceRef = firestore.collection("attendance_records")

    /**
     * Builds the full daily picture:
     *   1. Fetch all classes (each has department, program, session, section).
     *   2. Fetch all subjects (each has department + program).
     *   3. Fetch all attendance_records for the given date (one query,
     *      filtered client-side per class+subject -- avoids N queries).
     *   4. For every class, match its subjects (by department+program),
     *      and check whether that date has attendance_records for that
     *      class+subject combination.
     *   5. Group results by Department -> Session for display.
     */
    suspend fun getDailyOverview(date: String): List<DailyDepartmentGroup> {
        val classesSnapshot = classesRef.get().await()
        val classes = classesSnapshot.documents.mapNotNull { doc ->
            doc.toObject(StudentClass::class.java)?.apply { classId = doc.id }
        }

        val subjectsSnapshot = subjectsRef.get().await()
        val subjects = subjectsSnapshot.documents.mapNotNull { doc ->
            doc.toObject(Subject::class.java)?.apply { subjectId = doc.id }
        }

        val attendanceSnapshot = attendanceRef
            .whereEqualTo("date", date)
            .get()
            .await()
        val recordsForDate = attendanceSnapshot.documents.mapNotNull { doc ->
            doc.toObject(AttendanceRecord::class.java)
        }

        val statuses = mutableListOf<DailyClassStatus>()

        for (studentClass in classes) {
            val matchingSubjects = subjects.filter {
                it.departmentName == studentClass.departmentName &&
                        it.programName == studentClass.programName
            }

            for (subject in matchingSubjects) {
                val recordsForThisClassSubject = recordsForDate.filter {
                    it.classId == studentClass.classId && it.subjectId == subject.subjectId
                }

                statuses.add(
                    DailyClassStatus(
                        departmentName = studentClass.departmentName,
                        classId = studentClass.classId,
                        classTitle = "${studentClass.programName} - Section ${studentClass.section}",
                        session = studentClass.session,
                        subjectId = subject.subjectId,
                        subjectName = subject.subjectName,
                        courseCode = subject.courseCode,
                        wasMarked = recordsForThisClassSubject.isNotEmpty(),
                        presentCount = recordsForThisClassSubject.size,
                        totalStudents = studentClass.studentCount.toInt()
                    )
                )
            }
        }

        return statuses
            .groupBy { it.departmentName }
            .map { (departmentName, deptStatuses) ->
                val sessionGroups = deptStatuses
                    .groupBy { it.session }
                    .map { (session, sessionStatuses) ->
                        DailySessionGroup(
                            session = session,
                            classStatuses = sessionStatuses.sortedWith(
                                compareBy({ it.classTitle }, { it.courseCode })
                            )
                        )
                    }
                    .sortedBy { it.session }

                DailyDepartmentGroup(departmentName = departmentName, sessionGroups = sessionGroups)
            }
            .sortedBy { it.departmentName }
    }
}