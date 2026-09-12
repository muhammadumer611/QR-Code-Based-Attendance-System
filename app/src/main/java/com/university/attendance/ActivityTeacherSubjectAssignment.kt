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

    private lateinit var vm:
            TeacherSubjectViewModel

    private lateinit var subjectAdapter:
            SubjectChecklistAdapter

    private var selectedTeacher:
            Teacher? = null

    private val semesters =
        (1..8).map {
            "Semester $it"
        }

    private var lastClassLoadKey =
        ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityTeacherSubjectAssignmentBinding
                .inflate(layoutInflater)

        setContentView(binding.root)

        vm =
            ViewModelProvider(this)[
                TeacherSubjectViewModel::class.java
            ]

        subjectAdapter =
            SubjectChecklistAdapter(
                emptyList(),
                emptySet(),
                ""
            ) {
                vm.toggleSubject(it)
            }

        binding.recyclerSubjects.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerSubjects.adapter =
            subjectAdapter

        setupInputs()
        observe()

        binding.btnBackHeader.setOnClickListener {

            if (
                binding.stepTwoContainer.visibility ==
                View.VISIBLE
            ) {
                showTeachers()
            } else {
                finish()
            }
        }

        binding.btnSaveAssignment.setOnClickListener {
            vm.saveAssignment()
        }

        binding.btnStudentAssignment.setOnClickListener {

            startActivity(
                android.content.Intent(
                    this,
                    ActivityStudentSubjectAssignment::class.java
                )
            )
        }

        binding.btnRefreshTeachers.setOnClickListener {
            vm.loadTeachers()
        }

        vm.loadTeachers()
    }

    private fun setupInputs() {

        binding.etAssignmentSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout
                    .simple_dropdown_item_1line,
                semesters
            )
        )

        binding.etAssignmentSemester.setOnClickListener {
            binding.etAssignmentSemester
                .showDropDown()
        }

        binding.etAssignmentSemester
            .setOnItemClickListener {
                    _,
                    _,
                    _,
                    _ ->

                reloadClasses(
                    force = true
                )
            }

        binding.etAssignmentSession
            .addTextChangedListener(
                SimpleTextWatcher {
                    reloadClasses()
                }
            )

        binding.etAssignmentClass
            .setOnClickListener {

                if (
                    binding.etAssignmentClass
                        .isEnabled
                ) {
                    binding.etAssignmentClass
                        .showDropDown()
                }
            }

        binding.etAssignmentClass
            .setOnItemClickListener {
                    _,
                    _,
                    position,
                    _ ->

                vm.classes.value
                    .orEmpty()
                    .getOrNull(position)
                    ?.let(vm::selectClass)
            }
    }

    private fun reloadClasses(
        force: Boolean = false
    ) {

        val semester =
            Regex("\\d+")
                .find(
                    binding.etAssignmentSemester
                        .text
                        .toString()
                )
                ?.value
                ?.toIntOrNull()
                ?: return

        val session =
            binding.etAssignmentSession
                .text
                .toString()
                .trim()

        if (
            selectedTeacher == null ||
            session.isBlank()
        ) {
            return
        }

        val key =
            "${selectedTeacher!!.teacherId}|" +
                    "$semester|" +
                    session

        if (
            !force &&
            key == lastClassLoadKey
        ) {
            return
        }

        lastClassLoadKey =
            key

        vm.loadClasses(
            semester,
            session
        )
    }

    private fun openTeacher(
        teacher: Teacher
    ) {

        selectedTeacher =
            teacher

        vm.selectTeacher(
            teacher
        )

        lastClassLoadKey =
            ""

        binding.stepOneContainer
            .visibility = View.GONE

        binding.stepTwoContainer
            .visibility = View.VISIBLE

        binding.tvHeaderTitle.text =
            teacher.fullName
                .ifBlank {
                    teacher.email
                        .ifBlank {
                            "Teacher"
                        }
                }

        binding.tvHeaderSubtitle.text =
            "Teacher → exact semester → class → subjects"

        binding.etAssignmentSemester
            .setText("", false)

        binding.etAssignmentSession
            .setText("")

        binding.etAssignmentClass
            .setText("", false)

        binding.etAssignmentClass
            .isEnabled = false

        binding.recyclerSubjects
            .visibility = View.GONE

        binding.tvSelectedCount.text =
            "Select Semester + Session + Class"

        subjectAdapter.updateData(
            emptyList(),
            emptySet(),
            teacher.teacherId
        )
    }

    private fun showTeachers() {

        selectedTeacher =
            null

        lastClassLoadKey =
            ""

        binding.stepTwoContainer
            .visibility = View.GONE

        binding.stepOneContainer
            .visibility = View.VISIBLE

        binding.tvHeaderTitle.text =
            "Teacher Assignment"

        binding.tvHeaderSubtitle.text =
            "Assign exact teacher + class + semester + subject"

        vm.selectTeacher(
            Teacher()
        )

        vm.selectedTeacher =
            null

        vm.loadTeachers()
    }

    private fun observe() {

        vm.teachers.observe(
            this
        ) { list ->

            binding.recyclerTeachers.visibility =
                if (list.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            binding.tvEmptyState.visibility =
                if (list.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.recyclerTeachers.adapter =
                TeacherAssignmentAdapter(
                    list
                ) {
                    openTeacher(it)
                }
        }

        vm.classes.observe(
            this
        ) { list ->

            val labels =
                list.map {

                    "${it.departmentName} • " +
                            "${it.programName} • " +
                            "Section ${it.section} • " +
                            "${it.studentCount} students"
                }

            binding.etAssignmentClass
                .setAdapter(
                    ArrayAdapter(
                        this,
                        android.R.layout
                            .simple_dropdown_item_1line,
                        labels
                    )
                )

            binding.etAssignmentClass
                .isEnabled =
                list.isNotEmpty()

            if (list.isNotEmpty()) {

                binding.etAssignmentClass
                    .showDropDown()
            }
        }

        vm.subjects.observe(
            this
        ) { list ->

            binding.recyclerSubjects
                .visibility =
                if (list.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            subjectAdapter.updateData(
                list,
                vm.selectedSubjectIds
                    .value
                    .orEmpty(),
                selectedTeacher?.teacherId
            )
        }

        vm.selectedSubjectIds.observe(
            this
        ) { selected ->

            binding.tvSelectedCount.text =
                if (selected.isEmpty()) {
                    "No subject selected"
                } else {
                    "${selected.size} subject(s) selected"
                }

            subjectAdapter.updateData(
                vm.subjects.value.orEmpty(),
                selected,
                selectedTeacher?.teacherId
            )
        }

        vm.uiState.observe(
            this
        ) { state ->

            binding.progressBar.visibility =
                if (
                    state is
                            TeacherSubjectViewModel
                            .UiState.Loading
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            when (state) {

                is TeacherSubjectViewModel
                .UiState.Error -> {

                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is TeacherSubjectViewModel
                .UiState.SaveSuccess -> {

                    Toast.makeText(
                        this,
                        "Teacher • Class • Semester • Subject assignment saved.",
                        Toast.LENGTH_LONG
                    ).show()

                    showTeachers()
                }

                else -> Unit
            }
        }
    }
}

private class SimpleTextWatcher(
    private val onChange: () -> Unit
) : android.text.TextWatcher {

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) = Unit

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        onChange()
    }

    override fun afterTextChanged(
        s: android.text.Editable?
    ) = Unit
}