package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityTeacherDashboardBinding

class ActivityTeacherDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherDashboardBinding

    private lateinit var viewModel: TeacherDashboardViewModel
    private lateinit var todayClassAdapter: TeacherTodayClassAdapter

    private lateinit var subjectAdapter: SubjectCardAdapter


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityTeacherDashboardBinding
                .inflate(layoutInflater)


        setContentView(
            binding.root
        )


        viewModel =
            ViewModelProvider(this)[
                TeacherDashboardViewModel::class.java
            ]


        setupUi()

        setupSubjectsList()

        setupTodayClasses()

        observeViewModel()

        setupBackPressHandler()


        // --------------------------------------------------------
        // LOAD DASHBOARD
        // --------------------------------------------------------

        viewModel.loadDashboard()
    }


    // ============================================================
    // UI SETUP
    // ============================================================

    private fun setupUi() {


        // --------------------------------------------------------
        // MENU
        // --------------------------------------------------------

        binding.menuIcon.setOnClickListener {

            binding.drawerLayout.openDrawer(
                binding.navigationView
            )
        }


        // --------------------------------------------------------
        // PROFILE
        // --------------------------------------------------------

        binding.profileImage.setOnClickListener {

            Toast.makeText(
                this,
                "Teacher Profile coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }


        // --------------------------------------------------------
        // NOTIFICATIONS
        // --------------------------------------------------------

        binding.notification.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ActivityNotifications::class.java
                )
            )
        }


        // --------------------------------------------------------
        // TAKE ATTENDANCE
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // MANUAL ATTENDANCE
        // --------------------------------------------------------

        binding.cardManualAttendance
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Manual Attendance coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // --------------------------------------------------------
        // UPLOAD NOTES
        // --------------------------------------------------------

        binding.cardUploadNotes
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Upload Notes coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // --------------------------------------------------------
        // ASSIGNMENTS
        // --------------------------------------------------------

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


        // --------------------------------------------------------
        // STUDENTS
        // --------------------------------------------------------

        binding.cardStudents
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Students list coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // --------------------------------------------------------
        // RESULTS
        // --------------------------------------------------------

        binding.cardResults
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Results module coming soon.",
                    Toast.LENGTH_SHORT
                ).show()
            }


        // --------------------------------------------------------
        // RETRY
        // --------------------------------------------------------

        binding.btnRetry
            .setOnClickListener {

                viewModel.loadDashboard()
            }


        // --------------------------------------------------------
        // DRAWER
        // --------------------------------------------------------

        binding.navigationView
            .setNavigationItemSelectedListener { item ->

                when (item.itemId) {


                    // --------------------------------------------
                    // DASHBOARD
                    // --------------------------------------------

                    R.id.nav_dashboard -> {

                        binding.drawerLayout
                            .closeDrawers()

                        true
                    }


                    // --------------------------------------------
                    // LOGOUT
                    // --------------------------------------------

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


                        startActivity(
                            intent
                        )


                        true
                    }


                    // --------------------------------------------
                    // OTHER MODULES
                    // --------------------------------------------

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


    // ============================================================
    // SUBJECT RECYCLER
    // ============================================================

    private fun setupSubjectsList() {


        subjectAdapter =
            SubjectCardAdapter { subject ->

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


            setHasFixedSize(
                true
            )
        }
    }


    // ============================================================
    // TODAY CLASSES RECYCLER
    // ============================================================

    private fun setupTodayClasses() {

        todayClassAdapter =
            TeacherTodayClassAdapter { classSchedule ->

                val intent =
                    Intent(
                        this,
                        ActivityTeacherClasses::class.java
                    )

                intent.putExtra(
                    "scheduleId",
                    classSchedule.scheduleId
                )

                intent.putExtra(
                    "className",
                    classSchedule.className
                )

                intent.putExtra(
                    "subjectName",
                    classSchedule.subjectName
                )

                startActivity(
                    intent
                )
            }

        binding.recyclerTodayClasses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherDashboard
                )

            adapter =
                todayClassAdapter

            setHasFixedSize(
                true
            )
        }
    }


    // ============================================================
    // OBSERVE VIEW MODEL
    // ============================================================

    private fun observeViewModel() {


        // --------------------------------------------------------
        // UI STATE
        // --------------------------------------------------------

        viewModel.uiState
            .observe(this) { state ->

                when (state) {


                    // --------------------------------------------
                    // LOADING
                    // --------------------------------------------

                    is TeacherDashboardViewModel.UiState.Loading -> {

                        binding.loadingOverlay.visibility =
                            View.VISIBLE


                        binding.errorState.visibility =
                            View.GONE


                        binding.scrollContent.visibility =
                            View.GONE
                    }


                    // --------------------------------------------
                    // SUCCESS
                    // --------------------------------------------

                    is TeacherDashboardViewModel.UiState.Success -> {

                        binding.loadingOverlay.visibility =
                            View.GONE


                        binding.errorState.visibility =
                            View.GONE


                        binding.scrollContent.visibility =
                            View.VISIBLE
                    }


                    // --------------------------------------------
                    // ERROR
                    // --------------------------------------------

                    is TeacherDashboardViewModel.UiState.Error -> {

                        binding.loadingOverlay.visibility =
                            View.GONE


                        binding.scrollContent.visibility =
                            View.GONE


                        binding.errorState.visibility =
                            View.VISIBLE


                        binding.txtErrorMessage.text =
                            state.message


                        Toast.makeText(
                            this,
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }


        // --------------------------------------------------------
        // TEACHER
        // --------------------------------------------------------

        viewModel.teacher
            .observe(this) { teacher ->

                binding.txtTeacher.text =
                    teacher.fullName
                        .ifBlank {
                            "Teacher"
                        }


                binding.txtRole.text =
                    buildString {

                        if (
                            teacher.designation
                                .isNotBlank()
                        ) {

                            append(
                                teacher.designation
                            )
                        }


                        if (
                            teacher.designation
                                .isNotBlank() &&
                            teacher.departmentName
                                .isNotBlank()
                        ) {

                            append(
                                " • "
                            )
                        }


                        if (
                            teacher.departmentName
                                .isNotBlank()
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
        // TOTAL STUDENTS
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
        // TODAY'S CLASSES
        //
        // Data is now loaded from:
        // classSchedules
        // --------------------------------------------------------

        viewModel.todayClasses
            .observe(this) { classes ->

                updateTodayClasses(
                    classes
                )
            }


        // --------------------------------------------------------
        // UPCOMING CLASSES
        //
        // Data is now loaded from:
        // classSchedules
        // --------------------------------------------------------

        viewModel.upcomingClasses
            .observe(this) { classes ->

                updateUpcomingClasses(
                    classes
                )
            }
    }


    // ============================================================
    // TODAY CLASSES
    // ============================================================

    private fun updateTodayClasses(
        classes: List<ClassSchedule>
    ) {

        todayClassAdapter.submitList(
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


    // ============================================================
    // UPCOMING CLASSES
    // ============================================================

    private fun updateUpcomingClasses(
        classes: List<ClassSchedule>
    ) {

        /*
         * Current XML contains a placeholder card for
         * Upcoming Classes.
         *
         * We intentionally don't reference any IDs that are
         * not present in the current XML.
         *
         * The real ClassSchedule data is now available through:
         *
         * viewModel.upcomingClasses
         *
         * and the XML card can be converted to a RecyclerView
         * in the next step.
         */
    }


    // ============================================================
    // BACK PRESS HANDLING
    //
    // onBackPressed() is deprecated on ComponentActivity /
    // AppCompatActivity. We register an OnBackPressedCallback
    // instead, which is the supported replacement and also works
    // correctly with predictive back gestures on newer Android
    // versions.
    // ============================================================

    private fun setupBackPressHandler() {

        onBackPressedDispatcher.addCallback(this) {

            if (
                binding.drawerLayout
                    .isDrawerOpen(
                        binding.navigationView
                    )
            ) {

                binding.drawerLayout
                    .closeDrawer(
                        binding.navigationView
                    )

            } else {

                // Disable this callback and re-trigger the
                // dispatcher so the default (finish activity)
                // behavior runs.

                isEnabled = false

                onBackPressedDispatcher
                    .onBackPressed()
            }
        }
    }
}