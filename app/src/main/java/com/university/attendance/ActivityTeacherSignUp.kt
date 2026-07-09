package com.university.attendance

import android.content.Intent

class ActivityTeacherSignUp : BaseAuthActivity() {
    override val role = "TEACHER"
    override val isSignUp = true
    override val accentColor = R.color.accent_teal
    override val orbDrawableTop = R.drawable.bg_orb_teal
    override val orbDrawableBottom = R.drawable.bg_orb_teal
    override val iconBgColor = R.color.icon_teacher_bg
    override val roleIcon = "📋"
    override val badgeBg = R.drawable.bg_badge_teal
    override val tagBg = R.drawable.bg_tag_teal
    override fun getOppositeScreen() = ActivityTeacherSignIn::class.java
    override fun getDashboardScreen() = MainActivity::class.java
}