package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityManualAttendanceRegisterBinding

/**
 * Screen: Manual Update register. Shows every student in the selected
 * class as a toggle row (Present/Absent). If attendance already exists
 * for this exact Class + Subject + Date (from a prior manual save or a
 * future QR scan), it's pre-loaded here for editing.
 *
 * Saving writes/deletes attendance_records documents via
 * ManualAttendanceRepository -- Present rows get a document, Absent rows
 * have theirs removed (Absent is never stored).
 */
class ActivityManualAttendanceRegister : AppCompatActivity() {

    private lateinit var binding: ActivityManualAttendanceRegisterBinding
    private lateinit var viewModel: ManualAttendanceViewModel
    private lateinit var adapter: AttendanceRosterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualAttendanceRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ManualAttendanceViewModel::class.java]

        readSelectionsFromIntent()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        binding.tvSubjectDate.text = "${viewModel.selectedCourseCode} • ${viewModel.selectedDate}"
        binding.tvClassInfo.text = "${viewModel.selectedDepartmentName} • ${viewModel.selectedClassTitle}"

        viewModel.loadRoster()
    }

    private fun readSelectionsFromIntent() {
        viewModel.selectedDepartmentId = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_DEPARTMENT_ID) ?: ""
        viewModel.selectedDepartmentName = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_DEPARTMENT_NAME) ?: ""
        viewModel.selectedClassId = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_CLASS_ID) ?: ""
        viewModel.selectedClassTitle = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_CLASS_TITLE) ?: ""
        viewModel.selectedSubjectId = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_SUBJECT_ID) ?: ""
        viewModel.selectedSubjectName = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_SUBJECT_NAME) ?: ""
        viewModel.selectedCourseCode = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_COURSE_CODE) ?: ""
        viewModel.selectedDate = intent.getStringExtra(ActivityManualAttendanceFilter.EXTRA_DATE) ?: ""
    }

    private fun setupRecyclerView() {
        adapter = AttendanceRosterAdapter(emptyList()) { studentId ->
            viewModel.toggleStudent(studentId)
        }
        binding.recyclerRoster.layoutManager = LinearLayoutManager(this)
        binding.recyclerRoster.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBackHeader.setOnClickListener { finish() }
        binding.btnMarkAllPresent.setOnClickListener { viewModel.markAllPresent() }
        binding.btnMarkAllAbsent.setOnClickListener { viewModel.markAllAbsent() }

        binding.btnSave.setOnClickListener {
            // Teacher name for this department -- used as the "marked by"
            // attribution on saved records. Falls back to "Admin" since
            // Manual Update is an Admin action either way.
            viewModel.saveRoster(teacherName = "Admin")
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is ManualAttendanceViewModel.UiState.Loading) View.VISIBLE else View.GONE

            when (state) {
                is ManualAttendanceViewModel.UiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is ManualAttendanceViewModel.UiState.SaveSuccess -> {
                    Toast.makeText(
                        this,
                        "Saved: ${state.presentCount} Present, ${state.totalCount - state.presentCount} Absent",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                else -> Unit
            }
        }

        viewModel.roster.observe(this) { rows ->
            adapter.updateRows(rows)
            binding.tvEmptyState.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerRoster.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE

            val presentCount = rows.count { it.isPresent }
            val absentCount = rows.size - presentCount
            binding.tvSummaryCount.text = "$presentCount Present • $absentCount Absent • ${rows.size} Total"
        }
    }
}