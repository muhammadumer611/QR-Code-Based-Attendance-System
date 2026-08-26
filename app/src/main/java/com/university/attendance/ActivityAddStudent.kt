package com.university.attendance

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityAddStudentBinding

/**
 * Screen: Admin -> Student Management -> Add Student
 *
 * Two-step form:
 *   Step 1: University, Department, Program, Session, Section
 *   Step 2: Full Name, RegNo, Contact, CNIC, Father CNIC, Guardian Number
 *
 * On save, StudentRepository (via AddStudentViewModel) will:
 *   1. Build a classId from Step 1 fields.
 *   2. Create or update the matching "classes" document (auto-grouping).
 *   3. Save the student under "students" with that classId attached.
 */
class ActivityAddStudent : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding
    private lateinit var viewModel: AddStudentViewModel

    // Common program/session/section suggestions shown in the dropdowns.
    // Admin can still type a custom value if it's not in this list.
    private val programSuggestions = listOf("BSSE", "BSCS", "BSIT", "BSAI", "BBA", "BSEE")
    private val sectionSuggestions = listOf("A", "B", "C", "D")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AddStudentViewModel::class.java]

        setupDropdownSuggestions()
        restoreFieldsFromViewModel()
        setupClickListeners()
        observeSaveState()
    }

    private fun setupDropdownSuggestions() {
        binding.etProgramName.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, programSuggestions)
        )
        binding.etSection.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sectionSuggestions)
        )
    }

    /** Re-fills fields from ViewModel in case Activity was recreated (e.g. rotation). */
    private fun restoreFieldsFromViewModel() {
        binding.etUniversityName.setText(viewModel.universityName)
        binding.etDepartmentName.setText(viewModel.departmentName)
        binding.etProgramName.setText(viewModel.programName)
        binding.etSession.setText(viewModel.session)
        binding.etSection.setText(viewModel.section)

        binding.etFullName.setText(viewModel.fullName)
        binding.etRegNo.setText(viewModel.regNo)
        binding.etContactNumber.setText(viewModel.contactNumber)
        binding.etCnicNumber.setText(viewModel.cnicNumber)
        binding.etFatherCnicNumber.setText(viewModel.fatherCnicNumber)
        binding.etGuardianNumber.setText(viewModel.guardianNumber)
    }

    private fun setupClickListeners() {
        binding.btnNextStep.setOnClickListener {
            saveStep1FieldsToViewModel()

            val error = viewModel.validateStep1()
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            goToStep(2)
        }

        binding.btnBackStep.setOnClickListener {
            saveStep2FieldsToViewModel()
            goToStep(1)
        }

        binding.btnSaveStudent.setOnClickListener {
            saveStep2FieldsToViewModel()

            val error = viewModel.validateStep2()
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveStudent()
        }
    }

    private fun saveStep1FieldsToViewModel() {
        viewModel.universityName = binding.etUniversityName.text.toString()
        viewModel.departmentName = binding.etDepartmentName.text.toString()
        viewModel.programName = binding.etProgramName.text.toString()
        viewModel.session = binding.etSession.text.toString()
        viewModel.section = binding.etSection.text.toString()
    }

    private fun saveStep2FieldsToViewModel() {
        viewModel.fullName = binding.etFullName.text.toString()
        viewModel.regNo = binding.etRegNo.text.toString()
        viewModel.contactNumber = binding.etContactNumber.text.toString()
        viewModel.cnicNumber = binding.etCnicNumber.text.toString()
        viewModel.fatherCnicNumber = binding.etFatherCnicNumber.text.toString()
        viewModel.guardianNumber = binding.etGuardianNumber.text.toString()
    }

    private fun goToStep(step: Int) {
        if (step == 1) {
            binding.stepOneContainer.visibility = android.view.View.VISIBLE
            binding.stepTwoContainer.visibility = android.view.View.GONE
            binding.tvStepIndicator.text = "Step 1 of 2 — Class Information"
        } else {
            binding.stepOneContainer.visibility = android.view.View.GONE
            binding.stepTwoContainer.visibility = android.view.View.VISIBLE
            binding.tvStepIndicator.text = "Step 2 of 2 — Personal Information"
        }
    }

    private fun observeSaveState() {
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is AddStudentViewModel.SaveUiState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.btnSaveStudent.isEnabled = false
                }
                is AddStudentViewModel.SaveUiState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSaveStudent.isEnabled = true
                    Toast.makeText(
                        this,
                        "Student saved successfully to class: ${state.classId}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Clear personal info fields so Admin can quickly add
                    // another student to the SAME class without re-entering
                    // University/Department/Program/Session/Section.
                    viewModel.resetPersonalInfoOnly()
                    clearStep2Inputs()
                    goToStep(1)
                }
                is AddStudentViewModel.SaveUiState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnSaveStudent.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AddStudentViewModel.SaveUiState.Idle -> {
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun clearStep2Inputs() {
        binding.etFullName.text?.clear()
        binding.etRegNo.text?.clear()
        binding.etContactNumber.text?.clear()
        binding.etCnicNumber.text?.clear()
        binding.etFatherCnicNumber.text?.clear()
        binding.etGuardianNumber.text?.clear()
    }
}