package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemAttendanceDepartmentBinding

/**
 * Screen 1 adapter: shows each department as a tappable row.
 * Tapping drills down into that department's classes.
 */
class AttendanceDepartmentAdapter(
    private val departments: List<Department>,
    private val onClick: (Department) -> Unit
) : RecyclerView.Adapter<AttendanceDepartmentAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAttendanceDepartmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(department: Department) {
            binding.tvDepartmentName.text = department.name
            binding.root.setOnClickListener { onClick(department) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceDepartmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(departments[position])
    }

    override fun getItemCount(): Int = departments.size
}