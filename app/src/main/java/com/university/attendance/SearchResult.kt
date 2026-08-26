package com.university.attendance

/**
 * NOT a Firestore model -- a unified wrapper so the search dropdown can
 * show Students and Teachers in one mixed list.
 *
 * UPDATED: now carries enough detail fields to expand IN PLACE when
 * tapped (no navigation to another screen) -- contactNumber and cnic are
 * populated for both types where available, department is shared.
 */
data class SearchResult(
    val id: String,
    val name: String,
    val subtitle: String,          // e.g. "BSSE - Section A" for student, "Assistant Professor" for teacher
    val resultType: SearchResultType,

    // Detail fields shown when the row is expanded in place.
    val department: String = "",
    val contactNumber: String = "",
    val cnicNumber: String = "",
    val extraLine: String = ""     // regNo for student, mainSubject for teacher
    
)

