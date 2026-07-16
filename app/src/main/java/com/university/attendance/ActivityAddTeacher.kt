package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityAddTeacherBinding

/**
 * Screen: Admin -> Teacher Management -> Add Teacher
 *
 * Two-step form:
 *   Step 1: Department (from existing classes), Designation, Main Subject
 *   Step 2: Full Name, Father Name, CNIC, Father CNIC, Contact Number
 *
 * On save, TeacherRepository (via AddTeacherViewModel) saves the teacher
 * under "teachers" in Firestore, after checking for duplicate CNIC.
 */
class ActivityAddTeacher : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private lateinit var viewModel: AddTeacherViewModel

    // Standard designation list as requested.
    private val designationOptions = listOf(
        "Professor",
        "Assistant Professor",
        "Lecturer",
        "Visiting Faculty"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AddTeacherViewModel::class.java]

        setupDesignationDropdown()
        observeDepartments()
        restoreFieldsFromViewModel()
        setupClickListeners()
        observeSaveState()

        viewModel.loadDepartments()
    }

    private fun setupDesignationDropdown() {
        binding.etDesignation.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, designationOptions)
        )
        // Force it to behave like a select-only dropdown (tap to open list,
        // no free typing), since designation must be one of the 4 fixed values.
        binding.etDesignation.setOnClickListener { binding.etDesignation.showDropDown() }
    }

    /**
     * Departments come from existing classes (same source Student Management
     * writes to), so this list always matches real departments in use.
     */
    private fun observeDepartments() {
        viewModel.departments.observe(this) { departments ->
            binding.etDepartment.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departments)
            )
            binding.etDepartment.setOnClickListener { binding.etDepartment.showDropDown() }

            if (departments.isEmpty()) {
                Toast.makeText(
                    this,
                    "No departments found yet. Add a student first, or type the department manually.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Re-fills fields from ViewModel in case Activity was recreated (e.g. rotation). */
    private fun restoreFieldsFromViewModel() {
        binding.etDepartment.setText(viewModel.departmentName)
        binding.etDesignation.setText(viewModel.designation)
        binding.etMainSubject.setText(viewModel.mainSubject)

        binding.etFullName.setText(viewModel.fullName)
        binding.etFatherName.setText(viewModel.fatherName)
        binding.etCnicNumber.setText(viewModel.cnicNumber)
        binding.etFatherCnicNumber.setText(viewModel.fatherCnicNumber)
        binding.etContactNumber.setText(viewModel.contactNumber)
    }

    private fun setupClickListeners() {
        binding.btnBackHeader.setOnClickListener { finish() }

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

        binding.btnSaveTeacher.setOnClickListener {
            saveStep2FieldsToViewModel()

            val error = viewModel.validateStep2()
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveTeacher()
        }
    }

    private fun saveStep1FieldsToViewModel() {
        viewModel.departmentName = binding.etDepartment.text.toString()
        viewModel.designation = binding.etDesignation.text.toString()
        viewModel.mainSubject = binding.etMainSubject.text.toString()
    }

    private fun saveStep2FieldsToViewModel() {
        viewModel.fullName = binding.etFullName.text.toString()
        viewModel.fatherName = binding.etFatherName.text.toString()
        viewModel.cnicNumber = binding.etCnicNumber.text.toString()
        viewModel.fatherCnicNumber = binding.etFatherCnicNumber.text.toString()
        viewModel.contactNumber = binding.etContactNumber.text.toString()
    }

    private fun goToStep(step: Int) {
        if (step == 1) {
            binding.stepOneContainer.visibility = View.VISIBLE
            binding.stepTwoContainer.visibility = View.GONE
            binding.tvStepIndicator.text = "Step 1 of 2 — Department & Role"
        } else {
            binding.stepOneContainer.visibility = View.GONE
            binding.stepTwoContainer.visibility = View.VISIBLE
            binding.tvStepIndicator.text = "Step 2 of 2 — Personal Information"
        }
    }

    private fun observeSaveState() {
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is AddTeacherViewModel.SaveUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSaveTeacher.isEnabled = false
                }
                is AddTeacherViewModel.SaveUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveTeacher.isEnabled = true
                    Toast.makeText(this, "Teacher saved successfully.", Toast.LENGTH_LONG).show()

                    viewModel.resetForm()
                    clearAllInputs()
                    goToStep(1)
                }
                is AddTeacherViewModel.SaveUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveTeacher.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AddTeacherViewModel.SaveUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun clearAllInputs() {
        binding.etDepartment.text?.clear()
        binding.etDesignation.text?.clear()
        binding.etMainSubject.text?.clear()
        binding.etFullName.text?.clear()
        binding.etFatherName.text?.clear()
        binding.etCnicNumber.text?.clear()
        binding.etFatherCnicNumber.text?.clear()
        binding.etContactNumber.text?.clear()
    }
}