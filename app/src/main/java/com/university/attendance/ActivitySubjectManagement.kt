package com.university.attendance

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivitySubjectManagementBinding
import com.university.attendance.databinding.DialogsubjectinputBinding

/**
 * Screen: Admin -> Subjects -> Subject Management
 *
 * Flow (matching a real university course catalogue, e.g. UOL):
 *   Department -> Program -> Semester -> Subjects (Course Code, Name, Credit Hours)
 *
 * The filter bar at the top MUST have all 3 selected before any subjects
 * are shown or can be added -- this guarantees subjects from different
 * departments/programs/semesters never mix together.
 *
 * Full CRUD: Create (FAB), Read (real-time list scoped to filter), Update
 * (edit icon), Delete (delete icon with confirmation).
 */
class ActivitySubjectManagement : AppCompatActivity() {

    private lateinit var binding: ActivitySubjectManagementBinding
    private lateinit var viewModel: SubjectViewModel
    private lateinit var adapter: SubjectAdapter

    private var departmentList: List<Department> = emptyList()

    // Standard program + semester options.
    private val programOptions = listOf("BSSE", "BSCS", "BSIT", "BSAI", "BBA", "BSEE")
    private val semesterOptions = (1..8).map { "Semester $it" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubjectManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SubjectViewModel::class.java]

        setupRecyclerView()
        setupFilterDropdowns()
        setupClickListeners()
        observeDepartments()
        observeSubjects()
        observeActionState()

        viewModel.loadDepartments()
    }

    private fun setupRecyclerView() {
        adapter = SubjectAdapter(
            onEditClick = { subject -> showEditDialog(subject) },
            onDeleteClick = { subject -> showDeleteConfirmation(subject) }
        )
        binding.recyclerSubjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerSubjects.adapter = adapter
    }

    private fun setupFilterDropdowns() {
        binding.etFilterProgram.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, programOptions)
        )
        binding.etFilterProgram.setOnClickListener { binding.etFilterProgram.showDropDown() }

        binding.etFilterSemester.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, semesterOptions)
        )
        binding.etFilterSemester.setOnClickListener { binding.etFilterSemester.showDropDown() }

        // Whenever any filter field changes, re-apply the combined filter.
        binding.etFilterDepartment.setOnItemClickListener { _, _, position, _ ->
            val selectedDept = departmentList.getOrNull(position)
            if (selectedDept != null) {
                applyCurrentFilter(departmentId = selectedDept.departmentId, departmentName = selectedDept.name)
            }
        }
        binding.etFilterProgram.setOnItemClickListener { _, _, _, _ -> applyCurrentFilter() }
        binding.etFilterSemester.setOnItemClickListener { _, _, _, _ -> applyCurrentFilter() }
    }

    /**
     * Re-reads all 3 filter fields and asks the ViewModel to switch the
     * real-time listener to that exact combination. Optionally accepts a
     * freshly-picked department id/name to avoid a stale-read race right
     * after the department dropdown selection.
     */
    private fun applyCurrentFilter(departmentId: String? = null, departmentName: String? = null) {
        val deptId = departmentId ?: run {
            val typedName = binding.etFilterDepartment.text.toString()
            departmentList.firstOrNull { it.name == typedName }?.departmentId ?: ""
        }
        val deptName = departmentName ?: binding.etFilterDepartment.text.toString()

        val program = binding.etFilterProgram.text.toString()

        val semesterText = binding.etFilterSemester.text.toString()
        val semester = semesterText.filter { it.isDigit() }.toIntOrNull() ?: 0

        viewModel.applyFilter(deptId, deptName, program, semester)

        val filterComplete = deptId.isNotBlank() && program.isNotBlank() && semester != 0
        binding.tvEmptyState.text = if (!filterComplete) {
            "Select Department, Program, and Semester above to view subjects."
        } else {
            "No subjects added yet for this selection.\nTap + to add one."
        }
    }

    private fun setupClickListeners() {
        binding.btnBackHeader.setOnClickListener { finish() }

        binding.fabAddSubject.setOnClickListener {
            if (viewModel.selectedDepartmentId.isBlank() ||
                viewModel.selectedProgram.isBlank() ||
                viewModel.selectedSemester == 0
            ) {
                Toast.makeText(
                    this,
                    "Please select Department, Program, and Semester first.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showAddDialog()
            }
        }
    }

    private fun observeDepartments() {
        viewModel.departments.observe(this) { departments ->
            departmentList = departments
            binding.etFilterDepartment.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments.map { it.name })
            )
            binding.etFilterDepartment.setOnClickListener { binding.etFilterDepartment.showDropDown() }

            if (departments.isEmpty()) {
                Toast.makeText(
                    this,
                    "No departments found. Please add a department first.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun observeSubjects() {
        viewModel.subjects.observe(this) { subjects ->
            adapter.submitList(subjects)
            binding.tvEmptyState.visibility = if (subjects.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerSubjects.visibility = if (subjects.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeActionState() {
        viewModel.actionState.observe(this) { state ->
            when (state) {
                is SubjectViewModel.ActionState.Loading -> binding.progressBar.visibility = View.VISIBLE
                is SubjectViewModel.ActionState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.consumeActionState()
                }
                is SubjectViewModel.ActionState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.consumeActionState()
                }
                is SubjectViewModel.ActionState.Idle -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    // ---------------------------- CREATE ----------------------------

    private fun showAddDialog() {
        val dialogBinding = DialogsubjectinputBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Add Subject — ${viewModel.selectedProgram}, Semester ${viewModel.selectedSemester}")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val courseCode = dialogBinding.etCourseCode.text.toString()
                val subjectName = dialogBinding.etSubjectName.text.toString()
                val creditHours = dialogBinding.etCreditHours.text.toString().toIntOrNull() ?: 3
                viewModel.addSubject(courseCode, subjectName, creditHours)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------------------- UPDATE ----------------------------

    private fun showEditDialog(subject: Subject) {
        val dialogBinding = DialogsubjectinputBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etCourseCode.setText(subject.courseCode)
        dialogBinding.etSubjectName.setText(subject.subjectName)
        dialogBinding.etCreditHours.setText(subject.creditHours.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Subject")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val courseCode = dialogBinding.etCourseCode.text.toString()
                val subjectName = dialogBinding.etSubjectName.text.toString()
                val creditHours = dialogBinding.etCreditHours.text.toString().toIntOrNull() ?: subject.creditHours
                viewModel.updateSubject(subject.subjectId, courseCode, subjectName, creditHours)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------------------- DELETE ----------------------------

    private fun showDeleteConfirmation(subject: Subject) {
        AlertDialog.Builder(this)
            .setTitle("Delete Subject")
            .setMessage("Are you sure you want to delete \"${subject.subjectName}\" (${subject.courseCode})?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubject(subject.subjectId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}