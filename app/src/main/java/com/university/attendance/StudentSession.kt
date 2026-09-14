package com.university.attendance

import android.content.Context

object StudentSession {
    private const val PREF = "student_session"
    private const val ID = "student_id"; private const val UID = "auth_uid"; private const val NAME = "student_name"; private const val EMAIL = "student_email"; private const val GENERATED = "student_generated_id"; private const val CLASS = "class_id"; private const val SEM = "semester"; private const val SESSION = "session"; private const val SECTION = "section"
    fun save(context: Context, s: Student) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(ID,s.studentId).putString(UID,s.authUid).putString(NAME,s.fullName).putString(EMAIL,s.personalEmail).putString(GENERATED,s.studentGeneratedId).putString(CLASS,s.classId).putInt(SEM,s.semester).putString(SESSION,s.session).putString(SECTION,s.section).apply() }
    fun getStudentId(c: Context) = c.getSharedPreferences(PREF,0).getString(ID,null)
    fun getAuthUid(c: Context) = c.getSharedPreferences(PREF,0).getString(UID,null)
    fun getStudentName(c: Context) = c.getSharedPreferences(PREF,0).getString(NAME,null)
    fun isLoggedIn(c: Context) = !getStudentId(c).isNullOrBlank()
    fun clear(c: Context) { c.getSharedPreferences(PREF,0).edit().clear().apply() }
}
