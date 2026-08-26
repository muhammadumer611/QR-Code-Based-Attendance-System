package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.WindowCompat
import com.university.attendance.databinding.ActivityOnBoardingScreenBinding

class ActivityOnBoardingScreen : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingScreenBinding

    private val list = listOf(
        OnboardingModel(R.drawable.onboard1, "Mark Attendance\nInstantly", "Scan QR codes to mark your presence in seconds. No paper, no hassle.", "Smart Attendance"),
        OnboardingModel(R.drawable.onboard2, "No More Fake\nAttendance", "Location-verified QR ensures only present students can check in.", "Zero Proxy"),
        OnboardingModel(R.drawable.onboard3, "Scan. Done.\nThat Simple.", "Your teacher shows the QR, you scan it. Done in under 3 seconds.", "Fast & Easy")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_dark)

        // Status bar icons light rakho (dark background pe white icons)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityOnBoardingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingAdapter(list)
        setupDots(0)

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                binding.btnNext.text = if (position == list.size - 1) "Get Started" else "Next"
            }
        })

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < list.size - 1) {
                binding.viewPager.currentItem += 1
            } else {
                startActivity(Intent(this, ActivityRoleSelection::class.java))
                finish()
            }
        }

        binding.tvSkip.setOnClickListener {
            startActivity(Intent(this, ActivityRoleSelection::class.java))
            finish()
        }
    }

    private fun setupDots(position: Int) {
        binding.dotsLayout.removeAllViews()
        for (i in list.indices) {
            val dot = ImageView(this)
            val params = android.widget.LinearLayout.LayoutParams(
                if (i == position) dpToPx(24) else dpToPx(8),
                dpToPx(8)
            )
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0)
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            )
            binding.dotsLayout.addView(dot)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}