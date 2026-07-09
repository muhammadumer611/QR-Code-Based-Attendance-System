package com.university.attendance

import android.content.Intent

class ActivityStudentSignUp : BaseAuthActivity() {

    override val role = "STUDENT"
    override val isSignUp = true
    override val accentColor = R.color.accent_purple
    override val orbDrawableTop = R.drawable.bg_orb_purple
    override val orbDrawableBottom = R.drawable.bg_orb_purple
    override val iconBgColor = R.color.icon_student_bg
    override val roleIcon = "🎓"
    override val badgeBg = R.drawable.bg_badge_purple
    override val tagBg = R.drawable.bg_tag_purple
    override fun getOppositeScreen() = ActivityStudentSignIn::class.java
    override fun getDashboardScreen() = MainActivity::class.java
}