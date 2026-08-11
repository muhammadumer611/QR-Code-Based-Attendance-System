package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityStudentAttendanceDetailBinding

/**
 * Screen 4 (final) of the Attendance flow. Shows the selected student's
 * info, then a subject-wise attendance breakdown -- percentage, progress
 * bar, and expandable present-date history per subject. Absent is
 * calculated (never stored) -- see SubjectAttendanceSummary.
 */
class ActivityStudentAttendanceDetail : AppCompatActivity() {

    private lateinit var binding: ActivityStudentAttendanceDetailBinding
    private lateinit var viewModel: AttendanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentAttendanceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }
        binding.recyclerSubjects.layoutManager = LinearLayoutManager(this)

        val studentId = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_ID) ?: ""
        val studentName = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_NAME) ?: ""
        val regNo = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_REGNO) ?: ""
        val departmentName = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_DEPARTMENT) ?: ""
        val programName = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_PROGRAM) ?: ""
        val session = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_SESSION) ?: ""
        val section = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_SECTION) ?: ""
        val classId = intent.getStringExtra(ActivityAttendanceStudentList.EXTRA_STUDENT_CLASSID) ?: ""

        if (studentId.isBlank()) {
            Toast.makeText(this, "Student not specified.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvStudentName.text = studentName
        binding.tvStudentDetails.text = "$regNo • $departmentName"
        binding.tvStudentClass.text = "$programName • Session $session • Section $section"

        val student = Student(
            studentId = studentId,
            fullName = studentName,
            regNo = regNo,
            departmentName = departmentName,
            programName = programName,
            session = session,
            section = section,
            classId = classId
        )

        observeViewModel()
        viewModel.loadSubjectAttendance(student)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is AttendanceViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is AttendanceViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.subjectSummaries.observe(this) { summaries ->
            binding.tvEmptyState.visibility = if (summaries.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerSubjects.visibility = if (summaries.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerSubjects.adapter = SubjectAttendanceAdapter(summaries)
        }
    }
}