package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityTeacherClassesBinding
import kotlinx.coroutines.launch

class ActivityTeacherClasses : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherClassesBinding
    private lateinit var viewModel: TeacherClassesViewModel
    private lateinit var adapter: TeacherClassesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityTeacherClassesBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel =
            ViewModelProvider(this)[TeacherClassesViewModel::class.java]

        setupRecyclerView()
        setupClicks()
        observeViewModel()

        loadClasses()
    }

    // ============================================================
    // LOAD CLASSES
    // ============================================================

    private fun loadClasses() {

        if (FirebaseAuth.getInstance().currentUser == null) {

            Toast.makeText(
                this,
                "Teacher session not found.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        lifecycleScope.launch {

            try {

                val teacherId =
                    TeacherSession.getTeacherId(this@ActivityTeacherClasses)

                if (teacherId.isNullOrBlank()) {

                    Toast.makeText(
                        this@ActivityTeacherClasses,
                        "Teacher session not found.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                viewModel.loadClasses(teacherId)

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityTeacherClasses,
                    e.message
                        ?: "Unable to load teacher profile.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // RECYCLER VIEW
    // ============================================================

    private fun setupRecyclerView() {

        adapter =
            TeacherClassesAdapter { subject ->

                openClassDetail(subject)
            }

        binding.recyclerClasses.apply {

            layoutManager =
                LinearLayoutManager(
                    this@ActivityTeacherClasses
                )

            adapter =
                this@ActivityTeacherClasses.adapter

            setHasFixedSize(false)
        }
    }

    // ============================================================
    // CLICKS
    // ============================================================

    private fun setupClicks() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRetry.setOnClickListener {
            loadClasses()
        }
    }

    // ============================================================
    // OBSERVERS
    // ============================================================

    private fun observeViewModel() {

        viewModel.uiState.observe(this) { state ->

            when (state) {

                is TeacherClassesViewModel.UiState.Idle -> {

                    binding.loadingOverlay.visibility =
                        View.GONE

                    binding.errorState.visibility =
                        View.GONE
                }

                is TeacherClassesViewModel.UiState.Loading -> {

                    binding.loadingOverlay.visibility =
                        View.VISIBLE

                    binding.errorState.visibility =
                        View.GONE
                }

                is TeacherClassesViewModel.UiState.Success -> {

                    binding.loadingOverlay.visibility =
                        View.GONE

                    binding.errorState.visibility =
                        View.GONE
                }

                is TeacherClassesViewModel.UiState.Error -> {

                    binding.loadingOverlay.visibility =
                        View.GONE

                    binding.errorState.visibility =
                        View.VISIBLE

                    binding.txtErrorMessage.text =
                        state.message
                }
            }
        }

        viewModel.classes.observe(this) { classes ->

            binding.txtEmpty.visibility =
                if (classes.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.recyclerClasses.visibility =
                if (classes.isEmpty()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            adapter.submitList(classes)
        }
    }

    // ============================================================
    // CLASS DETAIL
    // ============================================================

    private fun openClassDetail(
        subject: Subject
    ) {

        val intent =
            Intent(
                this,
                ActivityTeacherClassDetail::class.java
            ).apply {

                putExtra(
                    "subjectId",
                    subject.subjectId
                )

                putExtra(
                    "courseCode",
                    subject.courseCode
                )

                putExtra(
                    "subjectName",
                    subject.subjectName
                )

                putExtra(
                    "programName",
                    subject.programName
                )

                putExtra(
                    "semester",
                    subject.semester
                )

                putExtra(
                    "departmentName",
                    subject.departmentName
                )

                putExtra(
                    "teacherId",
                    subject.teacherId
                )
            }

        startActivity(intent)
    }
}

