package com.university.attendance

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.university.attendance.databinding.ActivityAdminDashboardBinding

class ActivityAdminDashboard : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardStudentManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddStudent::class.java))
        }

        binding.cardTeacherManagement.setOnClickListener {
            startActivity(Intent(this, ActivityAddTeacher::class.java))
        }

        binding.cardDepartments.setOnClickListener {
            startActivity(Intent(this, ActivityDepartmentManagement::class.java))
        }

        binding.cardSubjects.setOnClickListener {
            startActivity(Intent(this, ActivitySubjectManagement::class.java))
        }

        binding.cardAttendance.setOnClickListener {
            startActivity(Intent(this, ActivityAttendanceDepartmentList::class.java))
        }

        binding.cardManualUpdate.setOnClickListener {
            startActivity(Intent(this, ActivityManualAttendanceFilter::class.java))
        }

        binding.cardDailyOverview.setOnClickListener {
            startActivity(Intent(this, ActivityDailyOverview::class.java))
        }

        binding.cardTeacherAssignment.setOnClickListener {
            startActivity(Intent(this, ActivityTeacherSubjectAssignment::class.java))
        }

        // Schedule card -> opens the schedule file placeholder screen
        binding.CardSchedule.setOnClickListener {
            startActivity(Intent(this, ActivitySchedule::class.java))
        }

        // Reports card -> opens the reports dashboard
        // (counts, today's attendance %, department breakdown)
        binding.CardReports.setOnClickListener {
            startActivity(Intent(this, ActivityReports::class.java))
        }
    }
}