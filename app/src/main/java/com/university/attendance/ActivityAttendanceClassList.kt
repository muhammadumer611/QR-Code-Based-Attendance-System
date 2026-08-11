package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityAttendanceClassListBinding

/**
 * Screen 2 of the Attendance flow. Shows all classes (Program + Session +
 * Section) belonging to the department passed via Intent extra. Tapping a
 * class opens ActivityAttendanceStudentList for that class.
 */
class ActivityAttendanceClassList : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceClassListBinding
    private lateinit var viewModel: AttendanceViewModel
    private var departmentName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceClassListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        departmentName = intent.getStringExtra(
            ActivityAttendanceDepartmentList.EXTRA_DEPARTMENT_NAME
        ) ?: ""

        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerList.layoutManager = LinearLayoutManager(this)

        observeViewModel()

        if (departmentName.isBlank()) {
            Toast.makeText(this, "Department not specified.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            viewModel.loadClasses(departmentName)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is AttendanceViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is AttendanceViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.classes.observe(this) { classes ->
            binding.tvEmptyState.visibility = if (classes.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerList.visibility = if (classes.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerList.adapter = AttendanceClassAdapter(classes) { studentClass ->
                val intent = Intent(this, ActivityAttendanceStudentList::class.java).apply {
                    putExtra(EXTRA_CLASS_ID, studentClass.classId)
                    putExtra(EXTRA_CLASS_TITLE, "${studentClass.programName} - Section ${studentClass.section}")
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_CLASS_TITLE = "extra_class_title"
    }
}