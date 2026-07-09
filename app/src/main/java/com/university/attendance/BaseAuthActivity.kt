package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.university.attendance.databinding.ActivityAuthBaseBinding

abstract class BaseAuthActivity : AppCompatActivity() {

    protected lateinit var binding: ActivityAuthBaseBinding

    abstract val role: String
    abstract val isSignUp: Boolean
    abstract val accentColor: Int
    abstract val orbDrawableTop: Int
    abstract val orbDrawableBottom: Int
    abstract val iconBgColor: Int
    abstract val roleIcon: String
    abstract val badgeBg: Int
    abstract val tagBg: Int

    // Har subclass batayegi kahan jana hai
    abstract fun getOppositeScreen(): Class<*>
    abstract fun getDashboardScreen(): Class<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = ContextCompat.getColor(this, R.color.bg_dark)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        binding = ActivityAuthBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTheme()
        setupContent()
        setupFields()
        animateViews()

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Main button — seedha dashboard pe
        binding.btnMain.setOnClickListener {
            it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                // Sirf navigate karo, koi validation nahi
                startActivity(Intent(this, getDashboardScreen()))
                finish()
            }.start()
        }

        // Switch — SignIn <-> SignUp
        binding.tvSwitch.setOnClickListener {
            startActivity(Intent(this, getOppositeScreen()))
            finish()
        }
    }

    private fun setupTheme() {
        binding.gridOverlay.background = GridDrawable()
        binding.orbTop.setBackgroundResource(orbDrawableTop)
        binding.orbBottom.setBackgroundResource(orbDrawableBottom)
        binding.accentLine.setBackgroundResource(accentColor)
        binding.iconWrap.setCardBackgroundColor(
            ContextCompat.getColor(this, iconBgColor)
        )
        binding.tvRoleIcon.text = roleIcon
        binding.roleBadge.setBackgroundResource(badgeBg)
        binding.btnMain.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, accentColor)
            )
    }

    private fun setupContent() {
        val roleName = role.lowercase().replaceFirstChar { it.uppercase() }
        binding.tvBadgeText.text = roleName
        binding.tvBadgeText.setTextColor(
            ContextCompat.getColor(this, accentColor)
        )

        if (isSignUp) {
            binding.tvHeading.text = when (role) {
                "ADMIN"   -> "Create\nAccount"
                "TEACHER" -> "Join as\nEducator"
                else      -> "Start Your\nJourney"
            }
            binding.tvSubHeading.text = when (role) {
                "ADMIN"   -> "Set up your admin workspace"
                "TEACHER" -> "Create your teaching account"
                else      -> "Register your student account"
            }
            binding.btnMain.text = "Create $roleName Account"
            binding.tvSwitch.text = "Already have an account? Sign In"
        } else {
            binding.tvHeading.text = when (role) {
                "TEACHER" -> "Welcome\nEducator"
                else      -> "Welcome\nBack"
            }
            binding.tvSubHeading.text = when (role) {
                "ADMIN"   -> "Sign in to your admin panel"
                "TEACHER" -> "Sign in to manage your classes"
                else      -> "Sign in to track your attendance"
            }
            binding.btnMain.text = "Sign In as $roleName"
            binding.tvSwitch.text = "Don't have an account? Sign Up"
        }
    }

    private fun setupFields() {
        val container = binding.fieldsContainer
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        if (isSignUp) {
            val nameRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            nameRow.addView(makeField("First Name", "John", weight = 1f))
            nameRow.addView(makeField("Last Name", "Doe", weight = 1f, marginStart = 10))
            container.addView(nameRow)
        }

        val emailLabel = when (role) {
            "TEACHER" -> "Faculty Email"
            "STUDENT" -> "Student Email"
            else      -> "Email Address"
        }
        container.addView(makeField(emailLabel, "${role.lowercase()}@university.edu"))

        if (isSignUp) {
            when (role) {
                "TEACHER" -> container.addView(makeField("Department", "Computer Science"))
                "STUDENT" -> container.addView(makeField("Roll Number", "BSCS-2021-001"))
            }
        }

        container.addView(makeField("Password", "••••••••", isPassword = true))

        if (!isSignUp) {
            val forgot = TextView(this).apply {
                text = "Forgot password?"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@BaseAuthActivity, accentColor))
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, (-6 * dp).toInt(), 0, (14 * dp).toInt()) }
            }
            container.addView(forgot)
        }
    }

    private fun makeField(
        label: String,
        hint: String,
        isPassword: Boolean = false,
        weight: Float = 0f,
        marginStart: Int = 0
    ): LinearLayout {
        val dp = resources.displayMetrics.density

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = if (weight > 0) {
                LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weight
                ).apply { setMargins((marginStart * dp).toInt(), 0, 0, (12 * dp).toInt()) }
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, (12 * dp).toInt()) }
            }
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_muted))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, (5 * dp).toInt()) }
        }

        val field = EditText(this).apply {
            this.hint = hint
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(this@BaseAuthActivity, R.color.text_muted))
            background = ContextCompat.getDrawable(this@BaseAuthActivity, R.drawable.bg_input_field)
            setPadding(
                (14 * dp).toInt(), (12 * dp).toInt(),
                (14 * dp).toInt(), (12 * dp).toInt()
            )
            if (isPassword) inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        wrapper.addView(labelView)
        wrapper.addView(field)
        return wrapper
    }

    private fun animateViews() {
        val views = listOf(
            binding.btnBack, binding.iconWrap, binding.roleBadge,
            binding.tvHeading, binding.tvSubHeading,
            binding.fieldsContainer, binding.btnMain, binding.tvSwitch
        )
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 40f
            v.animate()
                .alpha(1f).translationY(0f)
                .setDuration(420)
                .setStartDelay((i * 70).toLong())
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}