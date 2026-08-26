package com.university.attendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.university.attendance.databinding.ActivityScheduleBinding
import com.university.attendance.databinding.DialogScheduleUploadBinding

class ActivitySchedule : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var viewModel: ScheduleViewModel

    // Currently selected PDF
    private var selectedPdfUri: Uri? = null

    // ============================================================
    // PDF PICKER
    // ============================================================

    private val pdfPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                selectedPdfUri = uri

                Toast.makeText(
                    this,
                    "PDF selected successfully.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityScheduleBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel =
            ViewModelProvider(this)[ScheduleViewModel::class.java]

        // --------------------------------------------------------
        // Back button
        // --------------------------------------------------------

        binding.btnBackHeader.setOnClickListener {
            finish()
        }

        // --------------------------------------------------------
        // Upload button
        // --------------------------------------------------------

        binding.btnUpload.setOnClickListener {
            showUploadDialog()
        }

        // --------------------------------------------------------
        // View button
        // --------------------------------------------------------

        binding.btnView.setOnClickListener {
            handleView()
        }

        // --------------------------------------------------------
        // Observers
        // --------------------------------------------------------

        observeViewModel()

        // --------------------------------------------------------
        // Load teachers
        // --------------------------------------------------------

        viewModel.loadTeachers()
    }

    // ============================================================
    // TEACHER SELECTION + UPLOAD DIALOG
    // ============================================================

    private fun showUploadDialog() {

        // Reset previous PDF selection
        selectedPdfUri = null

        val teachers =
            viewModel.teachers.value ?: emptyList()

        // --------------------------------------------------------
        // No teachers
        // --------------------------------------------------------

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

        // ========================================================
        // TEACHER NAMES
        // ========================================================

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

        // Show dropdown when clicked
        dialogBinding.etTeacher.setOnClickListener {
            dialogBinding.etTeacher.showDropDown()
        }

        // Also show dropdown when focused
        dialogBinding.etTeacher.setOnFocusChangeListener {
                _, hasFocus ->

            if (hasFocus) {
                dialogBinding.etTeacher.showDropDown()
            }
        }

        // ========================================================
        // TEACHER SELECTED
        // ========================================================

        dialogBinding.etTeacher.setOnItemClickListener {
                _, _, position, _ ->

            val selectedTeacher =
                teachers[position]

            // Load existing schedule
            viewModel.loadScheduleForTeacher(
                selectedTeacher.authUid
            )
        }

        // ========================================================
        // PDF SELECT BUTTON
        // ========================================================

        dialogBinding.btnSelectPdf.setOnClickListener {

            pdfPicker.launch(
                arrayOf("application/pdf")
            )
        }

        // ========================================================
        // ALERT DIALOG
        // ========================================================

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Upload Schedule")
                .setView(dialogBinding.root)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create()

        // ========================================================
        // SAVE BUTTON
        // ========================================================

        dialog.setOnShowListener {

            val saveButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            saveButton.setOnClickListener {

                // ------------------------------------------------
                // Teacher validation
                // ------------------------------------------------

                val selectedPosition =
                    teacherNames.indexOf(
                        dialogBinding
                            .etTeacher
                            .text
                            .toString()
                    )

                if (selectedPosition == -1) {

                    Toast.makeText(
                        this,
                        "Please select a teacher.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // ------------------------------------------------
                // PDF validation
                // ------------------------------------------------

                val pdfUri =
                    selectedPdfUri

                if (pdfUri == null) {

                    Toast.makeText(
                        this,
                        "Please select a PDF file first.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // ------------------------------------------------
                // Selected teacher
                // ------------------------------------------------

                val selectedTeacher =
                    teachers[selectedPosition]

                // ------------------------------------------------
                // File name
                // ------------------------------------------------

                val fileName =
                    dialogBinding
                        .etFileName
                        .text
                        .toString()
                        .trim()

                if (fileName.isBlank()) {

                    Toast.makeText(
                        this,
                        "Please enter a file name.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                // ------------------------------------------------
                // Note
                // ------------------------------------------------

                val note =
                    dialogBinding
                        .etNote
                        .text
                        .toString()
                        .trim()

                // ------------------------------------------------
                // Save
                // ------------------------------------------------

                viewModel.saveSchedule(

                    teacher =
                        selectedTeacher,

                    fileName =
                        fileName,

                    note =
                        note,

                    pdfUri =
                        pdfUri
                )

                // ------------------------------------------------
                // Close dialog
                // ------------------------------------------------

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // ============================================================
    // VIEW PDF
    // ============================================================

    private fun handleView() {

        val schedule =
            viewModel.schedule.value

        // --------------------------------------------------------
        // No schedule
        // --------------------------------------------------------

        if (
            schedule == null ||
            schedule.fileUrl.isBlank()
        ) {

            Toast.makeText(
                this,
                "No schedule PDF uploaded yet.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // --------------------------------------------------------
        // Open PDF
        // --------------------------------------------------------

        try {


            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(schedule.fileUrl)
                )

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No PDF viewer or browser is available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // OBSERVERS
    // ============================================================

    private fun observeViewModel() {

        // ========================================================
        // UI STATE
        // ========================================================

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
                        "Schedule uploaded and assigned to teacher successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }

        // ========================================================
        // SCHEDULE
        // ========================================================

        viewModel.schedule.observe(this) { schedule ->

            if (
                schedule == null ||
                schedule.fileName.isBlank()
            ) {

                binding.tvFileName.text =
                    "No schedule assigned"

                binding.tvUploadedInfo.text =
                    ""

            } else {

                binding.tvFileName.text =
                    schedule.fileName

                binding.tvUploadedInfo.text =
                    "Teacher: ${schedule.teacherName}"
            }
        }
    }
}