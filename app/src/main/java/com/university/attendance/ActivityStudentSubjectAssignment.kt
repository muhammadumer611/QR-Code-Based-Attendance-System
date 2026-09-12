package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.university.attendance.databinding.ActivityStudentSubjectAssignmentBinding
import kotlinx.coroutines.launch

class ActivityStudentSubjectAssignment :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentSubjectAssignmentBinding

    private val repo =
        StudentSubjectAssignmentRepository()

    private var students:
            List<Student> = emptyList()

    private var selectedStudent:
            Student? = null

    private var available:
            List<Subject> = emptyList()

    private val checks =
        linkedMapOf<String, CheckBox>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityStudentSubjectAssignmentBinding
                .inflate(layoutInflater)

        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etStudent.setOnClickListener {
            binding.etStudent.showDropDown()
        }

        binding.etStudent.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            students
                .getOrNull(position)
                ?.let {
                    loadStudent(it)
                }
        }

        binding.btnSave.setOnClickListener {
            save()
        }

        loadStudents()
    }

    private fun loadStudents() {

        binding.progress.visibility =
            View.VISIBLE

        lifecycleScope.launch {

            try {

                students =
                    repo.getStudents()

                val labels =
                    students.map {

                        "${it.fullName} • " +
                                "${it.programName} • " +
                                "Sem ${it.semester} • " +
                                "${it.session}-${it.section}"
                    }

                binding.etStudent.setAdapter(
                    ArrayAdapter(
                        this@ActivityStudentSubjectAssignment,
                        android.R.layout
                            .simple_dropdown_item_1line,
                        labels
                    )
                )

                if (students.isEmpty()) {

                    showMessage(
                        "No active students with a class found."
                    )
                }

            } catch (e: Exception) {

                showMessage(
                    e.message
                        ?: "Failed to load students."
                )

            } finally {

                binding.progress.visibility =
                    View.GONE
            }
        }
    }

    private fun loadStudent(
        student: Student
    ) {

        selectedStudent =
            student

        binding.tvStudentInfo.text =
            "${student.departmentName} • " +
                    "${student.programName} • " +
                    "Semester ${student.semester} • " +
                    "${student.session}-${student.section}"

        checks.clear()

        binding.subjectContainer
            .removeAllViews()

        binding.progress.visibility =
            View.VISIBLE

        lifecycleScope.launch {

            try {

                /*
                 * Subjects come directly from
                 * Subject Management.
                 */
                available =
                    repo.getSubjectsForStudent(
                        student
                    )

                val assigned =
                    repo.getAssignedSubjectIds(
                        student.studentId
                    )

                if (available.isEmpty()) {

                    showMessage(
                        "No subjects found in Subject Management for this student's department, program and semester."
                    )

                } else {

                    available.forEach { subject ->

                        val row =
                            LinearLayout(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                orientation =
                                    LinearLayout.VERTICAL

                                setPadding(
                                    8,
                                    10,
                                    8,
                                    10
                                )
                            }

                        val checkBox =
                            CheckBox(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                text =
                                    if (
                                        subject.courseCode
                                            .isBlank()
                                    ) {

                                        subject.subjectName

                                    } else {

                                        "${subject.courseCode} • " +
                                                subject.subjectName
                                    }

                                textSize =
                                    15f

                                isChecked =
                                    assigned.contains(
                                        subject.subjectId
                                    )
                            }

                        val meta =
                            TextView(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                text =
                                    "Semester ${student.semester} • " +
                                            "${subject.creditHours.ifBlank { "3" }} credit hours"

                                textSize =
                                    12f
                            }

                        row.addView(
                            checkBox
                        )

                        row.addView(
                            meta
                        )

                        binding.subjectContainer
                            .addView(row)

                        checks[
                            subject.subjectId
                        ] = checkBox
                    }
                }

            } catch (e: Exception) {

                showMessage(
                    e.message
                        ?: "Failed to load subjects."
                )

            } finally {

                binding.progress.visibility =
                    View.GONE
            }
        }
    }

    private fun showMessage(
        message: String
    ) {

        binding.subjectContainer
            .removeAllViews()

        binding.subjectContainer.addView(
            TextView(this).apply {

                text = message

                setPadding(
                    8,
                    20,
                    8,
                    20
                )
            }
        )
    }

    private fun save() {

        val student =
            selectedStudent
                ?: run {

                    Toast.makeText(
                        this,
                        "Select a student.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }

        val selected =
            checks
                .filterValues {
                    it.isChecked
                }
                .keys

        binding.btnSave.isEnabled =
            false

        binding.progress.visibility =
            View.VISIBLE

        lifecycleScope.launch {

            when (
                val result =
                    repo.saveAssignments(
                        student,
                        available,
                        selected
                    )
            ) {

                is StudentSubjectAssignmentRepository
                .Result.Success -> {

                    Toast.makeText(
                        this@ActivityStudentSubjectAssignment,
                        "${result.count} subject(s) enrolled for ${student.fullName}.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is StudentSubjectAssignmentRepository
                .Result.Error -> {

                    Toast.makeText(
                        this@ActivityStudentSubjectAssignment,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            binding.progress.visibility =
                View.GONE

            binding.btnSave.isEnabled =
                true
        }
    }
}