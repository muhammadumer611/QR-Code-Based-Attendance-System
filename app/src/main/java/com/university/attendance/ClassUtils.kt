package com.university.attendance

/**
 * Helper functions to build a consistent, unique classId from a
 * university + department + program + session + section combination.
 *
 * IMPORTANT: Firestore document IDs cannot contain '/' and should avoid
 * other special characters for readability, so we sanitize each part.
 */
object ClassUtils {

    /**
     * Builds a stable composite classId, e.g.:
     * "COMSATS_CS_BSSE_2022_A"
     *
     * The same 5 inputs will ALWAYS produce the same classId, which is what
     * lets us detect "this class already exists" reliably.
     */
    fun buildClassId(
        universityName: String,
        departmentName: String,
        programName: String,
        session: String,
        section: String
    ): String {
        val parts = listOf(universityName, departmentName, programName, session, section)
            .map { sanitize(it) }
        return parts.joinToString(separator = "_")
    }

    /**
     * Removes/replaces characters that are unsafe or messy inside a Firestore
     * document ID, and normalizes casing/whitespace so that
     * "bsse" and "BSSE " both resolve to the same classId.
     */
    private fun sanitize(input: String): String {
        return input
            .trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "-") // spaces, slashes, punctuation -> "-"
            .trim('-')
    }
}