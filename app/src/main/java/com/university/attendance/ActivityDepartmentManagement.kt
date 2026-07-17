package com.university.attendance

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityDepartmentManagementBinding
import com.university.attendance.databinding.DialogdepartmentinputBinding

/**
 * Screen: Admin -> Departments -> Department Management
 *
 * Full CRUD:
 *   - Create: FAB opens a dialog to type a new department name
 *   - Read:   RecyclerView shows all departments, updated in real time
 *   - Update: Tapping the Edit icon on a row opens the same dialog, pre-filled
 *   - Delete: Tapping the Delete icon shows a confirmation, then deletes
 *
 * Only the department "name" field is stored, as requested.
 */
class ActivityDepartmentManagement : AppCompatActivity() {

    private lateinit var binding: ActivityDepartmentManagementBinding
    private lateinit var viewModel: DepartmentViewModel
    private lateinit var adapter: DepartmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDepartmentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DepartmentViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeDepartments()
        observeActionState()

        viewModel.startListening()
    }

    private fun setupRecyclerView() {
        adapter = DepartmentAdapter(
            onEditClick = { department -> showEditDialog(department) },
            onDeleteClick = { department -> showDeleteConfirmation(department) }
        )
        binding.recyclerDepartments.layoutManager = LinearLayoutManager(this)
        binding.recyclerDepartments.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBackHeader.setOnClickListener { finish() }
        binding.fabAddDepartment.setOnClickListener { showAddDialog() }
    }

    private fun observeDepartments() {
        viewModel.departments.observe(this) { departments ->
            adapter.submitList(departments)
            binding.tvEmptyState.visibility = if (departments.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun observeActionState() {
        viewModel.actionState.observe(this) { state ->
            when (state) {
                is DepartmentViewModel.ActionState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is DepartmentViewModel.ActionState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.consumeActionState()
                }
                is DepartmentViewModel.ActionState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.consumeActionState()
                }
                is DepartmentViewModel.ActionState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // ---------------------------- CREATE ----------------------------

    private fun showAddDialog() {
        val dialogBinding = DialogdepartmentinputBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Add Department")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogBinding.etDepartmentNameInput.text.toString()
                viewModel.addDepartment(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------------------- UPDATE ----------------------------

    private fun showEditDialog(department: Department) {
        val dialogBinding = DialogdepartmentinputBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etDepartmentNameInput.setText(department.name)
        dialogBinding.etDepartmentNameInput.setSelection(department.name.length) // cursor at end

        AlertDialog.Builder(this)
            .setTitle("Edit Department")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val newName = dialogBinding.etDepartmentNameInput.text.toString()
                viewModel.updateDepartment(department.departmentId, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------------------- DELETE ----------------------------

    private fun showDeleteConfirmation(department: Department) {
        AlertDialog.Builder(this)
            .setTitle("Delete Department")
            .setMessage("Are you sure you want to delete \"${department.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDepartment(department.departmentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}