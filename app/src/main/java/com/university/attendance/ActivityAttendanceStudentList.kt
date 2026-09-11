package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityAttendanceStudentListBinding

/**
 * Screen 3 of the Attendance flow. Shows all students enrolled in the
 * class passed via Intent extra. Tapping a student opens
 * ActivityStudentAttendanceDetail for that student.
 */
class ActivityAttendanceStudentList : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceStudentListBinding
    private lateinit var viewModel: AttendanceViewModel
    private var classId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAttendanceStudentListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        classId = intent.getStringExtra(ActivityAttendanceClassList.EXTRA_CLASS_ID) ?: ""
        val classTitle = intent.getStringExtra(ActivityAttendanceClassList.EXTRA_CLASS_TITLE)

        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerList.layoutManager = LinearLayoutManager(this)

        observeViewModel()

        if (classId.isBlank()) {
            Toast.makeText(this, "Class not specified.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            viewModel.loadStudents(classId)
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

        viewModel.students.observe(this) { students ->
            binding.tvEmptyState.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerList.visibility = if (students.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerList.adapter = AttendanceStudentAdapter(students) { student ->
                val intent = Intent(this, ActivityStudentAttendanceDetail::class.java).apply {
                    putExtra(EXTRA_STUDENT_ID, student.studentId)
                    putExtra(EXTRA_STUDENT_NAME, student.fullName)
                    putExtra(EXTRA_STUDENT_REGNO, student.regNo)
                    putExtra(EXTRA_STUDENT_DEPARTMENT, student.departmentName)
                    putExtra(EXTRA_STUDENT_PROGRAM, student.programName)
                    putExtra(EXTRA_STUDENT_SESSION, student.session)
                    putExtra(EXTRA_STUDENT_SECTION, student.section)
                    putExtra(EXTRA_STUDENT_CLASSID, student.classId)
                }
                startActivity(intent)
            }
        }
    }

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
        const val EXTRA_STUDENT_NAME = "extra_student_name"
        const val EXTRA_STUDENT_REGNO = "extra_student_regno"
        const val EXTRA_STUDENT_DEPARTMENT = "extra_student_department"
        const val EXTRA_STUDENT_PROGRAM = "extra_student_program"
        const val EXTRA_STUDENT_SESSION = "extra_student_session"
        const val EXTRA_STUDENT_SECTION = "extra_student_section"
        const val EXTRA_STUDENT_CLASSID = "extra_student_classid"
    }
}