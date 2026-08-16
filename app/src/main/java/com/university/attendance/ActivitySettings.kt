package com.university.attendance

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivitySettingsBinding

/**
 * Screen: Admin -> Settings.
 *
 * Three sections:
 *   1. Appearance -- shows the current theme (Dark). Only Dark is
 *      supported right now, so this is informational only; the toggle
 *      is disabled with a note, ready to wire up if/when a Light theme
 *      is added later.
 *   2. Change Password -- re-authenticates with the current password,
 *      then updates via FirebaseAuth. Required because Firebase requires
 *      a recent sign-in before allowing a password change, for security.
 *   3. About -- app name and version (from PackageInfo), read-only.
 */
class ActivitySettings : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackHeader.setOnClickListener { finish() }

        setupAppearanceSection()
        setupChangePassword()
        setupAboutSection()
    }

    private fun setupAppearanceSection() {
        // Only Dark theme exists right now -- shown as the selected
        // state, switch disabled since there's nothing to toggle to yet.
        binding.switchDarkTheme.isChecked = true
        binding.switchDarkTheme.isEnabled = false
        binding.tvThemeNote.text = "Light theme coming soon"
    }

    private fun setupChangePassword() {
        binding.rowChangePassword.setOnClickListener { showChangePasswordDialog() }
    }

    private fun showChangePasswordDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val currentPasswordInput = EditText(this).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val newPasswordInput = EditText(this).apply {
            hint = "New Password (min 6 characters)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        container.addView(currentPasswordInput)
        container.addView(newPasswordInput)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                attemptPasswordChange(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun attemptPasswordChange(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email

        if (user == null || email == null) {
            Toast.makeText(this, "No signed-in user found.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase requires a recent sign-in before allowing sensitive
        // operations like password changes -- re-authenticate first.
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update password: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Current password is incorrect.", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupAboutSection() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0"
        }
        binding.tvAppVersion.text = "Version $versionName"
    }
}