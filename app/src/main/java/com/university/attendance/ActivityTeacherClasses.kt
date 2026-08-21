package com.university.attendance

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityTeacherClassesBinding

class ActivityTeacherClasses : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherClassesBinding
    private lateinit var viewModel: TeacherClassesViewModel
    private lateinit var adapter: ClassListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeacherClassesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[TeacherClassesViewModel::class.java]

        setupUi()
        setupList()
        observeViewModel()

        viewModel.loadClasses()
    }

    private fun setupUi() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRetry.setOnClickListener { viewModel.loadClasses() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupList() {
        adapter = ClassListAdapter { subject ->
            // Class Detail screen isn't built yet -- it needs Subject.kt's
            // full field set, Student.kt, and the attendance_records shape
            // confirmed first, so we don't guess at those here (Step 2).
            Toast.makeText(
                this,
                "Class Detail for ${subject.courseCode} — coming in Step 2.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerClasses.apply {
            layoutManager = LinearLayoutManager(this@ActivityTeacherClasses)
            adapter = this@ActivityTeacherClasses.adapter
        }
    }

    private fun observeViewModel() {

        viewModel.uiState.observe(this) { state ->
            when (state) {

                is TeacherClassesViewModel.UiState.Loading -> {
                    binding.progressLoading.visibility = View.VISIBLE
                    binding.errorState.visibility = View.GONE
                    binding.recyclerClasses.visibility = View.GONE
                    binding.txtEmpty.visibility = View.GONE
                }

                is TeacherClassesViewModel.UiState.Success -> {
                    binding.progressLoading.visibility = View.GONE
                    binding.errorState.visibility = View.GONE
                }

                is TeacherClassesViewModel.UiState.Error -> {
                    binding.progressLoading.visibility = View.GONE
                    binding.recyclerClasses.visibility = View.GONE
                    binding.txtEmpty.visibility = View.GONE
                    binding.errorState.visibility = View.VISIBLE
                    binding.txtErrorMessage.text = state.message
                }
            }
        }

        viewModel.filteredSubjects.observe(this) { subjects ->
            adapter.submitList(subjects)

            if (viewModel.uiState.value is TeacherClassesViewModel.UiState.Success) {
                binding.recyclerClasses.visibility =
                    if (subjects.isEmpty()) View.GONE else View.VISIBLE
                binding.txtEmpty.visibility =
                    if (subjects.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}