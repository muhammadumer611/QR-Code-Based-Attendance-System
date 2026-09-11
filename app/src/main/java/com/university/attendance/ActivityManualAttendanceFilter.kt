package com.university.attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityManualAttendanceFilterBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

        binding.etDepartment.threshold = 1
        binding.etClass.threshold = 1
        binding.etSubject.threshold = 1

        binding.btnBackHeader.setOnClickListener { finish() }

        binding.etDepartment.setOnItemClickListener { _, _, position, _ ->
            Log.d("ManualFilter", "Department item clicked, position=$position, listSize=${departmentList.size}")
            selectedDepartment = departmentList.getOrNull(position)
            Log.d("ManualFilter", "selectedDepartment=${selectedDepartment?.name}")

            selectedClass = null
            selectedSubject = null
            binding.etClass.setText("", false)
            binding.etSubject.setText("", false)
            binding.etClass.setAdapter(null)
            binding.etSubject.setAdapter(null)

            selectedDepartment?.let { dept ->
                Log.d("ManualFilter", "Calling loadClasses(${dept.name})")
                viewModel.loadClasses(dept.name)
            }
        }

        binding.etClass.setOnItemClickListener { _, _, position, _ ->
            Log.d("ManualFilter", "Class item clicked, position=$position, listSize=${classList.size}")
            selectedClass = classList.getOrNull(position)
            Log.d("ManualFilter", "selectedClass=${selectedClass?.classId}")

            selectedSubject = null
            binding.etSubject.setText("", false)
            binding.etSubject.setAdapter(null)

            val dept = selectedDepartment
            val cls = selectedClass
            if (dept != null && cls != null) {
                Log.d("ManualFilter", "Calling loadSubjects(${dept.departmentId}, ${cls.programName})")
                viewModel.loadSubjects(dept.departmentId, cls.programName)
            }
        }

        binding.etSubject.setOnItemClickListener { _, _, position, _ ->
            Log.d("ManualFilter", "Subject item clicked, position=$position, listSize=${subjectList.size}")
            selectedSubject = subjectList.getOrNull(position)
            Log.d("ManualFilter", "selectedSubject=${selectedSubject?.subjectName}")
        }

        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnOpenRegister.setOnClickListener { attemptOpenRegister() }

        observeViewModel()
        viewModel.loadDepartments()
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
            datePicker.maxDate = System.currentTimeMillis()
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
                Log.d("ManualFilter", "uiState Error: ${state.message}")
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.departments.observe(this) { departments ->
            Log.d("ManualFilter", "departments.observe fired, count=${departments.size}")
            departmentList = departments
            binding.etDepartment.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments.map { it.name })
            )
            binding.etDepartment.setOnClickListener {
                Log.d("ManualFilter", "Department field clicked, showing dropdown")
                binding.etDepartment.showDropDown()
            }
        }

        viewModel.classes.observe(this) { classes ->
            Log.d("ManualFilter", "classes.observe fired, count=${classes.size}")
            classList = classes
            binding.etClass.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    classes.map { "${it.programName} - Section ${it.section} (${it.session})" }
                )
            )
            binding.etClass.setOnClickListener {
                Log.d("ManualFilter", "Class field clicked, adapter count=${classList.size}, showing dropdown")
                binding.etClass.showDropDown()
            }

            if (classes.isEmpty() && selectedDepartment != null) {
                Toast.makeText(this, "No classes found for this department.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.subjects.observe(this) { subjects ->
            Log.d("ManualFilter", "subjects.observe fired, count=${subjects.size}")
            subjectList = subjects
            binding.etSubject.setAdapter(
                ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    subjects.map { "${it.courseCode} - ${it.subjectName}" }
                )
            )
            binding.etSubject.setOnClickListener {
                Log.d("ManualFilter", "Subject field clicked, adapter count=${subjectList.size}, showing dropdown")
                binding.etSubject.showDropDown()
            }

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