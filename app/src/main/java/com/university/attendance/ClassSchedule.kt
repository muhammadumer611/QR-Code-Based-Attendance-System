package com.university.attendance

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ClassSchedule(

    @get:Exclude
    @set:Exclude
    var scheduleId: String = "",

    // ============================================================
    // TEACHER
    // ============================================================

    var teacherId: String = "",
    var teacherName: String = "",
    var teacherAuthUid: String = "",

    // ============================================================
    // CLASS
    // ============================================================

    var className: String = "",

    var subjectId: String = "",
    var subjectName: String = "",
    var courseCode: String = "",

    var roomNumber: Int = 0,

    // ============================================================
    // CLASS FILTER
    // ============================================================

    var programName: String = "",
    var semester: Int = 1,
    var session: String = "",
    var section: String = "",

    // ============================================================
    // DATE
    // ============================================================

    var date: String = "",
    var dayName: String = "",

    // ============================================================
    // TIME
    // ============================================================

    var startTime: String = "",
    var endTime: String = "",

    // ============================================================
    // PERIOD
    // ============================================================

    var periodType: String = "Weekly",

    // ============================================================
    // NOTE
    // ============================================================

    var note: String = "",

    var createdBy: String = "Admin",

    @ServerTimestamp
    var createdAt: Date? = null

) {

    // ============================================================
    // FIRESTORE -> CLASS SCHEDULE
    //
    // Important:
    // roomNumber / semester may exist in Firestore as either
    // Number or String because of old records.
    //
    // This mapper safely handles both.
    // ============================================================

    companion object {

        fun fromDocument(
            document: DocumentSnapshot
        ): ClassSchedule {

            val roomNumber =
                when (
                    val value =
                        document.get("roomNumber")
                ) {

                    is Number ->
                        value.toInt()

                    is String ->
                        value.trim().toIntOrNull()
                            ?: 0

                    else ->
                        0
                }

            val semester =
                when (
                    val value =
                        document.get("semester")
                ) {

                    is Number ->
                        value.toInt()

                    is String ->
                        value.trim().toIntOrNull()
                            ?: 1

                    else ->
                        1
                }

            return ClassSchedule(

                scheduleId =
                    document.id,

                teacherId =
                    document.getString("teacherId")
                        .orEmpty(),

                teacherName =
                    document.getString("teacherName")
                        .orEmpty(),

                teacherAuthUid =
                    document.getString("teacherAuthUid")
                        .orEmpty(),

                className =
                    document.getString("className")
                        .orEmpty(),

                subjectId =
                    document.getString("subjectId")
                        .orEmpty(),

                subjectName =
                    document.getString("subjectName")
                        .orEmpty(),

                courseCode =
                    document.getString("courseCode")
                        .orEmpty(),

                roomNumber =
                    roomNumber,

                programName =
                    document.getString("programName")
                        .orEmpty(),

                semester =
                    semester,

                session =
                    document.getString("session")
                        .orEmpty(),

                section =
                    document.getString("section")
                        .orEmpty(),

                date =
                    document.getString("date")
                        .orEmpty(),

                dayName =
                    document.getString("dayName")
                        .orEmpty(),

                startTime =
                    document.getString("startTime")
                        .orEmpty(),

                endTime =
                    document.getString("endTime")
                        .orEmpty(),

                periodType =
                    document.getString("periodType")
                        ?: "Weekly",

                note =
                    document.getString("note")
                        .orEmpty(),

                createdBy =
                    document.getString("createdBy")
                        ?: "Admin",

                createdAt =
                    document.getDate("createdAt")
            )
        }
    }

    // ============================================================
    // CLASS SCHEDULE -> FIRESTORE
    // ============================================================

    @Exclude
    fun toMap(): Map<String, Any?> {

        return mapOf(

            "teacherId" to teacherId,

            "teacherName" to teacherName,

            "teacherAuthUid" to teacherAuthUid,

            "className" to className,

            "subjectId" to subjectId,

            "subjectName" to subjectName,

            "courseCode" to courseCode,

            "roomNumber" to roomNumber,

            "programName" to programName,

            "semester" to semester,

            "session" to session,

            "section" to section,

            "date" to date,

            "dayName" to dayName,

            "startTime" to startTime,

            "endTime" to endTime,

            "periodType" to periodType,

            "note" to note,

            "createdBy" to createdBy,

            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}