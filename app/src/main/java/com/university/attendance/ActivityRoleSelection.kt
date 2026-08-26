package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.university.attendance.databinding.ActivityRoleSelectionBinding

class ActivityRoleSelection : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_dark)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Grid — directly GridDrawable set karo, no import needed same package mein
        binding.gridOverlay.background = GridDrawable()

        // Pulse animation on dot
        val pulse = android.view.animation.ScaleAnimation(
            1f, 1.4f, 1f, 1.4f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 900
            repeatMode = android.view.animation.Animation.REVERSE
            repeatCount = android.view.animation.Animation.INFINITE
            interpolator = DecelerateInterpolator()
        }
        binding.pulseDot.startAnimation(pulse)

        animateViews()

        binding.cardAdmin.setOnClickListener {
            it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                startActivity(Intent(this, ActivityAdminSignIn::class.java))
            }.start()
        }

        binding.cardTeacher.setOnClickListener {
            it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                startActivity(Intent(this, ActivityTeacherSignIn::class.java))
            }.start()
        }

        binding.cardStudent.setOnClickListener {
            it.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                startActivity(Intent(this, ActivityStudentSignIn::class.java))
            }.start()
        }
    }

    private fun animateViews() {
        val views = listOf(
            binding.badgeLayout,
            binding.tvHeading,
            binding.tvSubtext,
            binding.cardAdmin,
            binding.cardTeacher,
            binding.cardStudent
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay((index * 90).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}