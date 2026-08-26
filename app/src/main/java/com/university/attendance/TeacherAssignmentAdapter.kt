package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemTeacherForAssignmentBinding

/**
 * Screen 1 adapter: list of teachers. Tapping one opens their subject
 * checklist (screen 2) for assignment.
 */
class TeacherAssignmentAdapter(
    private val teachers: List<Teacher>,
    private val onClick: (Teacher) -> Unit
) : RecyclerView.Adapter<TeacherAssignmentAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemTeacherForAssignmentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(teacher: Teacher) {
            binding.tvTeacherName.text = teacher.fullName
            binding.tvTeacherInfo.text = "${teacher.designation} • ${teacher.departmentName}"
            binding.root.setOnClickListener { onClick(teacher) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTeacherForAssignmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(teachers[position])
    }

    override fun getItemCount(): Int = teachers.size
}