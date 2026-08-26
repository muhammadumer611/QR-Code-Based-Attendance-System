package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Powers the Dashboard's live search box. Searches Students and Teachers
 * by name prefix and merges results into one list.
 *
 * NOTE: Firestore doesn't support case-insensitive "contains" queries
 * natively. This uses a prefix-range query (>= query, <= query + '\uf8ff'),
 * which matches Firestore's standard "starts with" search pattern -- so
 * search matches names starting with what's typed, not partial/middle
 * matches. This is the same technique recommended in Firebase's own docs
 * for simple search without a third-party search service.
 */
class SearchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val studentsRef = firestore.collection("students")
    private val teachersRef = firestore.collection("teachers")

    suspend fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val trimmed = query.trim()
        val endBound = trimmed + "\uf8ff"

        val studentResults = try {
            studentsRef
                .orderBy("fullName")
                .whereGreaterThanOrEqualTo("fullName", trimmed)
                .whereLessThanOrEqualTo("fullName", endBound)
                .limit(10)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val name = doc.getString("fullName") ?: return@mapNotNull null
                    val program = doc.getString("programName") ?: ""
                    val section = doc.getString("section") ?: ""
                    SearchResult(
                        id = doc.id,
                        name = name,
                        subtitle = if (program.isNotBlank()) "$program - Section $section" else "Student",
                        resultType = SearchResultType.STUDENT
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }

        val teacherResults = try {
            teachersRef
                .orderBy("fullName")
                .whereGreaterThanOrEqualTo("fullName", trimmed)
                .whereLessThanOrEqualTo("fullName", endBound)
                .limit(10)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val name = doc.getString("fullName") ?: return@mapNotNull null
                    val designation = doc.getString("designation") ?: "Teacher"
                    SearchResult(
                        id = doc.id,
                        name = name,
                        subtitle = designation,
                        resultType = SearchResultType.TEACHER
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }

        return (studentResults + teacherResults).sortedBy { it.name }
    }
}