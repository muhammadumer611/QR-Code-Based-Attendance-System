package com.university.attendance

import android.content.Intent
import com.university.attendance.R

class ActivityAdminSignUp : BaseAuthActivity() {
    override val role = "ADMIN"
    override val isSignUp = true
    override val accentColor = R.color.accent_blue
    override val orbDrawableTop = R.drawable.bg_ord_blue
    override val orbDrawableBottom = R.drawable.bg_ord_blue
    override val iconBgColor = R.color.icon_admin_bg
    override val roleIcon = "🛡️"
    override val badgeBg = R.drawable.bg_badge_blue
    override val tagBg = R.drawable.bg_tag_blue
    override fun getOppositeScreen() = ActivityAdminSignIn::class.java
    override fun getDashboardScreen() = MainActivity::class.java
}