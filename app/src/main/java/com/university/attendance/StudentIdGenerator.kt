package com.university.attendance

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object StudentIdGenerator {

    suspend fun generateStudentId(
        program: String,
        session: String
    ): String {

        val db = FirebaseFirestore.getInstance()

        val cleanProgram = program
            .trim()
            .uppercase()
            .replace(" ", "")

        val cleanSession = session
            .trim()
            .uppercase()

        val prefix = "$cleanProgram-$cleanSession"

        val snapshot = db.collection("Students")
            .whereGreaterThanOrEqualTo(
                "studentId",
                prefix
            )
            .whereLessThan(
                "studentId",
                prefix + "\uf8ff"
            )
            .get()
            .await()

        var highestNumber = 0

        for (document in snapshot.documents) {

            val id = document.getString("studentId")
                ?: continue

            val numberPart = id
                .removePrefix(prefix)
                .toIntOrNull()

            if (numberPart != null && numberPart > highestNumber) {
                highestNumber = numberPart
            }
        }

        val nextNumber = highestNumber + 1

        return "$prefix$nextNumber"
    }
}