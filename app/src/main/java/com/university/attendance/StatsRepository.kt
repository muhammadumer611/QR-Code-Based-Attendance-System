package com.university.attendance

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Dashboard's 4 stat cards (Total Students / Teachers /
 * Departments / Subjects) with LIVE counts from Firestore, instead of
 * hardcoded numbers.
 *
 * Uses Firestore's server-side count() aggregation -- this only reads back
 * a single number per collection, not every document, so it stays cheap
 * even as the collections grow.
 *
 * IMPORTANT: adjust the collection names below (studentsCollection,
 * teachersCollection, departmentsCollection, subjectsCollection) if your
 * Firestore collections are named differently -- "students" and "teachers"
 * match what SearchRepository already uses; "departments" and "subjects"
 * are assumed to match ActivityDepartmentManagement / ActivitySubjectManagement.
 */
class StatsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    data class DashboardStats(
        val studentCount: Int = 0,
        val teacherCount: Int = 0,
        val departmentCount: Int = 0,
        val subjectCount: Int = 0
    )

    private val studentsCollection = "students"
    private val teachersCollection = "teachers"
    private val departmentsCollection = "departments"
    private val subjectsCollection = "subjects"

    suspend fun getDashboardStats(): DashboardStats {
        return DashboardStats(
            studentCount = countOf(studentsCollection),
            teacherCount = countOf(teachersCollection),
            departmentCount = countOf(departmentsCollection),
            subjectCount = countOf(subjectsCollection)
        )
    }

    private suspend fun countOf(collection: String): Int {
        return try {
            firestore.collection(collection)
                .count()
                .get(AggregateSource.SERVER)
                .await()
                .count
                .toInt()
        } catch (e: Exception) {
            // Keep the dashboard usable even if one collection's count
            // query fails (permissions, offline, etc.) -- just show 0
            // for that card instead of crashing.
            0
        }
    }
}