package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemsubjectBinding

/**
 * Adapter for the subject list (RecyclerView), scoped to whatever
 * Department/Program/Semester is currently selected in the filter bar.
 *
 * Each row now shows the FULL record (Department, Program, Semester,
 * Subject Name, Course Code, Credit Hours) so the data displayed here
 * matches exactly what would later be fetched on the Student side.
 */
class SubjectAdapter(
    private val onEditClick: (Subject) -> Unit,
    private val onDeleteClick: (Subject) -> Unit
) : ListAdapter<Subject, SubjectAdapter.SubjectViewHolder>(DIFF_CALLBACK) {

    inner class SubjectViewHolder(private val binding: ItemsubjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            binding.tvDeptProgramSemester.text =
                "${subject.departmentName} • ${subject.programName} • Semester ${subject.semester}"

            binding.tvSubjectName.text = subject.subjectName
            binding.tvCourseCode.text = "Course Code: ${subject.courseCode}"
            binding.tvCreditHours.text = "${subject.creditHours} Credit Hours"

            binding.btnEdit.setOnClickListener { onEditClick(subject) }
            binding.btnDelete.setOnClickListener { onDeleteClick(subject) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemsubjectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SubjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Subject>() {
            override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean =
                oldItem.subjectId == newItem.subjectId

            override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean =
                oldItem == newItem
        }
    }
}