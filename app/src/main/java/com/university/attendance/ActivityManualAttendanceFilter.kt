package com.university.attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityManualAttendanceFilterBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Screen: Admin -> Manual Update -> Filter screen (Department -> Class ->
 * Subject -> Date). Once all 4 are picked, "Open Register" launches
 * ActivityManualAttendanceRegister for that exact combination.
 */
class ActivityManualAttendanceFilter : AppCompatActivity() {

    private lateinit var binding: ActivityManualAttendanceFilterBinding
    private lateinit var viewModel: ManualAttendanceViewModel

    private var departmentList: List<Department> = emptyList()
    private var classList: List<StudentClass> = emptyList()
    private var subjectList: List<Subject> = emptyList()

    private var selectedDepartment: Department? = null
    private var selectedClass: StudentClass? = null
    private var selectedSubject: Subject? = null
    private var selectedDateString: String = ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualAttendanceFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ManualAttendanceViewModel::class.java]

        binding.btnBackHeader.setOnClickListener { finish() }

        setupDropdownListeners()
        observeViewModel()

        viewModel.loadDepartments()
    }

    private fun setupDropdownListeners() {
        binding.etDepartment.setOnItemClickListener { _, _, position, _ ->
            selectedDepartment = departmentList.getOrNull(position)
            selectedClass = null
            selectedSubject = null
            binding.etClass.setText("", false)
            binding.etSubject.setText("", false)

            selectedDepartment?.let { dept ->
                viewModel.loadClasses(dept.name)
            }
        }

        binding.etClass.setOnItemClickListener { _, _, position, _ ->
            selectedClass = classList.getOrNull(position)
            selectedSubject = null
            binding.etSubject.setText("", false)

            val dept = selectedDepartment
            val cls = selectedClass
            if (dept != null && cls != null) {
                viewModel.loadSubjects(dept.departmentId, cls.programName)
            }
        }

        binding.etSubject.setOnItemClickListener { _, _, position, _ ->
            selectedSubject = subjectList.getOrNull(position)
        }

        binding.etDate.setOnClickListener { showDatePicker() }

        binding.btnOpenRegister.setOnClickListener { attemptOpenRegister() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDateString = dateFormat.format(calendar.time)
                binding.etDate.text = selectedDateString
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis() // can't mark attendance for a future date
        }.show()
    }

    private fun attemptOpenRegister() {
        val dept = selectedDepartment
        val cls = selectedClass
        val subject = selectedSubject

        if (dept == null || cls == null || subject == null || selectedDateString.isBlank()) {
            Toast.makeText(
                this,
                "Please select Department, Class, Subject, and Date.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = Intent(this, ActivityManualAttendanceRegister::class.java).apply {
            putExtra(EXTRA_DEPARTMENT_ID, dept.departmentId)
            putExtra(EXTRA_DEPARTMENT_NAME, dept.name)
            putExtra(EXTRA_CLASS_ID, cls.classId)
            putExtra(EXTRA_CLASS_TITLE, "${cls.programName} - Section ${cls.section}")
            putExtra(EXTRA_SUBJECT_ID, subject.subjectId)
            putExtra(EXTRA_SUBJECT_NAME, subject.subjectName)
            putExtra(EXTRA_COURSE_CODE, subject.courseCode)
            putExtra(EXTRA_DATE, selectedDateString)
        }
        startActivity(intent)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility =
                if (state is ManualAttendanceViewModel.UiState.Loading) View.VISIBLE else View.GONE

            if (state is ManualAttendanceViewModel.UiState.Error) {
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.departments.observe(this) { departments ->
            departmentList = departments
            binding.etDepartment.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments.map { it.name })
            )
            binding.etDepartment.setOnClickListener { binding.etDepartment.showDropDown() }
        }

        viewModel.classes.observe(this) { classes ->
            classList = classes
            binding.etClass.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    classes.map { "${it.programName} - Section ${it.section} (${it.session})" }
                )
            )
            binding.etClass.setOnClickListener { binding.etClass.showDropDown() }
        }

        viewModel.subjects.observe(this) { subjects ->
            subjectList = subjects
            binding.etSubject.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    subjects.map { "${it.courseCode} - ${it.subjectName}" }
                )
            )
            binding.etSubject.setOnClickListener { binding.etSubject.showDropDown() }

            if (subjects.isEmpty() && selectedClass != null) {
                Toast.makeText(this, "No subjects found for this class's program.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_DEPARTMENT_ID = "extra_department_id"
        const val EXTRA_DEPARTMENT_NAME = "extra_department_name"
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_CLASS_TITLE = "extra_class_title"
        const val EXTRA_SUBJECT_ID = "extra_subject_id"
        const val EXTRA_SUBJECT_NAME = "extra_subject_name"
        const val EXTRA_COURSE_CODE = "extra_course_code"
        const val EXTRA_DATE = "extra_date"
    }
}