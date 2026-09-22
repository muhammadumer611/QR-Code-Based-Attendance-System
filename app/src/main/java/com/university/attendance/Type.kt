package com.university.attendance

/** Shared type tags used by both ActivityLog and Notification, so icon logic stays consistent. */
object Type{
    const val STUDENT_ADDED = "student_added"
    const val TEACHER_ADDED = "teacher_added"
    const val DEPARTMENT_ADDED = "department_added"
    const val SUBJECT_ADDED = "subject_added"
    const val ATTENDANCE_MARKED = "attendance_marked"
    const val TEACHER_ASSIGNED = "teacher_assigned"
    const val LOW_ATTENDANCE = "low_attendance"
}