package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.university.attendance.databinding.ActivityStudentClassAssignmentBinding
import kotlinx.coroutines.launch

class ActivityStudentClassAssignment : AppCompatActivity() {
    private lateinit var binding: ActivityStudentClassAssignmentBinding
    private val repo = StudentClassAssignmentRepository()
    private var students: List<Student> = emptyList()
    private var classes: List<StudentClass> = emptyList()
    private var selectedStudent: Student? = null
    private var selectedClass: StudentClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentClassAssignmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.etStudent.setOnClickListener { binding.etStudent.showDropDown() }
        binding.etClass.setOnClickListener {
            if (binding.etClass.isEnabled) binding.etClass.showDropDown()
        }
        binding.etStudent.setOnItemClickListener { _, _, position, _ ->
            students.getOrNull(position)?.let { loadStudent(it) }
        }
        binding.etClass.setOnItemClickListener { _, _, position, _ ->
            selectedClass = classes.getOrNull(position)
            selectedClass?.let {
                binding.tvClassInfo.text =
                    "New class: ${formatClass(it)}\nStudents in class: ${it.studentCount}"
            }
        }
        binding.btnSave.setOnClickListener { save() }
        loadStudents()
    }

    private fun loadStudents() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                students = repo.getStudents()
                binding.etStudent.setAdapter(
                    ArrayAdapter(
                        this@ActivityStudentClassAssignment,
                        android.R.layout.simple_dropdown_item_1line,
                        students.map {
                            "${it.fullName} • ${it.programName} • Sem ${it.semester} • ${it.session}-${it.section}"
                        }
                    )
                )
                if (students.isEmpty()) {
                    binding.tvClassInfo.text = "No active students found."
                }
            } catch (e: Exception) {
                showError(e.message ?: "Failed to load students.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadStudent(student: Student) {
        selectedStudent = student
        selectedClass = null
        binding.tvStudentInfo.text =
            "Current class: ${if (student.classId.isBlank()) "Not assigned" else "${student.programName} • Section ${student.section}"}\n" +
                "${student.departmentName} • Semester ${student.semester} • Session ${student.session}"
        binding.tvClassInfo.text = "Loading available classes..."
        binding.etClass.setText("", false)
        binding.etClass.isEnabled = false
        setLoading(true)

        lifecycleScope.launch {
            try {
                classes = repo.getAvailableClasses(student)
                binding.etClass.setAdapter(
                    ArrayAdapter(
                        this@ActivityStudentClassAssignment,
                        android.R.layout.simple_dropdown_item_1line,
                        classes.map { "${formatClass(it)} • ${it.studentCount} students" }
                    )
                )
                binding.etClass.isEnabled = classes.isNotEmpty()
                if (classes.isEmpty()) {
                    binding.tvClassInfo.text =
                        "No matching class found for this student's department, program, semester and session."
                } else {
                    binding.etClass.showDropDown()
                }
            } catch (e: Exception) {
                showError(e.message ?: "Failed to load classes.")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun save() {
        val student = selectedStudent ?: run {
            showError("Please select a student.")
            return
        }
        val target = selectedClass ?: run {
            showError("Please select a class.")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            when (val result = repo.assignStudentToClass(student, target)) {
                is StudentClassAssignmentRepository.Result.Success -> {
                    Toast.makeText(
                        this@ActivityStudentClassAssignment,
                        "${student.fullName} assigned to ${result.className}. Re-enroll subjects if this was a class change.",
                        Toast.LENGTH_LONG
                    ).show()
                    loadStudents()
                }
                is StudentClassAssignmentRepository.Result.Error -> showError(result.message)
            }
            setLoading(false)
        }
    }

    private fun formatClass(c: StudentClass): String =
        listOf(c.departmentName, c.programName, "Section ${c.section}")
            .filter { it.isNotBlank() }
            .joinToString(" • ")

    private fun setLoading(value: Boolean) {
        binding.progress.visibility = if (value) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !value
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
