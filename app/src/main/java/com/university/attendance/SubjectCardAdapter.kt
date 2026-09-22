package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSubjectCardBinding

/**
 * Shows the subjects assigned to the logged-in teacher.
 *
 * Only fields confirmed to exist on Subject (via TeacherSubjectsRepository's
 * sort/query usage) are displayed: courseCode, programName, semester,
 * departmentName. No schedule/time/room data is shown since Subject does not
 * currently carry that information -- avoid showing fake data.
 */
class SubjectCardAdapter(
    private val onItemClick: ((Subject) -> Unit)? = null
) : ListAdapter<Subject, SubjectCardAdapter.SubjectViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SubjectViewHolder(
        private val binding: ItemSubjectCardBinding,
        private val onItemClick: ((Subject) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            binding.tvCourseCode.text = subject.courseCode
            binding.tvProgramSemester.text =
                "${subject.programName} - Semester ${subject.semester}"
            binding.tvDepartment.text = subject.departmentName

            if (onItemClick != null) {
                binding.root.setOnClickListener { onItemClick(subject) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Subject>() {

        override fun areItemsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem.subjectId == newItem.subjectId

        override fun areContentsTheSame(oldItem: Subject, newItem: Subject): Boolean =
            oldItem == newItem
    }
}