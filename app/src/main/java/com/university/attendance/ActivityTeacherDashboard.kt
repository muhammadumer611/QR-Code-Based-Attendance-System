package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityTeacherDashboardBinding

class ActivityTeacherDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var viewModel: TeacherDashboardViewModel
    private lateinit var subjectAdapter: SubjectCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TeacherDashboardViewModel::class.java]

        setupUi()
        setupSubjectsList()
        observeViewModel()

        viewModel.loadDashboard()
    }

    private fun setupUi() {

        // ---------------- MENU ----------------
        binding.menuIcon.setOnClickListener {
            binding.drawerLayout.open()
        }

        // ---------------- PROFILE ----------------
        binding.profileImage.setOnClickListener {
            Toast.makeText(this, "Teacher Profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // ---------------- NOTIFICATIONS ----------------
        binding.notification.setOnClickListener {
            startActivity(Intent(this, ActivityNotifications::class.java))
        }

        // ---------------- TAKE ATTENDANCE (FAB + Quick Action card) ----------------
        val openAttendance = View.OnClickListener {
            Toast.makeText(
                this,
                "QR Attendance will be connected in Phase 2.",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.fabAttendance.setOnClickListener(openAttendance)
        binding.cardTakeAttendance.setOnClickListener(openAttendance)

        // ---------------- QUICK ACTION PLACEHOLDERS ----------------
        // These modules are not implemented yet. We show a clean, honest
        // "coming soon" message instead of navigating to fake/broken screens.
        binding.cardManualAttendance.setOnClickListener {
            Toast.makeText(this, "Manual Attendance coming soon.", Toast.LENGTH_SHORT).show()
        }
        binding.cardUploadNotes.setOnClickListener {
            Toast.makeText(this, "Upload Notes coming soon.", Toast.LENGTH_SHORT).show()
        }
        binding.cardAssignmentsAction.setOnClickListener {
            Toast.makeText(this, "Assignments module coming soon.", Toast.LENGTH_SHORT).show()
        }
        binding.cardStudents.setOnClickListener {
            Toast.makeText(this, "Students list coming soon.", Toast.LENGTH_SHORT).show()
        }
        binding.cardResults.setOnClickListener {
            Toast.makeText(this, "Results module coming soon.", Toast.LENGTH_SHORT).show()
        }

        // ---------------- RETRY (Error state) ----------------
        binding.btnRetry.setOnClickListener {
            viewModel.loadDashboard()
        }

        // ---------------- DRAWER ----------------
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_dashboard -> {
                    binding.drawerLayout.closeDrawers()
                    true
                }

                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this, ActivityTeacherSignIn::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    true
                }

                else -> {
                    binding.drawerLayout.closeDrawers()
                    Toast.makeText(
                        this,
                        "This module will be connected in the next phase.",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
            }
        }
    }

    private fun setupSubjectsList() {
        subjectAdapter = SubjectCardAdapter { subject ->
            // Class Detail screen isn't built yet -- see ActivityTeacherClasses
            // for why. For now every subject tap opens the full My Classes list.
            startActivity(Intent(this, ActivityTeacherClasses::class.java))
        }

        binding.tvViewAllSubjects.setOnClickListener {
            startActivity(Intent(this, ActivityTeacherClasses::class.java))
        }

        binding.recyclerMySubjects.apply {
            layoutManager = LinearLayoutManager(
                this@ActivityTeacherDashboard,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = subjectAdapter
        }
    }

    private fun observeViewModel() {

        viewModel.uiState.observe(this) { state ->
            when (state) {

                is TeacherDashboardViewModel.UiState.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.errorState.visibility = View.GONE
                    binding.scrollContent.visibility = View.GONE
                }

                is TeacherDashboardViewModel.UiState.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.errorState.visibility = View.GONE
                    binding.scrollContent.visibility = View.VISIBLE
                }

                is TeacherDashboardViewModel.UiState.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.scrollContent.visibility = View.GONE
                    binding.errorState.visibility = View.VISIBLE
                    binding.txtErrorMessage.text = state.message

                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.teacher.observe(this) { teacher ->
            binding.txtTeacher.text = teacher.fullName.ifBlank { "Teacher" }

            binding.txtRole.text = buildString {
                if (teacher.designation.isNotBlank()) {
                    append(teacher.designation)
                }

                if (teacher.designation.isNotBlank() && teacher.departmentName.isNotBlank()) {
                    append(" • ")
                }

                if (teacher.departmentName.isNotBlank()) {
                    append(teacher.departmentName)
                }

                if (isBlank()) {
                    append("Faculty")
                }
            }
        }

        viewModel.subjects.observe(this) { subjects ->
            binding.tvAssignedSubjects.text = subjects.size.toString()

            subjectAdapter.submitList(subjects)

            binding.recyclerMySubjects.visibility =
                if (subjects.isEmpty()) View.GONE else View.VISIBLE
            binding.txtSubjectsEmpty.visibility =
                if (subjects.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalStudents.observe(this) { count ->
            binding.tvTotalStudents.text = count.toString()
        }

        viewModel.attendancePercentage.observe(this) { percentage ->
            binding.tvAttendancePercentage.text = "$percentage%"
            binding.progressAttendance.progress = percentage
            binding.tvAttendanceSummary.text = "$percentage% Students Present"
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
            binding.drawerLayout.closeDrawer(binding.navigationView)
        } else {
            super.onBackPressed()
        }
    }
}