package com.university.attendance

/**
 * NOT a Firestore model -- this represents ONE ROW in the Manual Update
 * register screen: a student plus their current Present/Absent toggle
 * state for the selected Department + Class + Subject + Date.
 *
 * isPresent starts as:
 *   - true, if an existing "present" attendance_records document was
 *     found for this student+subject+date (i.e. loaded for editing)
 *   - false, otherwise (default -- not yet marked)
 */
data class AttendanceRosterRow(
    val student: Student,
    var isPresent: Boolean
)