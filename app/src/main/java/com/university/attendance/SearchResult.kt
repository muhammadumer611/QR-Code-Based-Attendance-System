package com.university.attendance

/**
 * NOT a Firestore model -- a unified wrapper so the search dropdown can
 * show Students and Teachers in one mixed list, each tagged with its
 * type so the adapter knows which icon/subtitle to show and where
 * tapping it should navigate.
 */
data class SearchResult(
    val id: String,
    val name: String,
    val subtitle: String,      // e.g. "BSSE - Section A" for student, "Assistant Professor" for teacher
    val resultType: SearchResultType
)

