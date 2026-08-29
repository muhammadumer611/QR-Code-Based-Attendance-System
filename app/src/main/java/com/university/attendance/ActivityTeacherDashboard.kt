package com.university.attendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityTeacherDashboardBinding

class ActivityTeacherDashboard : AppCompatActivity() {

    private lateinit var binding:
            ActivityTeacherDashboardBinding

    private lateinit var viewModel:
            TeacherDashboardViewModel

    private lateinit var subjectAdapter:
            SubjectCardAdapter

    private lateinit var todayAdapter:
            TeacherTodayClassAdapter

    private lateinit var weekAdapter:
            TeacherTodayClassAdapter

    private lateinit var semesterAdapter:
            TeacherTodayClassAdapter


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)


        binding =
            ActivityTeacherDashboardBinding
                .inflate(layoutInflater)

        setContentView(binding.root)


        viewModel =
            ViewModelProvider(this)[
                TeacherDashboardViewModel::class.java
            ]


        setupBackPressHandler()

        setupUi()

        setupSubjectsList()

        setupClassLists()

        observeViewModel()


        viewModel.loadDashboard()
    }


    // ============================================================
    // BACK PRESS
    // ============================================================

    private fun setupBackPressHandler() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (
                        binding.drawerLayout.isDrawerOpen(
                            binding.navigationView
                        )
                    ) {

                        binding.drawerLayout.closeDrawers()

                    } else {

                        isEnabled = false

                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }


    // ============================================================
    // UI
    // ============================================================

    private fun setupUi() {

        binding.menuIcon.setOnClickListener {

            binding.drawerLayout.openDrawer(
                binding.navigationView
            )
        }


        binding.profileImage.setOnClickListener {

            Toast.makeText(
                this,
                "Teacher Profile coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }


        binding.notification.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ActivityNotifications::class.java
                )
            )
        }


        val openAttendance =
            View.OnClickListener {

                Toast.makeText(
                    this,
                    "QR Attendance will be connected in Phase 2.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.fabAttendance
            .setOnClickListener(
                openAttendance
            )


        binding.cardTakeAttendance
            .setOnClickListener(
                openAttendance
            )


        binding.cardManualAttendance
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Manual Attendance coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.cardUploadNotes
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Upload Notes coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.cardAssignmentsAction
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Assignments module coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.cardAssignments
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Assignments module coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.cardStudents
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Students list coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.cardResults
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Results module coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        binding.btnRetry
            .setOnClickListener {

                viewModel.loadDashboard()
            }


        setupDrawer()
    }


    // ============================================================
    // SUBJECTS
    // ============================================================

    private fun setupSubjectsList() {

        subjectAdapter =
            SubjectCardAdapter { _ ->

                startActivity(
                    Intent(
                        this,
                        ActivityTeacherClasses::class.java
                    )
                )
            }


        binding.tvViewAllSubjects
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ActivityTeacherClasses::class.java
                    )
                )
            }


        binding.recyclerMySubjects.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherDashboard,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            adapter =
                subjectAdapter

            setHasFixedSize(true)
        }
    }


    // ============================================================
    // CLASS RECYCLERS
    // ============================================================

    private fun setupClassLists() {

        // --------------------------------------------------------
        // TODAY
        // --------------------------------------------------------

        todayAdapter =
            TeacherTodayClassAdapter {

                openClass(it)
            }


        binding.recyclerTodayClasses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherDashboard
                )

            adapter =
                todayAdapter
        }


        // --------------------------------------------------------
        // THIS WEEK
        // --------------------------------------------------------

        weekAdapter =
            TeacherTodayClassAdapter {

                openClass(it)
            }


        binding.recyclerWeeklyClasses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherDashboard
                )

            adapter =
                weekAdapter
        }


        // --------------------------------------------------------
        // SEMESTER
        // --------------------------------------------------------

        semesterAdapter =
            TeacherTodayClassAdapter {

                openClass(it)
            }


        binding.recyclerSemesterClasses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherDashboard
                )

            adapter =
                semesterAdapter
        }
    }


    // ============================================================
    // CLASS CLICK
    // ============================================================

    private fun openClass(
        classSchedule: ClassSchedule
    ) {

        /*
         * Abhi direct class detail activity tumhare
         * existing project mein defined nahi hai.
         *
         * Isliye click par selected class ka basic
         * information show kar rahe hain.
         */

        Toast.makeText(
            this,
            "${classSchedule.subjectName} • ${classSchedule.className}",
            Toast.LENGTH_SHORT
        ).show()
    }


    // ============================================================
    // OBSERVE
    // ============================================================

    private fun observeViewModel() {

        // --------------------------------------------------------
        // UI STATE
        // --------------------------------------------------------

        viewModel.uiState
            .observe(this) { state ->

                when (state) {

                    is TeacherDashboardViewModel.UiState.Loading -> {

                        binding.loadingOverlay.visibility =
                            View.VISIBLE

                        binding.errorState.visibility =
                            View.GONE

                        binding.scrollContent.visibility =
                            View.GONE
                    }


                    is TeacherDashboardViewModel.UiState.Success -> {

                        binding.loadingOverlay.visibility =
                            View.GONE

                        binding.errorState.visibility =
                            View.GONE

                        binding.scrollContent.visibility =
                            View.VISIBLE
                    }


                    is TeacherDashboardViewModel.UiState.Error -> {

                        binding.loadingOverlay.visibility =
                            View.GONE

                        binding.scrollContent.visibility =
                            View.GONE

                        binding.errorState.visibility =
                            View.VISIBLE

                        binding.txtErrorMessage.text =
                            state.message
                    }
                }
            }


        // --------------------------------------------------------
        // TEACHER
        // --------------------------------------------------------

        viewModel.teacher
            .observe(this) { teacher ->

                binding.txtTeacher.text =
                    teacher.fullName.ifBlank {
                        "Teacher"
                    }


                binding.txtRole.text =
                    buildString {

                        if (
                            teacher.designation.isNotBlank()
                        ) {

                            append(
                                teacher.designation
                            )
                        }


                        if (
                            teacher.designation.isNotBlank() &&
                            teacher.departmentName.isNotBlank()
                        ) {

                            append(" • ")
                        }


                        if (
                            teacher.departmentName.isNotBlank()
                        ) {

                            append(
                                teacher.departmentName
                            )
                        }


                        if (isBlank()) {

                            append(
                                "Faculty"
                            )
                        }
                    }
            }


        // --------------------------------------------------------
        // SUBJECTS
        // --------------------------------------------------------

        viewModel.subjects
            .observe(this) { subjects ->

                binding.tvAssignedSubjects.text =
                    subjects.size.toString()


                subjectAdapter.submitList(
                    subjects
                )


                if (subjects.isEmpty()) {

                    binding.recyclerMySubjects.visibility =
                        View.GONE

                    binding.txtSubjectsEmpty.visibility =
                        View.VISIBLE

                } else {

                    binding.recyclerMySubjects.visibility =
                        View.VISIBLE

                    binding.txtSubjectsEmpty.visibility =
                        View.GONE
                }
            }


        // --------------------------------------------------------
        // STUDENTS
        // --------------------------------------------------------

        viewModel.totalStudents
            .observe(this) { count ->

                binding.tvTotalStudents.text =
                    count.toString()
            }


        // --------------------------------------------------------
        // ATTENDANCE
        // --------------------------------------------------------

        viewModel.attendancePercentage
            .observe(this) { percentage ->

                binding.tvAttendancePercentage.text =
                    "$percentage%"


                binding.progressAttendance.progress =
                    percentage


                binding.tvAttendanceSummary.text =
                    "$percentage% Students Present"
            }


        // --------------------------------------------------------
        // TODAY
        // --------------------------------------------------------

        viewModel.todayClasses
            .observe(this) { classes ->

                todayAdapter.submitList(
                    classes
                )


                if (classes.isEmpty()) {

                    binding.recyclerTodayClasses.visibility =
                        View.GONE

                    binding.txtTodayClassesEmpty.visibility =
                        View.VISIBLE

                } else {

                    binding.recyclerTodayClasses.visibility =
                        View.VISIBLE

                    binding.txtTodayClassesEmpty.visibility =
                        View.GONE
                }
            }


        // --------------------------------------------------------
        // THIS WEEK
        // --------------------------------------------------------

        viewModel.weekClasses
            .observe(this) { classes ->

                weekAdapter.submitList(
                    classes
                )


                if (classes.isEmpty()) {

                    binding.recyclerWeeklyClasses.visibility =
                        View.GONE

                    binding.txtWeeklyClassesEmpty.visibility =
                        View.VISIBLE

                } else {

                    binding.recyclerWeeklyClasses.visibility =
                        View.VISIBLE

                    binding.txtWeeklyClassesEmpty.visibility =
                        View.GONE
                }
            }


        // --------------------------------------------------------
        // SEMESTER
        // --------------------------------------------------------

        viewModel.semesterClasses
            .observe(this) { classes ->

                semesterAdapter.submitList(
                    classes
                )


                if (classes.isEmpty()) {

                    binding.recyclerSemesterClasses.visibility =
                        View.GONE

                    binding.txtSemesterClassesEmpty.visibility =
                        View.VISIBLE

                } else {

                    binding.recyclerSemesterClasses.visibility =
                        View.VISIBLE

                    binding.txtSemesterClassesEmpty.visibility =
                        View.GONE
                }
            }


        // --------------------------------------------------------
        // SCHEDULE PDF
        // --------------------------------------------------------

        viewModel.schedule
            .observe(this) { schedule ->

                if (
                    schedule == null ||
                    schedule.fileUrl.isBlank()
                ) {

                    binding.cardSchedulePdf.visibility =
                        View.GONE

                    binding.txtSchedulePdfEmpty.visibility =
                        View.VISIBLE

                    binding.btnViewSchedulePdf.isEnabled =
                        false

                } else {

                    binding.cardSchedulePdf.visibility =
                        View.VISIBLE

                    binding.txtSchedulePdfEmpty.visibility =
                        View.GONE

                    binding.tvSchedulePdfName.text =
                        schedule.fileName.ifBlank {
                            "Class Schedule PDF"
                        }

                    binding.tvSchedulePdfNote.text =
                        schedule.note.ifBlank {
                            "Schedule uploaded by Admin."
                        }

                    binding.btnViewSchedulePdf.isEnabled =
                        true

                    binding.btnViewSchedulePdf.setOnClickListener {

                        try {

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        schedule.fileUrl
                                    )
                                )

                            intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )

                            startActivity(intent)

                        } catch (_: Exception) {

                            Toast.makeText(
                                this,
                                "No PDF viewer or browser is available.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
    }


    // ============================================================
    // DRAWER
    // ============================================================

    private fun setupDrawer() {

        binding.navigationView
            .setNavigationItemSelectedListener { item ->

                when (item.itemId) {

                    R.id.nav_dashboard -> {

                        binding.drawerLayout
                            .closeDrawers()

                        true
                    }


                    R.id.nav_logout -> {

                        FirebaseAuth
                            .getInstance()
                            .signOut()


                        val intent =
                            Intent(
                                this,
                                ActivityTeacherSignIn::class.java
                            )


                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK


                        startActivity(intent)

                        true
                    }


                    else -> {

                        binding.drawerLayout
                            .closeDrawers()


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
}