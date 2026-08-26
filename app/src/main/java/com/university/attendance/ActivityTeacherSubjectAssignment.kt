package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityTeacherSubjectAssignmentBinding

/**
 * Screen: Admin -> Teacher-Subject Assignment.
 *
 * Two-step flow inside ONE Activity (toggled containers, same pattern as
 * Add Student/Add Teacher):
 *   Step 1: pick a teacher from the list.
 *   Step 2: check/uncheck which subjects that teacher is assigned to,
 *           then Save.
 *
 * Fixes the earlier placeholder behavior where "Taught by" on Attendance
 * Summary / Daily Overview screens guessed the first teacher in a
 * department -- once assignments are saved here, those screens show the
 * real assigned teacher per subject.
 */
class ActivityTeacherSubjectAssignment : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherSubjectAssignmentBinding
    private lateinit var viewModel: TeacherSubjectViewModel
    private lateinit var subjectAdapter: SubjectChecklistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeacherSubjectAssignmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TeacherSubjectViewModel::class.java]

        binding.btnBackHeader.setOnClickListener {
            if (binding.stepTwoContainer.visibility == View.VISIBLE) {
                goToStep1()
            } else {
                finish()
            }
        }

        binding.recyclerTeachers.layoutManager = LinearLayoutManager(this)

        setupSubjectChecklist()
        observeViewModel()

        viewModel.loadTeachers()
    }

    private fun setupSubjectChecklist() {
        subjectAdapter = SubjectChecklistAdapter(
            subjects = emptyList(),
            selectedIds = emptySet(),
            currentTeacherId = "",
            onToggle = { subjectId -> viewModel.toggleSubject(subjectId) }
        )
        binding.recyclerSubjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerSubjects.adapter = subjectAdapter

        binding.btnSaveAssignment.setOnClickListener { viewModel.saveAssignment() }
    }

    private fun goToStep1() {
        binding.stepOneContainer.visibility = View.VISIBLE
        binding.stepTwoContainer.visibility = View.GONE
        binding.tvHeaderTitle.text = "Assign Subjects"
        binding.tvHeaderSubtitle.text = "Select a teacher"
    }

    private fun goToStep2(teacher: Teacher) {
        binding.stepOneContainer.visibility = View.GONE
        binding.stepTwoContainer.visibility = View.VISIBLE
        binding.tvHeaderTitle.text = teacher.fullName
        binding.tvHeaderSubtitle.text = "${teacher.designation} • ${teacher.departmentName}"

        viewModel.loadSubjectsForTeacher(teacher.teacherId, teacher.fullName)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is TeacherSubjectViewModel.UiState.Loading) View.VISIBLE else View.GONE

            when (state) {
                is TeacherSubjectViewModel.UiState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                is TeacherSubjectViewModel.UiState.SaveSuccess -> {
                    Toast.makeText(this, "Assignment saved.", Toast.LENGTH_SHORT).show()
                    goToStep1()
                }
                else -> Unit
            }
        }

        viewModel.teachers.observe(this) { teachers ->
            binding.tvEmptyState.visibility = if (teachers.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerTeachers.visibility = if (teachers.isEmpty()) View.GONE else View.VISIBLE

            binding.recyclerTeachers.adapter = TeacherAssignmentAdapter(teachers) { teacher ->
                goToStep2(teacher)
            }
        }

        viewModel.subjects.observe(this) { subjects ->
            subjectAdapter.updateData(subjects, viewModel.selectedSubjectIds.value.orEmpty(), viewModel.selectedTeacherId)
        }

        viewModel.selectedSubjectIds.observe(this) { selectedIds ->
            subjectAdapter.updateData(viewModel.subjects.value.orEmpty(), selectedIds, viewModel.selectedTeacherId)
            binding.tvSelectedCount.text = "${selectedIds.size} subject(s) selected"
        }
    }
}