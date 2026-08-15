package com.university.attendance

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityAdminProfileBinding

/**
 * Screen: Admin -> Profile (opened by tapping the profile icon on the
 * Dashboard header). Shows the signed-in admin's email (from Firebase
 * Auth) and role, with a Logout button that signs out and returns to
 * role selection.
 *
 * NOTE: Display name isn't guaranteed to be set on the FirebaseUser
 * object (depends on how sign-up was done) -- falls back to the email's
 * local part if no display name exists, so the screen never shows blank.
 */
class ActivityAdminProfile : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackHeader.setOnClickListener { finish() }

        loadAdminInfo()

        binding.btnLogout.setOnClickListener { performLogout() }
    }

    private fun loadAdminInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "admin@university.edu"
        val displayName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

        binding.tvAdminName.text = displayName
        binding.tvAdminEmail.text = email
        binding.tvAdminRole.text = "Administrator"
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()

        // Returns to role selection and clears the back stack so the
        // admin can't navigate back into the app with Back after logout.
        val intent = Intent(this, ActivityRoleSelection::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}