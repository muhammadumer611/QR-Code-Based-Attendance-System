package com.university.attendance

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityScheduleBinding
import com.university.attendance.databinding.DialogScheduleUploadBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ActivitySchedule : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var viewModel: ScheduleViewModel

    private val displayFormat =
        SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.US
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityScheduleBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel =
            ViewModelProvider(this)[ScheduleViewModel::class.java]

        binding.btnBackHeader.setOnClickListener {
            finish()
        }

        binding.btnUpload.setOnClickListener {
            showUploadDialog()
        }

        binding.btnView.setOnClickListener {
            handleView()
        }

        observeViewModel()

        // Admin ke saare teachers load karo
        viewModel.loadTeachers()
    }

    // ============================================================
    // TEACHER SELECTION + UPLOAD DIALOG
    // ============================================================

    private fun showUploadDialog() {

        val teachers =
            viewModel.teachers.value ?: emptyList()

        if (teachers.isEmpty()) {

            Toast.makeText(
                this,
                "No teachers found. Please add a teacher first.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val dialogBinding =
            DialogScheduleUploadBinding.inflate(
                LayoutInflater.from(this)
            )

        // --------------------------------------------------------
        // Teacher names
        // --------------------------------------------------------

        val teacherNames =
            teachers.map { teacher ->

                if (teacher.fullName.isNotBlank()) {
                    teacher.fullName
                } else {
                    teacher.email
                }
            }

        val teacherAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                teacherNames
            )

        dialogBinding.etTeacher.setAdapter(
            teacherAdapter
        )

        dialogBinding.etTeacher.setOnClickListener {
            dialogBinding.etTeacher.showDropDown()
        }

        // --------------------------------------------------------
        // Teacher select hone par uska existing schedule load
        // --------------------------------------------------------

        dialogBinding.etTeacher.setOnItemClickListener {
                _, _, position, _ ->

            val selectedTeacher =
                teachers[position]

            viewModel.loadScheduleForTeacher(
                selectedTeacher.authUid
            )
        }

        // --------------------------------------------------------
        // Save button
        // --------------------------------------------------------

        AlertDialog.Builder(this)
            .setTitle("Upload Schedule")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->

                val selectedPosition =
                    teacherNames.indexOf(
                        dialogBinding.etTeacher.text.toString()
                    )

                if (selectedPosition == -1) {

                    Toast.makeText(
                        this,
                        "Please select a teacher.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                val selectedTeacher =
                    teachers[selectedPosition]

                val fileName =
                    dialogBinding.etFileName
                        .text
                        .toString()

                val note =
                    dialogBinding.etNote
                        .text
                        .toString()

                viewModel.saveSchedule(
                    teacher = selectedTeacher,
                    fileName = fileName,
                    note = note
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // ============================================================
    // VIEW
    // ============================================================

    private fun handleView() {

        val schedule =
            viewModel.schedule.value

        if (
            schedule == null ||
            schedule.fileName.isBlank()
        ) {

            Toast.makeText(
                this,
                "No schedule file uploaded yet.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            this,
            "Teacher: ${schedule.teacherName}\n" +
                    "File: ${schedule.fileName}\n" +
                    "Note: ${schedule.note}",
            Toast.LENGTH_LONG
        ).show()
    }

    // ============================================================
    // OBSERVERS
    // ============================================================

    private fun observeViewModel() {

        viewModel.uiState.observe(this) { state ->

            binding.progressBar.visibility =
                if (
                    state is ScheduleViewModel.UiState.Loading
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            when (state) {

                is ScheduleViewModel.UiState.Error -> {

                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is ScheduleViewModel.UiState.SaveSuccess -> {

                    Toast.makeText(
                        this,
                        "Schedule assigned to teacher successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }

        viewModel.schedule.observe(this) { schedule ->

            if (
                schedule == null ||
                schedule.fileName.isBlank()
            ) {

                binding.tvFileName.text =
                    "No schedule assigned"

                binding.tvUploadedInfo.text = ""

            } else {

                binding.tvFileName.text =
                    schedule.fileName

                binding.tvUploadedInfo.text =
                    "Teacher: ${schedule.teacherName}"
            }
        }
    }
}