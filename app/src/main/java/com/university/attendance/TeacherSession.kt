package com.university.attendance

import android.content.Context

object TeacherSession {

    private const val PREF_NAME = "teacher_session"

    private const val KEY_TEACHER_ID = "teacher_id"
    private const val KEY_AUTH_UID = "auth_uid"
    private const val KEY_NAME = "teacher_name"
    private const val KEY_EMAIL = "teacher_email"

    fun save(
        context: Context,
        teacher: Teacher
    ) {

        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putString(KEY_TEACHER_ID, teacher.teacherId)
            .putString(KEY_AUTH_UID, teacher.authUid)
            .putString(KEY_NAME, teacher.fullName)
            .putString(KEY_EMAIL, teacher.email)
            .apply()
    }

    fun getTeacherId(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEACHER_ID, null)
    }

    fun getAuthUid(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AUTH_UID, null)
    }

    fun getTeacherName(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NAME, null)
    }

    fun getTeacherEmail(context: Context): String? {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, null)
    }

    fun isLoggedIn(context: Context): Boolean {
        return !getTeacherId(context).isNullOrBlank()
    }

    fun clear(context: Context) {

        context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}