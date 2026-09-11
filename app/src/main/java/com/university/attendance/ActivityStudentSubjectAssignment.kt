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
            List<TeacherSubjectAssignment> =
        emptyList()

    private val checks =
        linkedMapOf<
                String,
                CheckBox
                >()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityStudentSubjectAssignmentBinding
                .inflate(layoutInflater)

        setContentView(
            binding.root
        )

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etStudent.setOnClickListener {
            binding.etStudent.showDropDown()
        }

        binding.etStudent.setOnItemClickListener {
                _, _, position, _ ->

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

                        "${it.fullName} • ${it.programName} • ${it.session}-${it.section}"
                    }

                binding.etStudent.setAdapter(
                    ArrayAdapter(
                        this@ActivityStudentSubjectAssignment,
                        android.R.layout.simple_dropdown_item_1line,
                        labels
                    )
                )

                if (
                    students.isEmpty()
                ) {

                    Toast.makeText(
                        this@ActivityStudentSubjectAssignment,
                        "No active students found.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityStudentSubjectAssignment,
                    e.message
                        ?: "Failed to load students.",
                    Toast.LENGTH_LONG
                ).show()

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
            "${student.programName} • Semester ${student.semester} • ${student.session}-${student.section}"

        checks.clear()

        binding.subjectContainer
            .removeAllViews()

        binding.progress.visibility =
            View.VISIBLE

        lifecycleScope.launch {

            try {

                available =
                    repo.getTeacherSubjectsForStudent(
                        student
                    )

                val assigned =
                    repo.getAssignedSubjectIds(
                        student.studentId
                    )

                if (
                    available.isEmpty()
                ) {

                    addMessage(
                        "No teacher-assigned subjects found for this student's exact class and semester."
                    )

                } else {

                    available.forEach { item ->

                        val row =
                            LinearLayout(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                orientation =
                                    LinearLayout.VERTICAL

                                setPadding(
                                    8,
                                    12,
                                    8,
                                    12
                                )
                            }

                        val checkbox =
                            CheckBox(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                text =
                                    if (
                                        item.courseCode.isBlank()
                                    ) {
                                        item.subjectName
                                    } else {
                                        "${item.courseCode} • ${item.subjectName}"
                                    }

                                textSize =
                                    15f

                                isChecked =
                                    assigned.contains(
                                        item.subjectId
                                    )
                            }

                        val teacher =
                            TextView(
                                this@ActivityStudentSubjectAssignment
                            ).apply {

                                text =
                                    "Teacher: ${item.teacherName.ifBlank { "Not specified" }}"

                                textSize =
                                    12f
                            }

                        row.addView(
                            checkbox
                        )

                        row.addView(
                            teacher
                        )

                        binding.subjectContainer
                            .addView(row)

                        checks[
                            StudentSubjectAssignmentRepository
                                .key(item)
                        ] =
                            checkbox
                    }
                }

            } catch (e: Exception) {

                addMessage(
                    e.message
                        ?: "Failed to load subjects."
                )

            } finally {

                binding.progress.visibility =
                    View.GONE
            }
        }
    }

    private fun addMessage(
        message: String
    ) {

        binding.subjectContainer
            .removeAllViews()

        binding.subjectContainer
            .addView(
                TextView(this).apply {

                    text =
                        message

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

        if (
            selected.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Select at least one subject.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

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
                        "${result.count} student subject assignment(s) saved.",
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