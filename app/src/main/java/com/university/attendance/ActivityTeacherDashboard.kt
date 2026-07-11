package com.university.attendance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.university.attendance.databinding.ActivityTeacherDashboardBinding

class ActivityTeacherDashboard : AppCompatActivity() {
    private lateinit var binding : ActivityTeacherDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}