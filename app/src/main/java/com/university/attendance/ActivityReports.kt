package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityReportsBinding

/**
 * Screen: Admin -> Reports.
 * Shows a dashboard-style overview: total students/teachers/departments/
 * subjects, today's overall attendance percentage, and a department-wise
 * breakdown of today's attendance.
 */
class ActivityReports : AppCompatActivity() {

   private lateinit var binding : ActivityReportsBinding
    private lateinit var viewModel: ReportsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ReportsViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerDepartmentReports.layoutManager = LinearLayoutManager(this)

        observeViewModel()
        viewModel.loadReports()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is ReportsViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is ReportsViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.summary.observe(this) { summary ->
            if (summary == null) return@observe

            binding.tvTotalStudents.text = summary.totalStudents.toString()
            binding.tvTotalTeachers.text = summary.totalTeachers.toString()
            binding.tvTotalDepartments.text = summary.totalDepartments.toString()
            binding.tvTotalSubjects.text = summary.totalSubjects.toString()

            binding.tvOverallPercentage.text = "${summary.todayAttendancePercentage}%"
            binding.progressOverall.progress = summary.todayAttendancePercentage
            binding.tvAttendanceSubtitle.text =
                "${summary.todayPresentCount} of ${summary.todayExpectedCount} students present"
        }

        viewModel.departmentBreakdown.observe(this) { rows ->
            binding.tvEmptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerDepartmentReports.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
            binding.recyclerDepartmentReports.adapter = DepartmentReportAdapter(rows)
        }
    }
}