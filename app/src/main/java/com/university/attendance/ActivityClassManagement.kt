package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.university.attendance.databinding.ActivityClassManagementBinding
import kotlinx.coroutines.launch

class ActivityClassManagement : AppCompatActivity() {

    private lateinit var binding: ActivityClassManagementBinding
    private val repo = ClassManagementRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClassManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Semester dropdown
        val semesters = (1..8).map { "Semester $it" }

        binding.etSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                semesters
            )
        )

        binding.etSemester.setOnClickListener {
            binding.etSemester.showDropDown()
        }

        binding.etSemester.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etSemester.showDropDown()
            }
        }

        // Save class
        binding.btnSave.setOnClickListener {
            saveClass()
        }

        // Load existing classes
        loadClasses()
    }

    private fun saveClass() {

        val university = binding.etUniversity.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        val program = binding.etProgram.text.toString().trim()
        val session = binding.etSession.text.toString().trim()
        val section = binding.etSection.text.toString().trim()

        // Basic validation
        if (university.isEmpty()) {
            toast("Enter university name.")
            binding.etUniversity.requestFocus()
            return
        }

        if (department.isEmpty()) {
            toast("Enter department name.")
            binding.etDepartment.requestFocus()
            return
        }

        if (program.isEmpty()) {
            toast("Enter program name.")
            binding.etProgram.requestFocus()
            return
        }

        if (session.isEmpty()) {
            toast("Enter session.")
            binding.etSession.requestFocus()
            return
        }

        if (section.isEmpty()) {
            toast("Enter section.")
            binding.etSection.requestFocus()
            return
        }

        /*
         * IMPORTANT:
         * Kotlin normal strings require \\d instead of \d.
         *
         * Example:
         * Regex("\\d+")
         */
        val semester = Regex("\\d+")
            .find(binding.etSemester.text.toString())
            ?.value
            ?.toIntOrNull()

        if (semester == null) {
            toast("Select a semester.")
            binding.etSemester.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {

            try {

                val result = repo.createOrUpdateClass(
                    university,
                    department,
                    program,
                    semester,
                    session,
                    section
                )

                setLoading(false)

                when (result) {

                    is ClassManagementRepository.Result.Success -> {

                        toast("Class saved successfully.")

                        // Clear fields after successful save
                        binding.etUniversity.setText("")
                        binding.etDepartment.setText("")
                        binding.etProgram.setText("")
                        binding.etSemester.setText("", false)
                        binding.etSession.setText("")
                        binding.etSection.setText("")

                        loadClasses()
                    }

                    is ClassManagementRepository.Result.Error -> {

                        toast(
                            result.message.ifBlank {
                                "Failed to save class."
                            }
                        )
                    }
                }

            } catch (e: Exception) {

                setLoading(false)

                toast(
                    e.message ?: "Failed to save class."
                )
            }
        }
    }

    private fun loadClasses() {

        lifecycleScope.launch {

            try {

                val classes = repo.getClasses()

                binding.classContainer.removeAllViews()

                binding.tvEmpty.visibility =
                    if (classes.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                classes.forEach { c ->

                    val card =
                        MaterialCardView(this@ActivityClassManagement).apply {

                            radius = 20f

                            cardElevation = 0f

                            setCardBackgroundColor(
                                getColor(
                                    com.university.attendance.R.color.glassCard
                                )
                            )

                            val tv =
                                TextView(this@ActivityClassManagement).apply {

                                    setPadding(
                                        18,
                                        18,
                                        18,
                                        18
                                    )

                                    text = buildString {

                                        append(
                                            "${c.departmentName} • ${c.programName}"
                                        )

                                        append("\n")

                                        append(
                                            "Semester ${c.semester} • " +
                                                    "Session ${c.session} • " +
                                                    "Section ${c.section}"
                                        )

                                        append("\n")

                                        append(
                                            "${c.studentCount} students  •  " +
                                                    "ID: ${c.classId}"
                                        )
                                    }

                                    textSize = 13f

                                    setTextColor(
                                        getColor(
                                            com.university.attendance.R.color.textPrimary
                                        )
                                    )
                                }

                            addView(tv)
                        }

                    val layoutParams =
                        android.widget.LinearLayout.LayoutParams(
                            -1,
                            -2
                        )

                    layoutParams.setMargins(
                        0,
                        6,
                        0,
                        6
                    )

                    binding.classContainer.addView(
                        card,
                        layoutParams
                    )
                }

            } catch (e: Exception) {

                toast(
                    e.message ?: "Failed to load classes."
                )
            }
        }
    }

    private fun setLoading(value: Boolean) {

        binding.progress.visibility =
            if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.btnSave.isEnabled = !value
    }

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}

