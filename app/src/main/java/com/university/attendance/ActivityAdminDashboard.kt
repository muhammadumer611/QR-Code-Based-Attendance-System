package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityAdminDashboardBinding

class ActivityAdminDashboard : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var feedViewModel: DashboardFeedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        feedViewModel = ViewModelProvider(this)[DashboardFeedViewModel::class.java]

        // Student Management card -> opens Add Student screen
        binding.cardStudentManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddStudent::class.java))
        }

        // Teacher Management card -> opens Add Teacher screen
        binding.cardTeacherManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddTeacher::class.java))
        }

        // Departments card -> opens Department Management (CRUD) screen
        binding.cardDepartments.setOnClickListener {
            startActivity(Intent(this, ActivityDepartmentManagement::class.java))
        }

        // Subjects card -> opens Subject Management (CRUD) screen
        binding.cardSubjects.setOnClickListener {
            startActivity(Intent(this, ActivitySubjectManagement::class.java))
        }

        // Attendance card -> opens Attendance drill-down flow
        binding.cardAttendance.setOnClickListener {
            startActivity(Intent(this, ActivityAttendanceDepartmentList::class.java))
        }

        // Manual Update card -> opens Manual Attendance flow
        binding.cardManualUpdate.setOnClickListener {
            startActivity(Intent(this, ActivityManualAttendanceFilter::class.java))
        }

        // Daily Overview card -> opens the full day's class overview
        binding.cardDailyOverview.setOnClickListener {
            startActivity(Intent(this, ActivityDailyOverview::class.java))
        }

        // Teacher-Subject Assignment card -> opens the assignment screen
        binding.cardTeacherAssignment.setOnClickListener {
            startActivity(Intent(this, ActivityTeacherSubjectAssignment::class.java))
        }

        // Schedule card -> opens the schedule file placeholder screen
        binding.cardSchedule.setOnClickListener {
            startActivity(Intent(this, ActivitySchedule::class.java))
        }

        // Reports card -> opens the reports dashboard
        binding.cardReports.setOnClickListener {
            startActivity(Intent(this, ActivityReports::class.java))
        }

        // Notification bell -> opens full Notifications screen
        binding.imgNotification.setOnClickListener {
            startActivity(Intent(this, ActivityNotifications::class.java))
        }
        // Profile
        binding.imgProfile.setOnClickListener {
            startActivity(Intent(this@ActivityAdminDashboard, ActivityAdminProfile::class.java))
        }

        setupRecentActivities()
        observeFeed()
    }

    override fun onResume() {
        super.onResume()
        // Refresh every time the dashboard becomes visible again (e.g.
        // after returning from Add Student, Manual Update, etc.) so the
        // Recent Activities list and notification badge reflect anything
        // that just happened.
        feedViewModel.loadRecentActivities()
        feedViewModel.loadNotifications()
    }

    private fun setupRecentActivities() {
        binding.recyclerRecentActivity.layoutManager = LinearLayoutManager(this)
    }

    private fun observeFeed() {
        feedViewModel.recentActivities.observe(this) { activities ->
            binding.recyclerRecentActivity.adapter = RecentActivityAdapter(activities)
        }

        feedViewModel.unreadCount.observe(this) { count ->
            if (count > 0) {
                binding.badge.visibility = View.VISIBLE
                binding.badge.text = if (count > 9) "9+" else count.toString()
            } else {
                binding.badge.visibility = View.GONE
            }
        }
    }
}