package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityAttendanceDepartmentListBinding

/**
 * Screen 1 of the Attendance flow (Admin -> Attendance card -> here).
 * Shows all departments; tapping one opens ActivityAttendanceClassList
 * for that department.
 */
class ActivityAttendanceDepartmentList : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceDepartmentListBinding
    private lateinit var viewModel: AttendanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceDepartmentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerList.layoutManager = LinearLayoutManager(this)

        observeViewModel()
        viewModel.loadDepartments()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is AttendanceViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is AttendanceViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.departments.observe(this) { departments ->
            binding.tvEmptyState.visibility = if (departments.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerList.visibility = if (departments.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerList.adapter = AttendanceDepartmentAdapter(departments) { department ->
                val intent = Intent(this, ActivityAttendanceClassList::class.java).apply {
                    putExtra(EXTRA_DEPARTMENT_NAME, department.name)
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        const val EXTRA_DEPARTMENT_NAME = "extra_department_name"
    }
}