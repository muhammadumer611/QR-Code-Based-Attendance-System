package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityTeacherSubjectAssignmentBinding

class ActivityTeacherSubjectAssignment :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityTeacherSubjectAssignmentBinding

    private lateinit var viewModel:
            TeacherSubjectViewModel

    private lateinit var subjectAdapter:
            SubjectChecklistAdapter

    private var selectedTeacher:
            Teacher? = null

    private val semesters =
        (1..8).map {
            "Semester $it"
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityTeacherSubjectAssignmentBinding
                .inflate(
                    layoutInflater
                )

        setContentView(
            binding.root
        )

        viewModel =
            ViewModelProvider(this)[
                TeacherSubjectViewModel::class.java
            ]

        setupSemester()
        setupSubjects()
        setupObservers()

        binding.btnBackHeader.setOnClickListener {

            if (
                binding.stepTwoContainer.visibility ==
                View.VISIBLE
            ) {

                binding.stepOneContainer.visibility =
                    View.VISIBLE

                binding.stepTwoContainer.visibility =
                    View.GONE

            } else {

                finish()
            }
        }

        viewModel.loadTeachers()
    }

    // ============================================================
    // SEMESTER
    // ============================================================

    private fun setupSemester() {

        binding.etAssignmentSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                semesters
            )
        )

        binding.etAssignmentSemester.setOnClickListener {
            binding.etAssignmentSemester.showDropDown()
        }

        binding.etAssignmentSemester.setOnItemClickListener { _, _, _, _ ->
            loadSubjects()
        }

        binding.etAssignmentSession.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                loadSubjects()
            }
        }
    }

    // ============================================================
    // SUBJECTS
    // ============================================================

    private fun setupSubjects() {

        subjectAdapter =
            SubjectChecklistAdapter(
                subjects = emptyList(),
                selectedIds = emptySet(),
                currentTeacherId = "",
                onToggle = {
                    viewModel.toggleSubject(
                        it
                    )
                }
            )

        binding.recyclerSubjects.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerSubjects.adapter =
            subjectAdapter

        binding.btnSaveAssignment.setOnClickListener {

            val teacher =
                selectedTeacher

            if (teacher == null) {

                Toast.makeText(
                    this,
                    "Teacher not selected.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.saveAssignment(
                teacher
            )
        }
    }

    // ============================================================
    // SELECT TEACHER
    // ============================================================

    private fun openTeacher(
        teacher: Teacher
    ) {

        selectedTeacher =
            teacher

        viewModel.selectTeacher(
            teacher
        )

        binding.stepOneContainer.visibility =
            View.GONE

        binding.stepTwoContainer.visibility =
            View.VISIBLE

        binding.tvHeaderTitle.text =
            teacher.fullName

        binding.tvHeaderSubtitle.text =
            "${teacher.designation} • ${teacher.departmentName}"

        binding.etAssignmentSemester.setText(
            ""
        )

        binding.etAssignmentSession.setText(
            ""
        )

        binding.recyclerSubjects.visibility =
            View.GONE

        binding.tvSelectedCount.text =
            "Select Semester + Session"
    }

    // ============================================================
    // LOAD SUBJECTS
    // ============================================================

    private fun loadSubjects() {

        val semesterText =
            binding.etAssignmentSemester
                .text
                .toString()

        val semester =
            Regex("\\d+")
                .find(
                    semesterText
                )
                ?.value
                ?.toIntOrNull()

        val session =
            binding.etAssignmentSession
                .text
                .toString()
                .trim()

        if (semester == null) {

            Toast.makeText(
                this,
                "Please select semester.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (session.isBlank()) {

            Toast.makeText(
                this,
                "Please enter/select session.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        viewModel.loadSubjects(
            semester,
            session
        )
    }

    // ============================================================
    // OBSERVERS
    // ============================================================

    private fun setupObservers() {

        viewModel.teachers.observe(
            this
        ) { teachers ->

            binding.tvEmptyState.visibility =
                if (teachers.isEmpty())
                    View.VISIBLE
                else
                    View.GONE

            binding.recyclerTeachers.visibility =
                if (teachers.isEmpty())
                    View.GONE
                else
                    View.VISIBLE

            binding.recyclerTeachers.adapter =
                TeacherAssignmentAdapter(
                    teachers
                ) { teacher ->

                    openTeacher(
                        teacher
                    )
                }
        }

        viewModel.subjects.observe(
            this
        ) { subjects ->

            binding.recyclerSubjects.visibility =
                if (subjects.isEmpty())
                    View.GONE
                else
                    View.VISIBLE

            subjectAdapter.updateData(

                subjects,

                viewModel
                    .selectedSubjectIds
                    .value
                    .orEmpty(),

                viewModel.selectedTeacherId
            )
        }

        viewModel.selectedSubjectIds.observe(
            this
        ) { selected ->

            binding.tvSelectedCount.text =
                "${selected.size} subject(s) selected"

            subjectAdapter.updateData(

                viewModel
                    .subjects
                    .value
                    .orEmpty(),

                selected,

                viewModel.selectedTeacherId
            )
        }

        viewModel.uiState.observe(
            this
        ) { state ->

            binding.progressBar.visibility =
                if (
                    state is TeacherSubjectViewModel.UiState.Loading
                )
                    View.VISIBLE
                else
                    View.GONE

            when (state) {

                is TeacherSubjectViewModel.UiState.Error -> {

                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is TeacherSubjectViewModel.UiState.SaveSuccess -> {

                    Toast.makeText(
                        this,
                        "Subjects assigned successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.stepOneContainer.visibility =
                        View.VISIBLE

                    binding.stepTwoContainer.visibility =
                        View.GONE
                }

                else -> Unit
            }
        }
    }
}