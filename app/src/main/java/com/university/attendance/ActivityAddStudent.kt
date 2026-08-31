package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityAddStudentBinding

class ActivityAddStudent : AppCompatActivity() {

    private lateinit var binding:
            ActivityAddStudentBinding

    private lateinit var viewModel:
            AddStudentViewModel

    private val programs =
        listOf(
            "BSSE",
            "BSCS",
            "BSIT",
            "BSAI",
            "BBA",
            "BSEE"
        )

    private val sections =
        listOf(
            "A",
            "B",
            "C",
            "D"
        )

    private val semesters =
        (1..8).map {
            "Semester $it"
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityAddStudentBinding.inflate(
                layoutInflater
            )

        setContentView(
            binding.root
        )

        viewModel =
            ViewModelProvider(this)[
                AddStudentViewModel::class.java
            ]

        setupDropdowns()
        restoreFields()
        setupListeners()
        observeState()
    }

    // ============================================================
    // DROPDOWNS
    // ============================================================

    private fun setupDropdowns() {

        binding.etProgramName.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                programs
            )
        )

        binding.etSection.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                sections
            )
        )

        binding.etSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                semesters
            )
        )
    }

    // ============================================================
    // RESTORE
    // ============================================================

    private fun restoreFields() {

        binding.etUniversityName.setText(
            viewModel.universityName
        )

        binding.etDepartmentName.setText(
            viewModel.departmentName
        )

        binding.etProgramName.setText(
            viewModel.programName
        )

        binding.etSemester.setText(
            "Semester ${viewModel.semester}"
        )

        binding.etSession.setText(
            viewModel.session
        )

        binding.etSection.setText(
            viewModel.section
        )

        binding.etFullName.setText(
            viewModel.fullName
        )

        binding.etFatherName.setText(
            viewModel.fatherName
        )

        binding.etPersonalEmail.setText(
            viewModel.personalEmail
        )

        binding.etRegNo.setText(
            viewModel.regNo
        )

        binding.etContactNumber.setText(
            viewModel.contactNumber
        )

        binding.etCnicNumber.setText(
            viewModel.cnicNumber
        )

        binding.etFatherCnicNumber.setText(
            viewModel.fatherCnicNumber
        )

        binding.etGuardianNumber.setText(
            viewModel.guardianNumber
        )
    }

    // ============================================================
    // LISTENERS
    // ============================================================

    private fun setupListeners() {

        binding.btnNextStep.setOnClickListener {

            saveStep1()

            val error =
                viewModel.validateStep1()

            if (error != null) {

                Toast.makeText(
                    this,
                    error,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            goToStep(
                2
            )
        }

        binding.btnBackStep.setOnClickListener {

            saveStep2()

            goToStep(
                1
            )
        }

        binding.btnSaveStudent.setOnClickListener {

            saveStep2()

            val error =
                viewModel.validateStep2()

            if (error != null) {

                Toast.makeText(
                    this,
                    error,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.saveStudent()
        }
    }

    // ============================================================
    // STEP 1
    // ============================================================

    private fun saveStep1() {

        viewModel.universityName =
            binding.etUniversityName
                .text
                .toString()

        viewModel.departmentName =
            binding.etDepartmentName
                .text
                .toString()

        viewModel.programName =
            binding.etProgramName
                .text
                .toString()

        val semesterText =
            binding.etSemester
                .text
                .toString()

        viewModel.semester =
            Regex("\\d+")
                .find(semesterText)
                ?.value
                ?.toIntOrNull()
                ?: 1

        viewModel.session =
            binding.etSession
                .text
                .toString()

        viewModel.section =
            binding.etSection
                .text
                .toString()
    }

    // ============================================================
    // STEP 2
    // ============================================================

    private fun saveStep2() {

        viewModel.fullName =
            binding.etFullName
                .text
                .toString()

        viewModel.fatherName =
            binding.etFatherName
                .text
                .toString()

        viewModel.personalEmail =
            binding.etPersonalEmail
                .text
                .toString()

        viewModel.regNo =
            binding.etRegNo
                .text
                .toString()

        viewModel.contactNumber =
            binding.etContactNumber
                .text
                .toString()

        viewModel.cnicNumber =
            binding.etCnicNumber
                .text
                .toString()

        viewModel.fatherCnicNumber =
            binding.etFatherCnicNumber
                .text
                .toString()

        viewModel.guardianNumber =
            binding.etGuardianNumber
                .text
                .toString()
    }

    // ============================================================
    // STEPS
    // ============================================================

    private fun goToStep(
        step: Int
    ) {

        if (step == 1) {

            binding.stepOneContainer.visibility =
                View.VISIBLE

            binding.stepTwoContainer.visibility =
                View.GONE

            binding.tvStepIndicator.text =
                "Step 1 of 2 — Class Information"

        } else {

            binding.stepOneContainer.visibility =
                View.GONE

            binding.stepTwoContainer.visibility =
                View.VISIBLE

            binding.tvStepIndicator.text =
                "Step 2 of 2 — Personal Information"
        }
    }

    // ============================================================
    // OBSERVE
    // ============================================================

    private fun observeState() {

        viewModel.saveState.observe(
            this
        ) { state ->

            when (state) {

                is AddStudentViewModel.SaveUiState.Loading -> {

                    binding.progressBar.visibility =
                        View.VISIBLE

                    binding.btnSaveStudent.isEnabled =
                        false
                }

                is AddStudentViewModel.SaveUiState.Success -> {

                    binding.progressBar.visibility =
                        View.GONE

                    binding.btnSaveStudent.isEnabled =
                        true

                    binding.tvGeneratedStudentId.text =
                        "Student ID: ${state.generatedStudentId}"

                    binding.tvGeneratedStudentId.visibility =
                        View.VISIBLE

                    Toast.makeText(
                        this,
                        "Student saved successfully.\nID: ${state.generatedStudentId}",
                        Toast.LENGTH_LONG
                    ).show()

                    viewModel.resetPersonalInfoOnly()

                    clearStep2()

                    goToStep(
                        1
                    )
                }

                is AddStudentViewModel.SaveUiState.Error -> {

                    binding.progressBar.visibility =
                        View.GONE

                    binding.btnSaveStudent.isEnabled =
                        true

                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                AddStudentViewModel.SaveUiState.Idle -> {

                    binding.progressBar.visibility =
                        View.GONE
                }
            }
        }
    }

    // ============================================================
    // CLEAR
    // ============================================================

    private fun clearStep2() {

        binding.etFullName.text?.clear()
        binding.etFatherName.text?.clear()
        binding.etPersonalEmail.text?.clear()
        binding.etRegNo.text?.clear()
        binding.etContactNumber.text?.clear()
        binding.etCnicNumber.text?.clear()
        binding.etFatherCnicNumber.text?.clear()
        binding.etGuardianNumber.text?.clear()
    }
}