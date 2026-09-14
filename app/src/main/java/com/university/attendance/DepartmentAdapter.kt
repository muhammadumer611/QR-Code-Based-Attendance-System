package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemdepartmentBinding

/**
 * Adapter for the department list (RecyclerView).
 * Each row shows the department name plus Edit and Delete icon buttons.
 *
 * Uses ListAdapter + DiffUtil so the real-time Firestore listener can push
 * new lists in and only the changed rows will animate/update -- efficient
 * even with frequent snapshot updates.
 */
class DepartmentAdapter(
    private val onEditClick: (Department) -> Unit,
    private val onDeleteClick: (Department) -> Unit
) : ListAdapter<Department, DepartmentAdapter.DepartmentViewHolder>(DIFF_CALLBACK) {

    inner class DepartmentViewHolder(private val binding: ItemdepartmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(department: Department) {
            binding.tvDepartmentName.text = department.name

            binding.btnEdit.setOnClickListener { onEditClick(department) }
            binding.btnDelete.setOnClickListener { onDeleteClick(department) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val binding = ItemdepartmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DepartmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Department>() {
            override fun areItemsTheSame(oldItem: Department, newItem: Department): Boolean =
                oldItem.departmentId == newItem.departmentId

            override fun areContentsTheSame(oldItem: Department, newItem: Department): Boolean =
                oldItem == newItem
        }
    }
}