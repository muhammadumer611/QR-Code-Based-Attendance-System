package com.university.attendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityAdminProfileBinding

/**
 * Screen: Admin -> Profile (opened by tapping the profile icon on the
 * Dashboard header).
 *
 * UPDATED: Name and photo are now editable and saved LOCALLY on the
 * device via LocalProfileStore (SharedPreferences + internal file
 * storage) -- per current requirements, this does NOT sync to Firebase.
 * Local edits always take priority for display over the Firebase Auth
 * email-derived name, everywhere this info needs to show (Dashboard
 * header, this screen).
 */
class ActivityAdminProfile : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            binding.imgProfilePhoto.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackHeader.setOnClickListener { finish() }

        loadAdminInfo()

        binding.imgProfilePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { performLogout() }
    }

    private fun loadAdminInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "admin@university.edu"

        // Local edits take priority; fall back to Firebase display name,
        // then to a name derived from the email, so the field is never blank.
        val savedName = LocalProfileStore.getDisplayName(this)
        val fallbackName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

        binding.etAdminName.setText(savedName ?: fallbackName)
        binding.tvAdminEmail.text = email
        binding.tvAdminRole.text = "Administrator"

        val savedPhotoPath = LocalProfileStore.getPhotoPath(this)
        if (savedPhotoPath != null) {
            binding.imgProfilePhoto.setImageURI(Uri.fromFile(java.io.File(savedPhotoPath)))
        }
    }

    private fun saveProfile() {
        val newName = binding.etAdminName.text.toString().trim()
        if (newName.isBlank()) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        LocalProfileStore.saveDisplayName(this, newName)

        selectedPhotoUri?.let { uri ->
            val savedPath = LocalProfileStore.savePhoto(this, uri)
            if (savedPath == null) {
                Toast.makeText(this, "Name saved, but photo failed to save.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, ActivityRoleSelection::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}