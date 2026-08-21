package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Schedule(

    @get:Exclude
    @set:Exclude
    var scheduleId: String = "",

    // Teacher ki Firebase Authentication UID
    var teacherAuthUid: String = "",

    // Teacher ka naam sirf display ke liye
    var teacherName: String = "",

    var fileName: String = "",

    var note: String = "",

    var uploadedBy: String = "Admin",

    @ServerTimestamp
    var uploadedAt: Date? = null
) {

    constructor() : this(scheduleId = "")

    @Exclude
    fun toMap(): Map<String, Any?> = mapOf(

        "teacherAuthUid" to teacherAuthUid,

        "teacherName" to teacherName,

        "fileName" to fileName,

        "note" to note,

        "uploadedBy" to uploadedBy,

        "uploadedAt" to FieldValue.serverTimestamp()
    )
}