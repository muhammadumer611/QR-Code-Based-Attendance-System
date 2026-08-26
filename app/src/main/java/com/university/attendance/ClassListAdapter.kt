package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemClassListRowBinding

class ClassListAdapter(
    private val onItemClick: (Subject) -> Unit
) : ListAdapter<Subject, ClassListAdapter.ClassViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val binding = ItemClassListRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClassViewHolder(
        private val binding: ItemClassListRowBinding,
        private val onItemClick: (Subject) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            binding.tvCourseCode.text = subject.courseCode
            binding.tvProgramSemester.text =
                "${subject.programName} - Semester ${subject.semester}"
            binding.tvDepartment.text = subject.departmentName

            binding.root.setOnClickListener { onItemClick(subject) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Subject>() {

        override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem.subjectId == newItem.subjectId

        override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem == newItem
    }
}