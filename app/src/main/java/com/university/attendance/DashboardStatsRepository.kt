package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the 4 stats cards on the Admin Dashboard (Total Students,
 * Teachers, Departments, Subjects) -- replaces the previously hardcoded
 * numbers in the XML with real Firestore counts.
 */
class DashboardStatsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class Counts(
        val totalStudents: Int,
        val totalTeachers: Int,
        val totalDepartments: Int,
        val totalSubjects: Int
    )

    suspend fun getCounts(): Counts {
        val students = firestore.collection("students").get().await().size()
        val teachers = firestore.collection("teachers").get().await().size()
        val departments = firestore.collection("departments").get().await().size()
        val subjects = firestore.collection("subjects").get().await().size()

        return Counts(
            totalStudents = students,
            totalTeachers = teachers,
            totalDepartments = departments,
            totalSubjects = subjects
        )
    }
}