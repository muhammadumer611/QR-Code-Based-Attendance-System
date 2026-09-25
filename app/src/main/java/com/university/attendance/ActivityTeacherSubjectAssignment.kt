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

    private var teachers:
            List<Teacher> = emptyList()

    private var classes:
            List<StudentClass> = emptyList()

    private var selectedTeacher:
            Teacher? = null

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
            finish()
        }

        binding.btnRefreshTeachers.setOnClickListener {
            vm.loadTeachers()
        }

        binding.btnStudentAssignment.setOnClickListener {

            startActivity(
                android.content.Intent(
                    this,
                    ActivityStudentSubjectAssignment::class.java
                )
            )
        }

        binding.btnSaveAssignment.setOnClickListener {
            vm.saveAssignment()
        }

        vm.loadTeachers()
    }

    // ------------------------------------------------------------
    // INPUTS
    // ------------------------------------------------------------

    private fun setupInputs() {

        binding.etTeacher.setOnClickListener {

            binding.etTeacher
                .showDropDown()
        }

        binding.etClass.setOnClickListener {

            if (
                binding.etClass.isEnabled
            ) {

                binding.etClass
                    .showDropDown()
            }
        }

        binding.etTeacher.setOnItemClickListener {

                _,
                _,
                position,
                _ ->

            teachers
                .getOrNull(position)
                ?.let { teacher ->

                    selectedTeacher =
                        teacher

                    binding.tvTeacherInfo.text =
                        buildTeacherInfo(
                            teacher
                        )

                    binding.etClass
                        .setText(
                            "",
                            false
                        )

                    binding.recyclerSubjects
                        .visibility =
                        View.GONE

                    binding.tvSelectedCount.text =
                        "Select a class to see its subjects."

                    subjectAdapter.updateData(
                        emptyList(),
                        emptySet(),
                        teacher.teacherId
                    )

                    vm.selectTeacher(
                        teacher
                    )
                }
        }

        binding.etClass.setOnItemClickListener {

                _,
                _,
                position,
                _ ->

            classes
                .getOrNull(position)
                ?.let { selectedClass ->

                    binding.tvClassInfo.text =
                        buildClassInfo(
                            selectedClass
                        )

                    vm.selectClass(
                        selectedClass
                    )
                }
        }
    }

    // ------------------------------------------------------------
    // OBSERVERS
    // ------------------------------------------------------------

    private fun observe() {

        vm.teachers.observe(
            this
        ) { list ->

            teachers =
                list

            val labels =
                list.map {

                    it.fullName
                        .ifBlank {
                            it.email
                        }
                }

            binding.etTeacher.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout
                        .simple_dropdown_item_1line,
                    labels
                )
            )

            binding.tvTeacherEmpty.visibility =
                if (list.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }

        vm.classes.observe(
            this
        ) { list ->

            classes =
                list

            val labels =
                list.map {

                    "${it.departmentName} • " +
                            "${it.programName} • " +
                            "Semester ${it.semester} • " +
                            "${it.session} • " +
                            "Section ${it.section}"
                }

            binding.etClass.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout
                        .simple_dropdown_item_1line,
                    labels
                )
            )

            binding.etClass.isEnabled =
                list.isNotEmpty()

            if (list.isEmpty()) {

                binding.tvClassInfo.text =
                    "No classes found. Create a class in Class Management first."
            }
        }

        vm.subjects.observe(
            this
        ) { list ->

            binding.recyclerSubjects.visibility =
                if (list.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            if (list.isEmpty()) {

                binding.tvSelectedCount.text =
                    "No subjects found for this class."
            }

            subjectAdapter.updateData(

                newSubjects =
                    list,

                newSelectedIds =
                    vm.selectedSubjectIds
                        .value
                        .orEmpty(),

                newCurrentTeacherId =
                    selectedTeacher
                        ?.teacherId
            )
        }

        vm.selectedSubjectIds.observe(
            this
        ) { selected ->

            binding.tvSelectedCount.text =
                if (selected.isEmpty()) {

                    "Select subjects for this class."

                } else {

                    "${selected.size} subject(s) selected"
                }

            subjectAdapter.updateData(

                newSubjects =
                    vm.subjects
                        .value
                        .orEmpty(),

                newSelectedIds =
                    selected,

                newCurrentTeacherId =
                    selectedTeacher
                        ?.teacherId
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
                        "Teacher, class and subject assignment saved successfully.",
                        Toast.LENGTH_LONG
                    ).show()

                    /*
                     * Reload selected class so the checked
                     * state remains synchronized with Firestore.
                     */
                    vm.selectedClass?.let {
                        vm.selectClass(it)
                    }
                }

                else -> Unit
            }
        }
    }

    // ------------------------------------------------------------
    // UI HELPERS
    // ------------------------------------------------------------

    private fun buildTeacherInfo(
        teacher: Teacher
    ): String {

        return listOf(

            teacher.designation,

            teacher.departmentName,

            teacher.email

        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                " • "
            )
    }

    private fun buildClassInfo(
        c: StudentClass
    ): String {

        return buildString {

            append(
                "${c.departmentName} • ${c.programName}"
            )

            append(
                "\nSemester ${c.semester}"
            )

            append(
                " • Session ${c.session}"
            )

            append(
                " • Section ${c.section}"
            )

            append(
                "\nStudents: ${c.studentCount}"
            )
        }
    }
}