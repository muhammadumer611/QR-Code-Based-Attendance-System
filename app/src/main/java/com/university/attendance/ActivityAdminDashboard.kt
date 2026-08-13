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
        // (Department -> Class -> Student -> Subject-wise attendance)
        binding.cardAttendance.setOnClickListener {
            startActivity(Intent(this, ActivityAttendanceDepartmentList::class.java))
        }

        // Manual Update card -> opens Manual Attendance flow
        // (Department -> Class -> Subject -> Date -> Register -> Save)
//        binding.cardManualUpdate.setOnClickListener {
//            startActivity(Intent(this, ActivityManualAttendanceFilter::class.java))
//        }
//
//        // Daily Overview card -> opens the full day's class overview
//        // across every department/session (Held vs Not Marked)
//        binding.cardDailyOverview.setOnClickListener {
//            startActivity(Intent(this, ActivityDailyOverview::class.java))
//        }

        // Teacher-Subject Assignment card -> opens the assignment screen
        // (pick a teacher, check/uncheck which subjects they teach)
        binding.cardTeacherAssignment.setOnClickListener {
            startActivity(Intent(this, ActivityTeacherSubjectAssignment::class.java))
        }
    }
}