package com.university.attendance

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Schedule(

    @get:Exclude
    @set:Exclude
    var scheduleId: String = "",

    var teacherAuthUid: String = "",

    var teacherName: String = "",

    var fileName: String = "",

    var note: String = "",

    var uploadedBy: String = "Admin",

    // Firebase Storage ka path
    var storagePath: String = "",

    // Firebase Storage download URL
    var fileUrl: String = "",

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

        "storagePath" to storagePath,

        "fileUrl" to fileUrl,

        "uploadedAt" to FieldValue.serverTimestamp()
    )
}